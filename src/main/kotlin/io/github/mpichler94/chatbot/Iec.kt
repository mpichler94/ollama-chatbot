package io.github.mpichler94.chatbot

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.reader.ExtractedTextFormatter
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/iec")
class IecController(private val vs: VectorStore, private val chatClientBuilder: ChatClient.Builder) {
    private val logger = LoggerFactory.getLogger(IecController::class.java)
    private val chatClient: ChatClient

    init {
        var system = """
            You are an AI powered system to help developers implement the IEC 62433 norm. Answer the questions of the
            user with your context.
            If you don't find an answer, then return a disappointed response suggesting we don't have any information.
        """.trimIndent()

        chatClient = chatClientBuilder
            .defaultSystem(system)
            .defaultAdvisors(QuestionAnswerAdvisor(vs, SearchRequest.builder().build()))
            .build()
    }

    @GetMapping("/init")
    fun initVectorStore(): String {
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


        val tokenSplitter = TokenTextSplitter()
        val pdfReader1 = ParagraphPdfDocumentReader("file:T:/IEC 62443-4-1 (2018-01).pdf", config)
        vs.add(tokenSplitter.apply(pdfReader1.read()))
        val pdfReader2 = ParagraphPdfDocumentReader("file:T:/IEC 62443-4-2 (2019-02).pdf", config)
        vs.add(tokenSplitter.apply(pdfReader2.read()))
        val pdfReader3 = ParagraphPdfDocumentReader("file:T:/IEC 62443-2-3 (2015-06).pdf", config)
        vs.add(tokenSplitter.apply(pdfReader3.get()))
        val pdfReader4 = ParagraphPdfDocumentReader("file:T:/IEC 62443-3-3 (2013-08).pdf", config)
        vs.add(tokenSplitter.apply(pdfReader4.get()))
        logger.info("All documents added")

        return "Success"
    }

    @GetMapping("/ask")
    fun ask(): String? {
        val template = PromptTemplate("""
            What do I need to know regarding patching?
            
        """.trimIndent())

        println("Q: ${template.render()}")
        println("")

        val content =  chatClient.prompt(template.create())
            .call()
            .content()
        println("A: $content")
        return content
    }
    }