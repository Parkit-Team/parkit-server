package com.parkit.analysis.kafka.mapper

import com.parkit.analysis.kafka.dto.ParkingSensorDto
import com.parkit.analysis.parking.domain.ParkingEvent

fun ParkingSensorDto.toParkingEvent(): ParkingEvent {
	return ParkingEvent(
		time = time,
		x = x,
		y = y,
		z = z,
		handleAngle = handleAngle,
		sensor = ParkingEvent.SensorData(
            frontDistance = frontDist,
            leftDistance = leftDist,
            rightDistance = rightDist,
            rearDistance = rearDist,
            speed = speed,
        ),
	)
}
