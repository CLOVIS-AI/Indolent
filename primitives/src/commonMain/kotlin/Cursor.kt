package opensavvy.indolent.primitives

import opensavvy.indolent.primitives.Cursor.Companion.root
import opensavvy.indolent.primitives.Cursor.Type
import kotlin.reflect.KClass

/**
 * An identifier that describes a specific value from a data source.
 *
 * Cursor instances are not typesafe: there is no way to verify that a cursor corresponds to an actual value
 * without asking a parser.
 *
 * ### The cursor tree
 *
 * Cursors form a tree: each document starts with a [root] which posses multiple [children][child].
 *
 * The root is marked `#`, and children are separated by dots:
 * ```
 * #       A reference to the document root
 * #.1     A reference to the document '1' accessible inside the root
 * #.3.2   A reference to the document '2' accessible inside the document '3' accessible inside the root
 * ```
 */
@PrimitiveApi
class Cursor<ParentKey, Key, out Content> private constructor(
	/**
	 * Identifier of the current element as a child of [parent].
	 *
	 * The [root] cursor uses the special key [Unit].
	 */
	val key: ParentKey,

	/**
	 * A cursor knows which [type][Type] of data it expects to read.
	 *
	 * The cursor itself cannot verify whether it actually points to data of the given type.
	 * However, any parser asked to read information about a cursor will assume that the type is respected.
	 * Therefore, the type may have an impact on reading when a value is ambiguous.
	 */
	val type: Type<Key, Content>,

	/**
	 * A reference to this cursor's parent.
	 *
	 * The [root] cursor has no parent (`null`).
	 */
	val parent: Cursor<*, ParentKey, *>?,
) {

	init {
		if (parent == null) {
			require(key == Unit) { "The root cursor must have the key Unit, but found $key" }
		} else {
			require(key is String || key is Long) { "Non-root cursors must have keys of type String or Long, but found $key" }
		}
	}

	/**
	 * Creates a [child cursor][Cursor].
	 *
	 * @param key See [Cursor.key].
	 * @param type See [Cursor.type].
	 */
	@PrimitiveApi
	fun <ChildKey, ChildContent> child(key: Key, type: Type<ChildKey, ChildContent>): Cursor<Key, ChildKey, ChildContent> =
		Cursor(key, type, this)

	/**
	 * The various [Cursor] types.
	 *
	 * To learn more, see each type's respective documentation, and [Cursor.type].
	 */
	@PrimitiveApi
	sealed class Type<Key, out Content> {

		/**
		 * A type that is atomically read by a parser.
		 *
		 * This library considers values of this type is opaque elements.
		 * This type may be used when parsing values of simple value types ([Int], [String]…).
		 *
		 * This type may also be used to parse custom types (custom classes…). Note, however, that the parsing is done
		 * by the parser, which may or may not support the requested type.
		 *
		 * Some types deserve special treatment, see [Series] and [Record].
		 */
		@PrimitiveApi
		data class Scalar<Content : Any>(val type: KClass<Content>) : Type<Any, Content>()

		/**
		 * A series is a cursor that contains an unknown number of elements.
		 *
		 * Series are ordered: elements are always returned in the same order.
		 * If elements are inserted or removed within the series, the order of every other element is unchanged.
		 *
		 * Elements are indexed by a [Long]. When the series changes, elements may change indexes.
		 * Indexes are not guaranteed to be continuous (there may be gaps).
		 * No two elements in a series may have the same index, but an element may use an index that previously referred to another element, if the series was modified.
		 */
		@PrimitiveApi
		data object Series : Type<SeriesIndex, Nothing>()

		/**
		 * A record is a cursor that contains multiple elements that are referred to by a name (represented as a [String]).
		 *
		 * Records have a consistent element as long as they are not modified.
		 * However, the order of elements is not guaranteed to be the same after elements have been inserted or removed.
		 *
		 * The name of an element is considered part of the element, and cannot change.
		 * If a name becomes unavailable, it means the element was deleted.
		 */
		@PrimitiveApi
		data object Record : Type<RecordIndex, Nothing>()
	}

	private fun StringBuilder.generateString() {
		if (parent == null) {
			append("#")
		} else {
			with(parent) { generateString() }
			append(".")

			if (key is String && (' ' in key || '.' in key)) {
				append('"')
				append(key.replace("\"", "\\\""))
				append('"')
			} else {
				append(key)
			}
		}

		if (type is Type.Scalar<*>) {
			append(" {")
			append(type.type.simpleName ?: type.type.toString())
			append('}')
		}
	}

	override fun toString() = buildString {
		generateString()
	}

	// region Equals & hashCode

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Cursor<*, *, *>) return false

		if (key != other.key) return false
		if (type != other.type) return false
		if (parent != other.parent) return false

		return true
	}

	override fun hashCode(): Int {
		var result = key?.hashCode() ?: 0
		result = 31 * result + type.hashCode()
		result = 31 * result + (parent?.hashCode() ?: 0)
		return result
	}

	// endregion

	companion object {

		/**
		 * Instantiates a root [Cursor].
		 *
		 * The root cursor has the key [Unit] and has no parent.
		 * Further cursors may be created with [child].
		 */
		@PrimitiveApi
		fun <Key, Content> root(type: Type<Key, Content>) = Cursor(
			Unit,
			type,
			null,
		)
	}
}

/**
 * [Cursor.Type.Series]'s index type.
 */
@PrimitiveApi
typealias SeriesIndex = Long

/**
 * [Cursor.Type.Record]'s index type.
 */
@PrimitiveApi
typealias RecordIndex = String
