package io.github.mpichler94.chatbot

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<OllamaChatbotApplication>().with(TestcontainersConfiguration::class).run(*args)
}
