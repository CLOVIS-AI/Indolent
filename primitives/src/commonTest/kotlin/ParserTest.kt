package opensavvy.indolent.primitives

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import opensavvy.prepared.runner.kotest.PreparedSpec

@OptIn(PrimitiveApi::class, ExperimentalCoroutinesApi::class)
class ParserTest : PreparedSpec({

	suite("Exceptions") {
		test("Incorrect scalar type copying") {
			val expectedCause = RuntimeException()

			assertSoftly(Parser.IncorrectScalarTypeException("test", expectedCause).createCopy()) {
				message shouldBe "test"
				cause shouldBe expectedCause
			}
		}

		test("Cursor not found copying") {
			val expectedCause = RuntimeException()

			assertSoftly(Parser.CursorNotFoundException("test", expectedCause).createCopy()) {
				message shouldBe "test"
				cause shouldBe expectedCause
			}
		}
	}

})
