package opensavvy.indolent.primitives

import kotlinx.coroutines.CopyableThrowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

/**
 * Makes an external data source available to the program.
 *
 * Proper implementations of this interface **must be entirely lazy**.
 * All functions should an instance of [Observable] immediately without performing any work.
 * Implementations should uphold the [Observable] contract; in particular, implementations must
 * communicate changes to the underlying data correctly.
 */
@PrimitiveApi
interface Parser {

	/**
	 * Reads the value of a [cursor] of type [Cursor.Type.Scalar].
	 *
	 * If the value cannot be read for some reason, [IncorrectScalarTypeException] is thrown when observing the returned value.
	 */
	fun <Content> readScalar(cursor: Cursor<*, *, Content>): Observable<Content>

	/**
	 * Lists direct children of the passed [cursor], which is a [Cursor.Type.Series].
	 *
	 * If children cannot be read for some reason, [IncorrectScalarTypeException] is thrown when observing the returned value.
	 */
	fun enumerateSeries(cursor: Cursor<*, SeriesIndex, Nothing>): Observable<Flow<Cursor<SeriesIndex, *, *>>>

	/**
	 * Lists direct children of the passed [cursor], which is a [Cursor.Type.Record].
	 *
	 * If children cannot be read for some reason, [IncorrectScalarTypeException] is thrown when observing the returned value.
	 */
	fun enumerateRecord(cursor: Cursor<*, RecordIndex, Nothing>): Observable<Flow<Cursor<RecordIndex, *, *>>>

	@ExperimentalCoroutinesApi
	abstract class ParserException(message: String, cause: Throwable? = null): RuntimeException(message, cause)

	/**
	 * Exception thrown by [readScalar] when the cursor's [Scalar.type][Cursor.Type.Scalar.type] doesn't correspond
	 * to the actual data (the actual data has another type).
	 */
	@ExperimentalCoroutinesApi
	class IncorrectScalarTypeException(message: String, cause: Throwable? = null) : ParserException(message, cause), CopyableThrowable<IncorrectScalarTypeException> {
		override fun createCopy(): IncorrectScalarTypeException =
			IncorrectScalarTypeException(message!!, cause)
	}

	/**
	 * Exception thrown by [Parser] when the requested cursor matches no available data.
	 */
	@ExperimentalCoroutinesApi
	class CursorNotFoundException(message: String, cause: Throwable? = null) : ParserException(message, cause), CopyableThrowable<CursorNotFoundException> {
		override fun createCopy(): CursorNotFoundException =
			CursorNotFoundException(message!!, cause)
	}
}
