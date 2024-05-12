package opensavvy.indolent.primitives

/**
 * Writes data into an external storage medium.
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

}
