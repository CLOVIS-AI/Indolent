package opensavvy.indolent.primitives

/**
 * Indolent primitives are used internally by the Indolent library to parse file formats, etc.
 *
 * If you are writing an integration with a new file format, you may safely opt in to this annotation.
 *
 * If you are not writing a parsing integration, and in particular if you are writing an application,
 * we recommend avoiding using these classes directly.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
@RequiresOptIn
@MustBeDocumented
annotation class PrimitiveApi
