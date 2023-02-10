package auction.infrastructure.out.events

import auction.domain.model.Amount
import auction.domain.model.AuctionCreated
import auction.domain.model.AuctionEnded
import auction.domain.model.AuctionOpened
import auction.domain.model.AuctionStatus.*
import auction.domain.model.Bid
import auction.domain.model.BidPlaced
import auction.domain.model.UserId
import auction.fixtures.Builders
import auction.infrastructure.out.db.OutboxMessage
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.events.AuctionIntegrationDto.*
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.EXPIRED
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.ON_PREVIEW
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.OPENED
import auction.infrastructure.out.events.AuctionIntegrationEvent.*
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigDecimal.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDateTime.*
import java.time.ZoneId
import java.util.UUID

class StoreOutboxMessageTest {

    private val outboxMongoRepository = mockk<OutboxMongoRepository>(relaxed = true)

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("UTC"))

    private val uuid = UUID.randomUUID()

    private val mapper = defaultObjectMapperForOutbox

    private val publishExternalIntegrationEvent = StoreOutboxMessage(
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
                    stream = "auctions",
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
    fun `should handle an auction opened event publishing an external integration event`() {
        val auction = Builders.buildAuction(status= Opened)
        val domainEvent = AuctionOpened(auction)

        publishExternalIntegrationEvent(domainEvent)

        verify {
            outboxMongoRepository.save(
                OutboxMessage(
                    id = uuid,
                    aggregateId = auction.id.value,
                    stream = "auctions",
                    payload = mapper.writeValueAsBytes(
                        AuctionOpenedEvent(
                            auction = AuctionIntegrationDto(
                                id = auction.id.value,
                                userId = auction.userId.value,
                                itemId = auction.itemId.value,
                                openingBid = auction.openingAmount.value,
                                minimalBid = auction.minimalAmount.value,
                                openingAt = auction.openingAt,
                                createdAt = auction.createdAt,
                                currentBid = null,
                                status = OPENED,
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
    fun `should handle a bid placed event publishing an external integration event`() {
        val ts = now()
        val bidderId = UserId(UUID.randomUUID())
        val auction = Builders.buildAuction(
            currentBid= Bid(bidderId, amount = Amount(TEN), ts = ts), status = Opened
        )
        val domainEvent = BidPlaced(auction)

        publishExternalIntegrationEvent(domainEvent)

        verify {
            outboxMongoRepository.save(
                OutboxMessage(
                    id = uuid,
                    aggregateId = auction.id.value,
                    stream = "auctions",
                    payload = mapper.writeValueAsBytes(
                        BidPlacedEvent(
                            auction = AuctionIntegrationDto(
                                id = auction.id.value,
                                userId = auction.userId.value,
                                itemId = auction.itemId.value,
                                openingBid = auction.openingAmount.value,
                                minimalBid = auction.minimalAmount.value,
                                openingAt = auction.openingAt,
                                createdAt = auction.createdAt,
                                currentBid = BidDto(bidderId.value, TEN, ts),
                                status = OPENED,
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
    fun `should handle an auction ended event publishing an external integration event`() {
        val ts = now()
        val bidderId = UserId(UUID.randomUUID())
        val auction = Builders.buildAuction(
            currentBid= Bid(bidderId, amount = Amount(TEN), ts = ts), status = Expired
        )
        val domainEvent = AuctionEnded(auction)

        publishExternalIntegrationEvent(domainEvent)

        verify {
            outboxMongoRepository.save(
                OutboxMessage(
                    id = uuid,
                    aggregateId = auction.id.value,
                    stream = "auctions",
                    payload = mapper.writeValueAsBytes(
                        AuctionEndedEvent(
                            auction = AuctionIntegrationDto(
                                id = auction.id.value,
                                userId = auction.userId.value,
                                itemId = auction.itemId.value,
                                openingBid = auction.openingAmount.value,
                                minimalBid = auction.minimalAmount.value,
                                openingAt = auction.openingAt,
                                createdAt = auction.createdAt,
                                currentBid = BidDto(bidderId.value, TEN, ts),
                                status = EXPIRED,
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
            StoreOutboxMessage(
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