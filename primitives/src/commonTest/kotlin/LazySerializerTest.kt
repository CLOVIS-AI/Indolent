@file:OptIn(PrimitiveApi::class)

package opensavvy.indolent.primitives

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import opensavvy.prepared.runner.kotest.PreparedSpec
import opensavvy.prepared.suite.backgroundScope
import kotlin.reflect.KClass

@OptIn(PrimitiveApi::class)
private fun <T> immutableObservable(value: T) = object : Observable<T> {
	override fun observe(): Flow<T> = flow {
		println("An immutable observable has been asked to compute its value: $value")
		emit(value)
		awaitCancellation()
	}

	override fun toString() = "ImmutableObservable($value)"
}

@OptIn(PrimitiveApi::class)
private inline fun <reified T : Any> Serializer.writeHelper(key: String, value: T, default: Boolean = false) {
	val cursor = Cursor.root(Cursor.Type.Record).child(key, Cursor.Type.Scalar(T::class))
	val obs = immutableObservable(value)

	if (default) {
		writeDefault(cursor, obs)
	} else {
		write(cursor, obs)
	}
}

@OptIn(PrimitiveApi::class)
private class SpyDirectSerializer : DirectSerializer {
	private val lock = Mutex()
	private var writeCounter = 0
	private val data = HashMap<Cursor<*, *, *>, Any?>()

	override suspend fun <T> write(cursor: Cursor<*, *, T>, content: T) {
		println("Writing $content at $cursor")
		lock.withLock("Writing $content at $cursor") {
			writeCounter++
			data[cursor] = content
		}
	}

	override suspend fun <T> writeDefault(cursor: Cursor<*, *, T>, content: T) {
		throw UnsupportedOperationException("writeDefault is not supported")
	}

	override suspend fun flush() {}

	override suspend fun close() {}

	suspend fun countWrites() = lock.withLock { writeCounter }
	suspend fun read(key: String, type: KClass<*>) = lock.withLock {
		data[Cursor.root(Cursor.Type.Record).child(key, Cursor.Type.Scalar(type))]
	}
}

class LazySerializerTest : PreparedSpec({
	suite("Writes are delayed until flushes") {
		test("No writes are executed before 'flush' is called") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			serializer.writeHelper("a", 1)
			serializer.writeHelper("b", 2, default = true)

			delay(1) // Give the machinery a chance to run

			withClue("The number of writes should be 0, because we didn't call 'flush' yet, and the implementation should be lazy") {
				spy.countWrites() shouldBe 0
			}
		}

		test("Writes are executed when 'flush' is called") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			serializer.writeHelper("a", 1)
			serializer.writeHelper("b", 2, default = true)

			delay(1) // Give the machinery a chance to run
			serializer.flush()

			withClue("After flushing the serializer, both writes should have been replicated") {
				spy.countWrites() shouldBe 2
			}
		}

		test("Writes are executed when 'close' is called") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			serializer.writeHelper("a", 1)
			serializer.writeHelper("b", 2, default = true)

			delay(1) // Give the machinery a chance to run
			serializer.close()

			withClue("After closing the serializer, both writes should have been replicated") {
				spy.countWrites() shouldBe 2
			}
		}
	}

	suite("Behavior after a flush or a close") {
		test("After a flush, the serializer still works") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)
			serializer.flush()

			serializer.writeHelper("a", 1)
			delay(1) // Give the machinery a chance to run
			serializer.flush()

			spy.countWrites() shouldBe 1
		}

		test("Values that have been written once are not written again") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			// Write a first value
			serializer.writeHelper("a", 1)
			delay(1) // Give the machinery a chance to run
			serializer.flush()

			// Write a second value
			serializer.writeHelper("b", 1)
			delay(1) // Give the machinery a chance to run
			serializer.flush()

			withClue("We did two writes. If there are 3 writes reported, maybe the implementation has written again the first write, even though it has already been flushed?") {
				spy.countWrites() shouldBe 2
			}
		}

		test("After a close, writes are refused") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)
			serializer.close()

			serializer.writeHelper("a", 1)
			delay(1) // Give the machinery a chance to run
			serializer.flush()

			spy.countWrites() shouldBe 0
		}
	}

	suite("Write priority") {
		test("The latest written value wins") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			serializer.writeHelper("a", 2)
			serializer.writeHelper("a", 3)

			delay(1) // Give the machinery a chance to run
			serializer.flush()

			withClue("The last written value for a key wins") {
				spy.read("a", Int::class) shouldBe 3
			}
		}

		test("The latest written value wins, especially if the previous value was a default") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			serializer.writeHelper("a", 2, default = true)
			serializer.writeHelper("a", 3)

			delay(1) // Give the machinery a chance to run
			serializer.flush()

			withClue("The last written value for a key wins") {
				spy.read("a", Int::class) shouldBe 3
			}
		}

		test("Default values do not override non-default values") {
			val spy = SpyDirectSerializer()
			val serializer = spy.serializeOnFlush(backgroundScope.coroutineContext)

			serializer.writeHelper("a", 2)
			serializer.writeHelper("a", 3, default = true)

			delay(1) // Give the machinery a chance to run
			serializer.flush()

			withClue("The default value should have been ignored, since another value was already written") {
				spy.read("a", Int::class) shouldBe 2
			}
		}
	}
})
