package opensavvy.indolent.primitives

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

@PrimitiveApi
private class SerializeOnFlush(
	private val upstream: DirectSerializer,
	private val parentContext: CoroutineContext,
) : Serializer {

	private val waitUntilWaitListIsAvailable = CoroutineScope(parentContext + CoroutineName("LazySerializer.waitUntilWaitListIsAvailable") + SupervisorJob(parentContext[Job]))

	private val waitListLock = Mutex()
	private var waitList = CoroutineScope(parentContext + CoroutineName("LazySerializer.waitList") + SupervisorJob(parentContext[Job]))

	private val bufferLock = Mutex()
	private val buffer = HashMap<Cursor<*, *, *>, Observable<*>>()

	private inline fun onWaitListAvailable(owner: Any?, crossinline block: suspend () -> Unit) {
		waitUntilWaitListIsAvailable.launch(CoroutineName("LazySerializer.onWaitListAvailable($owner)")) {
			waitListLock.withLock(owner) {
				block()
			}
		}
	}

	private inline fun onBufferAvailable(owner: Any?, crossinline block: suspend () -> Unit) {
		waitList.launch(CoroutineName("LazySerializer.onBufferAvailable($owner)")) {
			bufferLock.withLock(owner) {
				block()
			}
		}
	}

	override fun <T> write(cursor: Cursor<*, *, T>, content: Observable<T>) {
		val owner = Triple("write", cursor, content)
		onWaitListAvailable(owner) {
			onBufferAvailable(owner) {
				buffer[cursor] = content
			}
		}
	}

	override fun <T> writeDefault(cursor: Cursor<*, *, T>, content: Observable<T>) {
		val owner = Triple("default write", cursor, content)
		onWaitListAvailable(owner) {
			onBufferAvailable(owner) {
				if (cursor !in buffer)
					buffer[cursor] = content
			}
		}
	}

	override suspend fun flush() {
		// Stop anyone from getting into the wait list while we are flushing it
		waitListLock.withLock("flush") {
			// Drain the wait list
			waitList.coroutineContext.job.children.forEach { it.join() }
			waitList.coroutineContext.job.cancel("LazySerializer.flush() was called, this job will not be used for any future writes")

			// Write everything in the buffer
			bufferLock.withLock("flush") {
				supervisorScope {
					for ((cursor, content) in buffer) {
						launch {
							upstream.write(cursor, content.observe().first())
						}
					}
				}

				buffer.clear()
			}

			upstream.flush()

			// Re-enable the wait list
			waitList = CoroutineScope(waitList.coroutineContext + SupervisorJob(parentContext[Job]))
		}
	}

	override suspend fun close() {
		flush()
		waitList.coroutineContext.job.cancel("LazySerializer.close() was called")
		waitUntilWaitListIsAvailable.coroutineContext.job.cancel("LazySerializer.close() was called")

		upstream.close()
	}
}

/**
 * Implements [Serializer] by calling [DirectSerializer.write] at the last possible moment: when [DirectSerializer.flush] is called.
 */
@PrimitiveApi
fun DirectSerializer.serializeOnFlush(context: CoroutineContext): Serializer =
	SerializeOnFlush(this, context)
