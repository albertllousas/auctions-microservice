package auction.infrastructure.out.events

import auction.domain.model.Auction
import auction.domain.model.AuctionCreated
import auction.domain.model.AuctionEnded
import auction.domain.model.AuctionOpened
import auction.domain.model.AuctionStatus.ItemSold
import auction.domain.model.AuctionStatus.Expired
import auction.domain.model.AuctionStatus.OnPreview
import auction.domain.model.AuctionStatus.Opened
import auction.domain.model.BidPlaced
import auction.domain.model.DomainEvent
import auction.domain.model.HandleEvent
import auction.infrastructure.out.db.OutboxMessage
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.ITEM_SOLD
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.EXPIRED
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.ON_PREVIEW
import auction.infrastructure.out.events.AuctionIntegrationDto.AuctionStatus.OPENED
import auction.infrastructure.out.events.AuctionIntegrationEvent.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

val defaultObjectMapperForOutbox = jacksonObjectMapper().findAndRegisterModules()

class StoreOutboxMessage(
    private val isAnActiveTransaction: () -> Boolean,
    private val auctionsStreamName: String,
    private val outboxMongoRepository: OutboxMongoRepository,
    private val clock: Clock,
    private val mapper: ObjectMapper = defaultObjectMapperForOutbox,
    private val generateEventId: () -> UUID = { UUID.randomUUID() }
) : HandleEvent {

    override operator fun invoke(event: DomainEvent) {
        if (!isAnActiveTransaction()) throw NonActiveTransactionException
        when (event) {
            is AuctionCreated ->
                Pair(AuctionCreatedEvent(event.auction.asIntegrationDto(), now(clock), generateEventId()), event.auction.id)

            is AuctionOpened ->
                Pair(AuctionOpenedEvent(event.auction.asIntegrationDto(), now(clock), generateEventId()), event.auction.id)

            is BidPlaced ->
                Pair(BidPlacedEvent(event.auction.asIntegrationDto(), now(clock), generateEventId()), event.auction.id)

            is AuctionEnded ->
                Pair(AuctionEndedEvent(event.auction.asIntegrationDto(), now(clock), generateEventId()), event.auction.id)
        }
            .let { (integrationEvent, aggregateId) ->
                OutboxMessage(
                    id = integrationEvent.eventId,
                    aggregateId = aggregateId.value,
                    stream = auctionsStreamName,
                    payload = mapper.writeValueAsBytes(integrationEvent)
                )
            }
            .also(outboxMongoRepository::save)
    }

    private fun Auction.asIntegrationDto() =
        AuctionIntegrationDto(
            id = id.value,
            userId = userId.value,
            itemId = itemId.value,
            openingBid = openingAmount.value,
            minimalBid = minimalAmount.value,
            openingAt = openingAt,
            createdAt = createdAt,
            status = when (status) {
                OnPreview -> ON_PREVIEW
                Opened -> OPENED
                is ItemSold -> ITEM_SOLD
                Expired -> EXPIRED
            },
            currentBid = currentBid?.let {
                AuctionIntegrationDto.BidDto(currentBid.bidderId.value, currentBid.amount.value, currentBid.ts)
            }
        )
}

object NonActiveTransactionException : Exception()

/*
Auction Integration event: Auction event interesting to other domains, applications or third party services.
Usually these definitions are imported from the scheme registry.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "event_type")
@JsonSubTypes(
    JsonSubTypes.Type(value = AuctionCreatedEvent::class, name = "auction_created_event"),
    JsonSubTypes.Type(value = AuctionOpenedEvent::class, name = "auction_opened_event"),
    JsonSubTypes.Type(value = BidPlacedEvent::class, name = "bid_placed_event"),
    JsonSubTypes.Type(value = AuctionEndedEvent::class, name = "auction_ended_event")
)
sealed class AuctionIntegrationEvent(
    @get:JsonProperty("event_type") val eventType: String
) {
    @get:JsonProperty("occurred_on")
    abstract val occurredOn: LocalDateTime

    @get:JsonProperty("event_id")
    abstract val eventId: UUID

    data class AuctionCreatedEvent(
        val auction: AuctionIntegrationDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
    ) : AuctionIntegrationEvent("auction_created_event")

    data class AuctionOpenedEvent(
        val auction: AuctionIntegrationDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
    ) : AuctionIntegrationEvent("auction_opened_event")

    data class BidPlacedEvent(
        val auction: AuctionIntegrationDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
    ) : AuctionIntegrationEvent("bid_placed_event")

    data class AuctionEndedEvent(
        val auction: AuctionIntegrationDto,
        override val occurredOn: LocalDateTime,
        override val eventId: UUID,
    ) : AuctionIntegrationEvent("auction_ended_event")
}

data class AuctionIntegrationDto(
    val id: UUID,
    @get:JsonProperty("user_id") val userId: UUID,
    @get:JsonProperty("item_id") val itemId: UUID,
    @get:JsonProperty("opening_bid") val openingBid: BigDecimal,
    @get:JsonProperty("minimal_bid") val minimalBid: BigDecimal,
    @get:JsonProperty("opening_at") val openingAt: LocalDateTime,
    @get:JsonProperty("created_at") val createdAt: LocalDateTime,
    @get:JsonProperty("current_bid") val currentBid: BidDto?,
    val status: AuctionStatus
) {
    enum class AuctionStatus { ON_PREVIEW, OPENED, ITEM_SOLD, EXPIRED }

    data class BidDto(val bidderId: UUID, val amount: BigDecimal, val ts: LocalDateTime)
}



