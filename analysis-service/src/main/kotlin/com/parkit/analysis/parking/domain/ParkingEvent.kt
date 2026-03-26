package com.parkit.analysis.parking.domain


data class ParkingEvent(
	val time: Double,
	val x: Double,
	val y: Double,
	val z: Double,
	val handleAngle: Double,
	val sensor: SensorData,
){
    data class SensorData(
        val frontDistance: Double,
        val leftDistance: Double,
        val rightDistance: Double,
        val rearDistance: Double,
        val speed: Double,
    )
}
