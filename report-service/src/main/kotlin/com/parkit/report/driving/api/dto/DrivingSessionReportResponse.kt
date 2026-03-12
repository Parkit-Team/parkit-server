package com.parkit.report.driving.api.dto

import com.parkit.report.driving.persistence.document.DrivingSessionDocument
import com.parkit.report.driving.persistence.document.SensorLogDocument

data class DrivingSessionReportResponse(
	val session: DrivingSessionDocument,
	val sensorLogs: List<SensorLogDocument>,
)
