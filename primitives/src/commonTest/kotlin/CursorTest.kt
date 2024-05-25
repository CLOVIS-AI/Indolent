package opensavvy.indolent.primitives

import io.kotest.matchers.shouldBe
import opensavvy.prepared.runner.kotest.PreparedSpec

private infix fun Any.shouldBeString(other: String) {
	toString() shouldBe other
}

@OptIn(PrimitiveApi::class)
class CursorTest : PreparedSpec({

	suite("Root cursors") {
		suite("Scalars") {
			test("Integer") {
				Cursor.root(Cursor.Type.Scalar(Int::class)) shouldBeString "# {Int}"
			}
		}

		test("List") {
			Cursor.root(Cursor.Type.Series) shouldBeString "#"
		}

		test("Record") {
			Cursor.root(Cursor.Type.Record) shouldBeString "#"
		}
	}

	suite("Nested cursors") {
		test("Integer in record") {
			Cursor.root(Cursor.Type.Series)
				.child(2, Cursor.Type.Series)
				.child(3, Cursor.Type.Record)
				.child("foo", Cursor.Type.Scalar(String::class))
				.shouldBeString("#.2.3.foo {String}")
		}

		test("Complex text keys") {
			Cursor.root(Cursor.Type.Record)
				.child("simple", Cursor.Type.Record)
				.child("with a space", Cursor.Type.Record)
				.child("with a \" character", Cursor.Type.Record)
				.child("with a . character", Cursor.Type.Scalar(Int::class))
				.shouldBeString("#.simple.\"with a space\".\"with a \\\" character\".\"with a . character\" {Int}")
		}
	}

	test("Equality") {
		val set = buildSet {
			add(Cursor.root(Cursor.Type.Scalar(Int::class)))
			add(Cursor.root(Cursor.Type.Scalar(Int::class)))
			add(Cursor.root(Cursor.Type.Record).child("foo", Cursor.Type.Scalar(String::class)))
		}

		set shouldBe setOf(
			Cursor.root(Cursor.Type.Scalar(Int::class)),
			Cursor.root(Cursor.Type.Record).child("foo", Cursor.Type.Scalar(String::class))
		)
	}
})
