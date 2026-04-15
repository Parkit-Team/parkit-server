import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

// Shared JaCoCo report configuration across independent Gradle projects.

tasks.named("jacocoTestReport", JacocoReport::class.java) {
	reports {
		xml.required.set(true)
		csv.required.set(true)
		html.required.set(true)
	}
}

tasks.withType(Test::class.java).configureEach {
	finalizedBy(tasks.named("jacocoTestReport"))
}
