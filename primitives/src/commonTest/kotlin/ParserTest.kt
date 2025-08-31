package opensavvy.indolent.primitives

import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.prepared.runner.testballoon.preparedSuite

@OptIn(PrimitiveApi::class, ExperimentalCoroutinesApi::class)
val ParserTest by preparedSuite {

	suite("Exceptions") {
		test("Incorrect scalar type copying") {
			val expectedCause = RuntimeException()

			val exception = Parser.IncorrectScalarTypeException("test", expectedCause).createCopy()
			check(exception.message == "test")
			check(exception.cause == expectedCause)
		}

		test("Cursor not found copying") {
			val expectedCause = RuntimeException()

			val exception = Parser.CursorNotFoundException("test", expectedCause).createCopy()
			check(exception.message == "test")
			check(exception.cause == expectedCause)
		}
	}

}
