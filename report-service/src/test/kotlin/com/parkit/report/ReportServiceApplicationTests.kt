package com.parkit.report

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
	properties = [
		"parkit.kafka.enabled=false",
	],
)
class ReportServiceApplicationTests {

	@Test
	fun contextLoads() {
	}

}
