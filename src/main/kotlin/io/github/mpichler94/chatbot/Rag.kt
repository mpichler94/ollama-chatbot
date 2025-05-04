package io.github.mpichler94.chatbot

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.document.Document
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import java.util.UUID

@RestController
@RequestMapping("/stocks")
class StockController(
    private val restTemplate: RestTemplate,
    private val mapper: ObjectMapper,
    private val store: VectorStore,
    chatClientBuilder: ChatClient.Builder,
    @Value("\${STOCK_API_KEY}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(StockController::class.java)

    private val chatClient = chatClientBuilder.defaultAdvisors(SimpleLoggerAdvisor()).build()
    private val rqtBuilder = RewriteQueryTransformer.builder().chatClientBuilder(chatClientBuilder)

    @PostMapping("/load-data")
    fun load() {
        val companies = listOf("AAPL", "MSFT", "GOOG", "AMZN", "META", "NVDA")
        for (company in companies) {
            val data = restTemplate.getForObject<StockData>("https://api.twelvedata.com/time_series?symbol={0}&interval=1day&outputsize=10&apikey={1}", company, apiKey)
            val list = data.values.map { it.close }
            val doc = Document.builder()
                .id(UUID.randomUUID().toString())
                .metadata("company", company)
                .text(mapper.writeValueAsString(Stock(company, list)))
                .build()
            store.add(listOf(doc))
            logger.info("Document added: $company")
        }
    }

    @GetMapping("/docs")
    fun query(): List<Document> {
        val searchRequest = SearchRequest.builder()
            .query("Find the most growth trends")
            .topK(2)
            .build()
        val docs = store.similaritySearch(searchRequest)
        return docs!!
    }

    @GetMapping("/v1/most-growth-trend")
    fun getBestTrend(): String? {
        val template = PromptTemplate("""
            {query}
            Which {target} is the most % growth?
            The 0 element in the prices table is the latest price, while the last element ist the oldest price.
            
        """.trimIndent())

        val p = template.create(mapOf("query" to "Find the most growth trends", "target" to "share"))

        return chatClient.prompt(p)
            .advisors(QuestionAnswerAdvisor(store))
            .call()
            .content()
    }

    @GetMapping("/v2/most-growth-trend")
    fun getBestTrendV2(): String? {
        val template = PromptTemplate("""
            Which share is the most % growth?
            The 0 element in the prices table is the latest price, while the last element ist the oldest price.
            Return a full name of company instead of a market shortcut.
            
        """.trimIndent())

        val searchRequest = SearchRequest.builder()
            .query("""
                FInd the most growth trends.
                The 0 element in the prices table ist the latest price, while the last element ist the oldest price.
            """.trimIndent())
            .topK(3)
            .similarityThreshold(0.5)
            .build()

        return chatClient.prompt(template.create())
            .advisors(QuestionAnswerAdvisor(store, searchRequest))
            .call()
            .content()
    }

    @RequestMapping("/v3/most-growth-trend")
    fun getBestTrendV3(): String? {
        val template = PromptTemplate("""
            {query}
            Which {target} is the most % growth?
            The 0 element in the prices table is the latest price, while the last element ist the oldest price.
        """.trimIndent())

        val p = template.create(mapOf("query" to "Find the most growth trends", "target" to "share"))

        val advisor = RetrievalAugmentationAdvisor.builder()
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                .similarityThreshold(0.5)
                .topK(3)
                .vectorStore(store)
                .build()
            )
            .queryTransformers(rqtBuilder.promptTemplate(template).build())
            .build()

        return chatClient.prompt(p)
            .advisors(advisor)
            .call()
            .content()
    }
}

data class  Stock(
    val name: String,
    val symbol: String,
    val prices: List<String>

) {
    constructor(name: String, prices: List<String>) : this(name, "", prices)
}