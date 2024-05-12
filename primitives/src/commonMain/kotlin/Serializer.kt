package opensavvy.indolent.primitives

/**
 * Writes data into an external storage medium.
 *
 * A serializer must be able to serialize [Observable] instances, dealing with their changes over time.
 * For simplification, you may implement [DirectSerializer] instead, which only needs to serialize values once.
 */
@PrimitiveApi
interface Serializer {

	/**
	 * Writes [content] at the position referred to by [cursor].
	 */
	fun <T> write(cursor: Cursor<*, *, T>, content: Observable<T>)

	/**
	 * Writes [content] at the position referred to by [cursor], but only if no value has already been written there.
	 */
	fun <T> writeDefault(cursor: Cursor<*, *, T>, content: Observable<T>)

	/**
	 * Forces the serializer to write all its contents.
	 *
	 * Some serializer implementations may delay writing the value.
	 * Calling this function ensures that any delayed value has been written.
	 *
	 * When this function returns, all [write] and [writeDefault] calls that happened **before** the call to this
	 * function are guaranteed to have been persisted.
	 *
	 * However, calls to [write] and [writeDefault] that happened while this function was running are
	 * not guaranteed to have been written. If they haven't, then they will be written by the next call to [flush].
	 */
	suspend fun flush()

	/**
	 * Stops this serializer from running.
	 */
	suspend fun close()
}

/**
 * Variant of [Serializer] that serializers direct values instead of [Observable] instances.
 *
 * ### Strategies
 *
 * - [serializeOnFlush]: Only write values at the last possible time, when [flush] is called.
 */
@PrimitiveApi
interface DirectSerializer {

	/**
	 * Writes [content] at the position referred to by [cursor].
	 */
	suspend fun <T> write(cursor: Cursor<*, *, T>, content: T)

	/**
	 * Writes [content] at the position referred to by [cursor], but only if no value has already been written there.
	 */
	suspend fun <T> writeDefault(cursor: Cursor<*, *, T>, content: T)

	/**
	 * Forces the serializer to write all its contents.
	 *
	 * Some serializer implementations may delay writing the value.
	 * Calling this function ensures that any delayed value has been written.
	 *
	 * When this function returns, all [write] and [writeDefault] calls that happened **before** the call to this
	 * function are guaranteed to have been persisted.
	 *
	 * However, calls to [write] and [writeDefault] that happened while this function was running are
	 * not guaranteed to have been written. If they haven't, then they will be written by the next call to [flush].
	 */
	suspend fun flush()

	/**
	 * Stops this serializer from running.
	 */
	suspend fun close()
}
