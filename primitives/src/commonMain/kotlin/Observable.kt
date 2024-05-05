package opensavvy.indolent.primitives

import kotlinx.coroutines.flow.Flow

/**
 * Wrapper for a value that changes over time.
 *
 * This interface supports two different workflows, chosen by the implementation:
 * - **Pull-based workflow**: when the caller wants to read a new value, they must [observe] this instance again.
 * - **Push-based workflow**: some implementations are able to detect whether a new value is available autonomously.
 * These implementations are able to send new values to the caller automatically.
 *
 * To read the current value, see [observe].
 */
@PrimitiveApi
interface Observable<out T> {

	/**
	 * Reads the current value behind this [Observable].
	 *
	 * ### Implementation notes
	 *
	 * The returned flow **must** follow these rules:
	 * - If the observable is able to detect by itself that its value has changed,
	 * the flow **must** remain flowing, and **must** emit new values, as long as the consumer doesn't stop collecting.
	 * - If the observable is not able to detect that its value has changed, then
	 * the flow **must** terminate as soon as the value is emitted.
	 *
	 * These constraints ensure that the consumer knows whether it should call this function again (pull-based workflow), or if it can just
	 * wait for the observable to notify of new values (push-based workflow).
	 *
	 * ### The three types of mutation
	 *
	 * Said in another way, we can distinguish the three cases of mutation:
	 * - An immutable value: represented by a [Flow] that emits a single value and never terminates.
	 * - An observable that can detect when its value changes: represented by a [Flow] that emits new values over time.
	 * - An observable that cannot detect when its value changes: represented by a [Flow] that emits a value, then terminates.
	 */
	@PrimitiveApi
	fun observe(): Flow<T>
}
