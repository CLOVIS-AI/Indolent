package opensavvy.indolent.primitives

/**
 * Stores data in a tree that can be queried with [cursors][Cursor].
 */
@PrimitiveApi
interface Store : Parser, Serializer
