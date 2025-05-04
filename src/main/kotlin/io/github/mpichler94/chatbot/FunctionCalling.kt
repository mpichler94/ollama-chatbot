package io.github.mpichler94.chatbot

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.tool.annotation.Tool
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@RestController
@RequestMapping("/wallet")
class WalletController(chatClientBuilder: ChatClient.Builder, private val walletTools: WalletTools) {
    private val chatClient = chatClientBuilder.defaultAdvisors(SimpleLoggerAdvisor()).build()

    @GetMapping
    fun calculateWalletValue(): String? {
        val template = PromptTemplate("""
            What's the current value in dollars of my wallet based on the latest stock prices?
            Return values per each company and a total value as sum of values per each company in JSON format. 
            Use the available tools to get the companies, quantities and latest stock prices.
            
        """.trimIndent())

        return chatClient
            .prompt(template.create())
            .tools(walletTools)
            .call()
            .content()
    }
}

@Component
class WalletTools(private val walletRepository: WalletRepository, private val restTemplate: RestTemplate, @Value("\${STOCK_API_KEY}") private val apiKey: String) {
    private val logger = LoggerFactory.getLogger(WalletTools::class.java)

    @Tool(description = "Companies in my wallet")
    fun companies(): List<String> {
        val companies = walletRepository.findAll().map { it.company }
        logger.info("Tool 'companies' returned $companies")
        return companies
    }

    @Tool(description = "Number of shares for each company in my wallet")
    fun numberOfShares(): List<Share> {
        val shares = walletRepository.findAll()
        logger.info("Tool 'numberOfShares' returned $shares")
        return shares
    }

    @Tool(description = "Latest stock prices for a company")
    fun latestStockPrices(company: String): Float {
        val data = restTemplate.getForObject<StockData>("https://api.twelvedata.com/time_series?symbol={0}&interval=1min&outputsize=1&apikey={1}", company, apiKey)
        val latestData = data.values.first()

        logger.info("Tool 'latestStockPrices'($company) returned ${latestData.close}")
        return latestData.close.toFloat()
    }
}

@Configuration
class FunctionCallingConfiguration {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}

@Entity
class Share(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    val company: String,
    val quantity: Int
)

interface WalletRepository : ListCrudRepository<Share, Long>

data class StockData(val values: List<DailyStockData>)

data class DailyStockData(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
)