package auction.infrastructure.out.events

import auction.domain.model.AuctionCreated
import auction.fixtures.Builders
import auction.infrastructure.out.db.MessagingSystem.KAFKA
import auction.infrastructure.out.db.OutboxMessage
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.ON_PREVIEW
import auction.infrastructure.out.events.AuctionIntegrationEvent.AuctionCreatedEvent
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime.now
import java.time.ZoneId
import java.util.UUID

class PublishAuctionChangesTest {

    private val outboxMongoRepository = mockk<OutboxMongoRepository>(relaxed = true)

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("UTC"))

    private val uuid = UUID.randomUUID()

    private val mapper = defaultObjectMapperForOutbox

    private val publishExternalIntegrationEvent = PublishAuctionChanges(
        isAnActiveTransaction = { true },
        auctionsStreamName = "auctions",
        outboxMongoRepository = outboxMongoRepository,
        clock = clock,
        generateEventId = { uuid },
        mapper = mapper
    )

    @Test
    fun `should handle an auction created event publishing an external integration event`() {
        val auction = Builders.buildAuction()
        val domainEvent = AuctionCreated(auction)

        publishExternalIntegrationEvent(domainEvent)

        verify {
            outboxMongoRepository.save(
                OutboxMessage(
                    id = uuid,
                    aggregateId = auction.id.value,
                    target = "auctions",
                    messagingSystem = KAFKA,
                    payload = mapper.writeValueAsBytes(
                        AuctionCreatedEvent(
                            auction = AuctionIntegrationDto(
                                id = auction.id.value,
                                userId = auction.userId.value,
                                itemId = auction.itemId.value,
                                openingBid = auction.openingAmount.value,
                                minimalBid = auction.minimalAmount.value,
                                openingAt = auction.openingAt,
                                createdAt = auction.createdAt,
                                currentBid = null,
                                status = ON_PREVIEW,
                            ),
                            occurredOn = now(clock),
                            eventId = uuid,
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `should not publish an external integration event when there is not an active transaction`() {
        val auction = Builders.buildAuction()
        val domainEvent = AuctionCreated(auction)

        assertThrows<NonActiveTransactionException> {
            PublishAuctionChanges(
                isAnActiveTransaction = { false },
                auctionsStreamName = "msgs",
                outboxMongoRepository = outboxMongoRepository,
                clock = clock,
                generateEventId = { uuid },
                mapper = mapper
            )(domainEvent)
        }

        verify { outboxMongoRepository wasNot Called }
    }
}