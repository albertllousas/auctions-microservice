package auction.infrastructure.out.db

import arrow.core.Option
import auction.domain.model.AggregateVersion
import auction.domain.model.Amount
import auction.domain.model.Auction
import auction.domain.model.AuctionId
import auction.domain.model.AuctionNotFound
import auction.domain.model.AuctionStatus.Expired
import auction.domain.model.AuctionStatus.ItemSold
import auction.domain.model.AuctionStatus.OnPreview
import auction.domain.model.AuctionStatus.Opened
import auction.domain.model.Bid
import auction.domain.model.FindAuction
import auction.domain.model.ItemId
import auction.domain.model.SaveAuction
import auction.domain.model.UserId
import auction.infrastructure.out.db.AuctionDBStatus.ENDED
import auction.infrastructure.out.db.AuctionDBStatus.EXPIRED
import auction.infrastructure.out.db.AuctionDBStatus.ON_PREVIEW
import auction.infrastructure.out.db.AuctionDBStatus.OPENED
import com.mongodb.BasicDBObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.IndexOptions
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.Decimal128
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

private val upserting = FindOneAndReplaceOptions().upsert(true)

class AuctionsMongoRepository(
    private val mongoCollection: MongoCollection<AuctionDBDto>,
    private val sessionHolder: MongoSessionHolder,
) {

    init {
        mongoCollection.createIndex(BasicDBObject("itemId", 1), IndexOptions().unique(true))
    }

    val save: SaveAuction = { auction ->
        Auction.increaseVersion(auction)
            .let(AuctionDBDto::from)
            .let { mongoCollection.findOneAndReplace(sessionHolder.get(), AuctionDBDto::id eq it.id, it, upserting) }
            .also { replacedAuction -> checkConcurrentAccess(replacedAuction, auction) }
    }

    val find: FindAuction = { auction ->
        mongoCollection.findOne(sessionHolder.get(), AuctionDBDto::id eq auction.value.toString())
            .let { dto -> Option.fromNullable(dto) }
            .map { dto -> AuctionDBDto.to(dto) }
            .toEither(ifEmpty = { AuctionNotFound })
    }

    private fun checkConcurrentAccess(prevDBDto: AuctionDBDto?, prevAuction: Auction) {
        if (prevDBDto != null && (prevDBDto.version != prevAuction.version.value))
            throw OptimisticLockException(prevAuction.id.value)
    }
}

data class AuctionDBDto(
    @BsonId val id: String,
    val userId: String,
    val itemId: String,
    val openingBid: Decimal128,
    val minimalBid: Decimal128,
    val openingAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val status: AuctionDBStatus,
    val version: Long,
    val currentBidNumber: Long,
    val currentBid: BidDBDto?,
    val endAt: LocalDateTime,
    val sellToHighestBidPeriodInMs: Long
) {
    companion object {
        fun from(auction: Auction) = AuctionDBDto(
            id = auction.id.value.toString(),
            userId = auction.userId.value.toString(),
            itemId = auction.itemId.value.toString(),
            openingBid = Decimal128(auction.openingAmount.value),
            minimalBid = Decimal128(auction.minimalAmount.value),
            openingAt = auction.openingAt,
            createdAt = auction.createdAt,
            status = when (auction.status) {
                OnPreview -> ON_PREVIEW
                Opened -> OPENED
                is ItemSold -> ENDED
                Expired -> EXPIRED
            },
            version = auction.version.value,
            currentBidNumber = auction.bidsCounter,
            currentBid = auction.currentBid?.let {
                BidDBDto(it.bidderId.value.toString(), Decimal128(it.amount.value), it.ts)
            },
            endAt = auction.endAt,
            sellToHighestBidPeriodInMs = auction.sellToHighestBidPeriod.toMillis()
        )

        fun to(dto: AuctionDBDto) = Auction.reconstitute(
            id = AuctionId(UUID.fromString(dto.id)),
            userId = UserId(UUID.fromString(dto.userId)),
            itemId = ItemId(UUID.fromString(dto.itemId)),
            openingAmount = Amount(dto.openingBid.bigDecimalValue()),
            minimalAmount = Amount(dto.minimalBid.bigDecimalValue()),
            openingAt = dto.openingAt,
            createdAt = dto.createdAt,
            status = when (dto.status) {
                ON_PREVIEW -> OnPreview
                OPENED -> Opened
                EXPIRED -> Expired
                ENDED -> ItemSold(winner = UserId(UUID.fromString(dto.currentBid!!.bidderId)))
            },
            version = AggregateVersion(dto.version),
            currentBidNumber = dto.currentBidNumber,
            currentBid = dto.currentBid?.let { bid ->
                Bid(
                    UserId(UUID.fromString(bid.bidderId)),
                    Amount(bid.amount.bigDecimalValue()),
                    bid.ts
                )
            },
            endAt = dto.endAt,
            sellToHighestBidPeriod = Duration.ofMillis(dto.sellToHighestBidPeriodInMs)
        )
    }
}

data class BidDBDto(val bidderId: String, val amount: Decimal128, val ts: LocalDateTime)

enum class AuctionDBStatus { ON_PREVIEW, OPENED, EXPIRED, ENDED }

data class OptimisticLockException(val aggregateInConflict: UUID) :
    Exception("There has been a concurrent access to auction aggregate with id '$aggregateInConflict'")
