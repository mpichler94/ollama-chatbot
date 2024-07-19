package io.github.mpichler94.chatbot

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.reader.ExtractedTextFormatter
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ChatbotConfiguration {

//    @Bean
    fun initVectorStore(vs: VectorStore): ApplicationRunner {
        return ApplicationRunner {

            val config = PdfDocumentReaderConfig.builder()
                .withPageTopMargin(1)
                .withPageExtractedTextFormatter(
                    ExtractedTextFormatter.builder()
                        .withNumberOfTopTextLinesToDelete(1)
                        .withNumberOfBottomTextLinesToDelete(1)
                        .build()
                )
                .withPageBottomMargin(1)
                .withPagesPerDocument(1)
                .build()


            val pdfReader1 = ParagraphPdfDocumentReader("file:T:/IEC 62443-4-1 (2018-01).pdf", config)
            vs.add(pdfReader1.read())
            val pdfReader2 = ParagraphPdfDocumentReader("file:T:/IEC 62443-4-2 (2019-02).pdf", config)
            vs.add(pdfReader2.read())
            val pdfReader3 = ParagraphPdfDocumentReader("file:T:/IEC 62443-2-3 (2015-06).pdf", config)
            vs.add(pdfReader3.get())
            val pdfReader4 = ParagraphPdfDocumentReader("file:T:/IEC 62443-3-3 (2013-08).pdf", config)
            vs.add(pdfReader4.get())
        }
    }

    @Bean
//    @DependsOn("initVectorStore")
    fun userPrompt(chatClient: ChatClient): ApplicationRunner {
        return ApplicationRunner {
            val prompt = """
                    What do I need to keep in mind regarding patching?
                    
					""".trimIndent()
            System.out.println("Q: $prompt")
            System.out.println("")

            val content = chatClient.prompt()
                .user(prompt).call().content()

            System.out.println("A: $content")
        }
    }

    @Bean
    fun chatClient(vs: VectorStore, builder: ChatClient.Builder): ChatClient {
        var system = """
            You are an AI powered system to help developers implement the IEC 62433 norm. Answer the questions of the
            user with your context.
            If you don't find an answer, then return a disappointed response suggesting we don't have any information.
        """.trimIndent()

        return builder.defaultSystem(system).defaultAdvisors(QuestionAnswerAdvisor(vs, SearchRequest.defaults()))
            .build()
    }
}