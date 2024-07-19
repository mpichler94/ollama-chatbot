package io.github.mpichler94.chatbot

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun pgvectorContainer(): PostgreSQLContainer<*> {
		return PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16"))
	}

}
