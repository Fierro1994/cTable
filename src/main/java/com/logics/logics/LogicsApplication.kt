package com.logics.logics

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class LogicsApplication

fun main(args: Array<String>) {
    runApplication<LogicsApplication>(*args)
}