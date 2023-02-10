package auction.acceptance

import auction.AppRunner
import auction.Kafka
import auction.Mongo
import auction.acceptance.BaseAcceptanceTest.Initializer
import auction.fixtures.buildKafkaConsumer
import auction.fixtures.buildKafkaProducer
import auction.infrastructure.cfg.GeneralConfig
import com.github.javafaker.Faker
import com.github.tomakehurst.wiremock.WireMockServer
import io.restassured.RestAssured
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [Initializer::class], classes = [AppRunner::class])
abstract class BaseAcceptanceTest {

    @Autowired
    protected lateinit var clock: Clock

    private val faker = Faker()

    init {
        RestAssured.defaultParser = io.restassured.parsing.Parser.JSON

//        wireMockServer.resetAll()
    }

    @LocalServerPort
    protected val servicePort: Int = 0

    protected val mapper = GeneralConfig().defaultObjectMapper()

    protected val kafkaConsumer = buildKafkaConsumer(kafka.container.bootstrapServers)

    protected val kafkaProducer = buildKafkaProducer(kafka.container.bootstrapServers)

    companion object {

        val mongo = Mongo()
        val kafka = Kafka()
        var wireMockServer = WireMockServer().also { it.start() }
    }

    class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "kafka.bootstrap.servers=${kafka.container.bootstrapServers}",
                "http.clients.users.url=${wireMockServer.baseUrl()}",
                "http.clients.items.url=${wireMockServer.baseUrl()}",
                "db.mongo.connection-string=mongodb://${mongo.container.host}:${mongo.container.firstMappedPort}",
                "db.mongo.database=test"
            ).applyTo(configurableApplicationContext.environment)
        }
    }
}
