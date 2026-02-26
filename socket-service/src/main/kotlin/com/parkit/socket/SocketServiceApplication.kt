package com.parkit.socket

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SocketServiceApplication

fun main(args: Array<String>) {
	runApplication<SocketServiceApplication>(*args)
}
