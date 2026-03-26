package com.parkit.analysis.util

import com.parkit.analysis.kafka.dto.ParkingSensorDto

object TestCsvUtils {

	fun parseCsvLine(line: String): ParkingSensorDto {
		val tokens = line.split(",").map { it.trim() }
		return ParkingSensorDto(
			time = tokens[0].toDoubleOrNull() ?: 0.0,
			x = tokens[1].toDoubleOrNull() ?: 0.0,
			y = tokens[2].toDoubleOrNull() ?: 0.0,
			z = tokens[3].toDoubleOrNull() ?: 0.0,
			steer = tokens[4].toDoubleOrNull() ?: 0.0,
			wheelDegree = tokens[5].toDoubleOrNull() ?: 0.0,
			handleAngle = tokens[6].toDoubleOrNull() ?: 0.0,
			speed = if (tokens[7] == "nan") 0.0 else tokens[7].toDoubleOrNull() ?: 0.0,
			frontDist = tokens[8].toDoubleOrNull() ?: 0.0,
			leftDist = tokens[9].toDoubleOrNull() ?: 0.0,
			rightDist = tokens[10].toDoubleOrNull() ?: 0.0,
			rearDist = tokens[11].toDoubleOrNull() ?: 0.0
		)
	}
}
