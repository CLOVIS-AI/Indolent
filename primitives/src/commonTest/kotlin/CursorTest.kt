package opensavvy.indolent.primitives

import opensavvy.prepared.runner.testballoon.preparedSuite

private infix fun Any.shouldBeString(other: String) {
	check(toString() == other)
}

@OptIn(PrimitiveApi::class)
val CursorTest by preparedSuite {

	suite("Root cursors") {
		suite("Scalars") {
			test("Integer") {
				Cursor.root(Cursor.Type.Scalar<Int>()) shouldBeString "# {Int}"
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
				.child("foo", Cursor.Type.Scalar<String>())
				.shouldBeString("#.2.3.foo {String}")
		}

		test("Complex text keys") {
			Cursor.root(Cursor.Type.Record)
				.child("simple", Cursor.Type.Record)
				.child("with a space", Cursor.Type.Record)
				.child("with a \" character", Cursor.Type.Record)
				.child("with a . character", Cursor.Type.Scalar<Int>())
				.shouldBeString("#.simple.\"with a space\".\"with a \\\" character\".\"with a . character\" {Int}")
		}
	}

	test("Equality") {
		val set = buildSet {
			add(Cursor.root(Cursor.Type.Scalar<Int>()))
			add(Cursor.root(Cursor.Type.Scalar<Int>()))
			add(Cursor.root(Cursor.Type.Record).child("foo", Cursor.Type.Scalar<String>()))
		}

		check(set == setOf(
			Cursor.root(Cursor.Type.Scalar<Int>()),
			Cursor.root(Cursor.Type.Record).child("foo", Cursor.Type.Scalar<String>())
		))
	}
}
