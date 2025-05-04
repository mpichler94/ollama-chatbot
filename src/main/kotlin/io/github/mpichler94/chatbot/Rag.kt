package io.github.mpichler94.chatbot

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
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
    @Value("\${STOCK_API_KEY}") private val apiKey: String
) {
    private val logger = LoggerFactory.getLogger(StockController::class.java)

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
}

data class  Stock(
    val name: String,
    val symbol: String,
    val prices: List<String>

) {
    constructor(name: String, prices: List<String>) : this(name, "", prices)
}