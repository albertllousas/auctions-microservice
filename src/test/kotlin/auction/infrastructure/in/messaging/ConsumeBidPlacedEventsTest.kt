package auction.infrastructure.`in`.messaging

import arrow.core.left
import arrow.core.right
import auction.Kafka
import auction.application.service.DisableAutoBidCommand
import auction.application.service.DisableAutoBidService
import auction.application.service.PlaceAutoBidCommand
import auction.application.service.PlaceAutoBidService
import auction.domain.model.AuctionId
import auction.domain.model.AutoBidLimitReached
import auction.domain.model.FindAutoBidsByAuction
import auction.fixtures.Builders
import auction.fixtures.buildKafkaProducer
import auction.infrastructure.cfg.KafkaConfig
import auction.infrastructure.out.events.AuctionIntegrationEvent
import auction.infrastructure.out.events.defaultObjectMapperForOutbox
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
@ContextConfiguration(classes = [TestConfiguration::class])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [KafkaConfig::class, ConsumerDependencies::class, ConsumeBidPlacedEvents::class]
)
@TestPropertySource(properties = ["spring.flyway.enabled=false"])
@EnableAutoConfiguration(exclude = [MongoAutoConfiguration::class])
class ConsumeBidPlacedEventsTest {
    companion object {
        val kafka = Kafka()
    }

    @Autowired
    private lateinit var findAutoBidsByAuction: FindAutoBidsByAuction

    @Autowired
    private lateinit var placeAutoBid: PlaceAutoBidService

    @Autowired
    private lateinit var disableAutoBid: DisableAutoBidService

    @Autowired
    private lateinit var noop: () -> Unit

    @Test
    fun `should place auto-bids for an auction when an a bid has been placed`() {
        val auctionDto = Builders.buildAuctionIntegrationDto()
        val event = AuctionIntegrationEvent.BidPlacedEvent(
            auction = auctionDto, eventId = UUID.randomUUID(), occurredOn = LocalDateTime.now()
        )
        val fstAutoBid = Builders.buildAutoBid()
        val sndAutoBid = Builders.buildAutoBid()
        every { findAutoBidsByAuction(AuctionId(auctionDto.id)) } returns listOf(fstAutoBid, sndAutoBid)
        every { placeAutoBid(any()) } returns Unit.right()

        buildKafkaProducer(kafka.container.bootstrapServers)
            .send(
                ProducerRecord(
                    "public.auctions",
                    auctionDto.userId.toString(),
                    defaultObjectMapperForOutbox.writeValueAsBytes(event)
                )
            )

        verify(timeout = 3000) {
            placeAutoBid(PlaceAutoBidCommand(fstAutoBid.id.value))
            placeAutoBid(PlaceAutoBidCommand(sndAutoBid.id.value))
        }
    }

    @Test
    fun `should disable an auto-bid for an auction when placing a bid for it fails`() {
        val auctionDto = Builders.buildAuctionIntegrationDto()
        val event = AuctionIntegrationEvent.BidPlacedEvent(
            auction = auctionDto, eventId = UUID.randomUUID(), occurredOn = LocalDateTime.now()
        )
        val fstAutoBid = Builders.buildAutoBid()
        val sndAutoBid = Builders.buildAutoBid()
        every { findAutoBidsByAuction(AuctionId(auctionDto.id)) } returns listOf(fstAutoBid, sndAutoBid)
        every { placeAutoBid(PlaceAutoBidCommand(fstAutoBid.id.value)) } returns AutoBidLimitReached.left()
        every { placeAutoBid(PlaceAutoBidCommand(sndAutoBid.id.value)) } returns Unit.right()
        buildKafkaProducer(kafka.container.bootstrapServers)
            .send(
                ProducerRecord(
                    "public.auctions",
                    auctionDto.userId.toString(),
                    defaultObjectMapperForOutbox.writeValueAsBytes(event)
                )
            )

        verify(timeout = 3000, exactly = 1) {
            disableAutoBid(DisableAutoBidCommand(fstAutoBid.id.value))
        }
    }

    @Test
    fun `should do nothing when an event that is not bid placed is consumed`() {
        val auctionDto = Builders.buildAuctionIntegrationDto()
        val event = AuctionIntegrationEvent.AuctionCreatedEvent(
            auction = auctionDto, eventId = UUID.randomUUID(), occurredOn = LocalDateTime.now()
        )

        buildKafkaProducer(kafka.container.bootstrapServers)
            .send(
                ProducerRecord(
                    "public.auctions",
                    auctionDto.userId.toString(),
                    defaultObjectMapperForOutbox.writeValueAsBytes(event)
                )
            )

        verify(timeout = 3000) { noop() }
    }
}

@ActiveProfiles("test")
@EnableKafka
@TestConfiguration
private class ConsumerDependencies {

    @Bean
    fun findAutoBidsByAuction(): FindAutoBidsByAuction = mockk(relaxed = true)

    @Bean
    fun placeAutoBidService(): PlaceAutoBidService = mockk(relaxed = true)

    @Bean
    fun disableAutoBidService(): DisableAutoBidService = mockk(relaxed = true)

    @Bean
    fun noop(): () -> Unit = mockk(relaxed = true)
}
