package com.parkit.analysis.global.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.context.annotation.Configuration

@OpenAPIDefinition(
	info = Info(
		title = "Parkit 주행 리포트 API",
		description = "주행 세션(start/stop)과 센서 로그 저장/조회 API",
		version = "v1",
	),
	servers = [
		Server(url = "/", description = "현재 서버"),
	],
)
@Configuration
class OpenApiConfig
