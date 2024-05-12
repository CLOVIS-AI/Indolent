package opensavvy.indolent.primitives

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * [Serializer] implementation that waits until the last possible time to write its values: when [flush] is called.
 */
@PrimitiveApi
abstract class LazySerializer(
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

	abstract suspend fun <T> write(cursor: Cursor<*, *, T>, content: T)

	final override fun <T> write(cursor: Cursor<*, *, T>, content: Observable<T>) {
		val owner = Triple("write", cursor, content)
		onWaitListAvailable(owner) {
			onBufferAvailable(owner) {
				buffer[cursor] = content
			}
		}
	}

	final override fun <T> writeDefault(cursor: Cursor<*, *, T>, content: Observable<T>) {
		val owner = Triple("default write", cursor, content)
		onWaitListAvailable(owner) {
			onBufferAvailable(owner) {
				if (cursor !in buffer)
					buffer[cursor] = content
			}
		}
	}

	protected open suspend fun onFlush() {}

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
							write(cursor, content.observe().first())
						}
					}
				}

				buffer.clear()
			}

			onFlush()

			// Re-enable the wait list
			waitList = CoroutineScope(waitList.coroutineContext + SupervisorJob(parentContext[Job]))
		}
	}

	suspend fun close() {
		flush()
		waitList.coroutineContext.job.cancel("LazySerializer.close() was called")
		waitUntilWaitListIsAvailable.coroutineContext.job.cancel("LazySerializer.close() was called")
	}
}
