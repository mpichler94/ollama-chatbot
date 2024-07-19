package io.github.mpichler94.chatbot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OllamaChatbotApplication

fun main(args: Array<String>) {
	runApplication<OllamaChatbotApplication>(*args)
}
