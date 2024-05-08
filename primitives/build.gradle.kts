plugins {
	alias(opensavvyConventions.plugins.base)
	alias(opensavvyConventions.plugins.kotlin.library)
}

kotlin {
	jvm()
	js {
		browser()
		nodejs()
	}
	linuxX64()
	iosArm64()
	iosSimulatorArm64()
	iosX64()

	sourceSets.commonMain.dependencies {
		api(libs.kotlinx.coroutines)
	}

	sourceSets.commonTest.dependencies {
		implementation(libs.prepared.kotest)
	}
}

library {
	name.set("Primitives")
	description.set("Low-level constructs for lazy data construction")
	homeUrl.set("https://gitlab.com/opensavvy/groundwork/indolent")

	license.set {
		name.set("Apache 2.0")
		url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
	}

	coverage.set(95)
}
