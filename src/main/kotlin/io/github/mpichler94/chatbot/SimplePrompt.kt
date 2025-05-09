package io.github.mpichler94.chatbot

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.client.entity
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/persons")
class PersonController(chatClientBuilder: ChatClient.Builder, chatMemory: InMemoryChatMemory) {
    val chatClient: ChatClient = chatClientBuilder
        .defaultAdvisors(PromptChatMemoryAdvisor(chatMemory), SimpleLoggerAdvisor())
        .build()

    @GetMapping
    fun findAll(): List<Person> {
        val template = PromptTemplate("""
            Return a current list of 10 persons if exists or generate a new list with random values.
            Each object should contain an auto-incremented id field.
            Do not include any explanations or additional text.
        """.trimIndent())

        return chatClient.prompt(template.create())
            .call()
            .entity<List<Person>>()
    }

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Int): Person {
        val template = PromptTemplate("""
            Find and return the object with id {id} in a current list of persons.
            
        """.trimIndent())

            val p = template.create(mapOf("id" to id))

        return chatClient.prompt(p)
            .call()
            .entity()
    }

}

data class Person(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val gender: Gender,
    val nationality: String,
)

enum class Gender {
    MALE, FEMALE
}

@Configuration
class SimplePromptConfiguration {
    @Bean
    fun chatMemory(): InMemoryChatMemory {
        return InMemoryChatMemory()
    }
}