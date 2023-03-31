package auction.infrastructure.out.db

import arrow.core.Option
import auction.domain.model.Amount
import auction.domain.model.AuctionId
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidId
import auction.domain.model.AutoBidNotFound
import auction.domain.model.FindAutoBid
import auction.domain.model.FindAutoBidsByAuction
import auction.domain.model.SaveAutoBid
import auction.domain.model.UserId
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.FindOneAndReplaceOptions
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.Decimal128
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.util.UUID

private val upserting = FindOneAndReplaceOptions().upsert(true)

class AutoBidsMongoRepository(
    private val mongoCollection: MongoCollection<AutoBidDBDto>,
    private val sessionHolder: MongoSessionHolder,
) {

    val save: SaveAutoBid = { autoBid ->
        AutoBidDBDto.from(autoBid)
            .let { mongoCollection.findOneAndReplace(sessionHolder.get(), AutoBidDBDto::id eq it.id, it, upserting) }
    }

    val find: FindAutoBid = { autoBid ->
        mongoCollection.findOne(sessionHolder.get(), AutoBidDBDto::id eq autoBid.value.toString())
            .let { dto -> Option.fromNullable(dto) }
            .map { dto -> AutoBidDBDto.toDomain(dto) }
            .toEither(ifEmpty = { AutoBidNotFound })
    }

    val findAutoBidsByAuction: FindAutoBidsByAuction = { auctionId ->
        mongoCollection.find(sessionHolder.get(), AutoBidDBDto::auctionId eq auctionId.value.toString())
            .map { dto: AutoBidDBDto -> AutoBidDBDto.toDomain(dto) }
            .toList()
    }
}

data class AutoBidDBDto(
    @BsonId val id: String,
    val auctionId: String,
    val userId: String,
    val amount: Decimal128,
    val limit: Decimal128,
    val enabled: Boolean
) {
    companion object {

        fun from(autoBid: AutoBid) = AutoBidDBDto(
            id = autoBid.id.value.toString(),
            auctionId = autoBid.auctionId.value.toString(),
            userId = autoBid.userId.value.toString(),
            amount = Decimal128(autoBid.amount.value),
            limit = Decimal128(autoBid.limit.value),
            enabled = autoBid.enabled
        )

        fun toDomain(dto: AutoBidDBDto) = AutoBid(
            id = AutoBidId(UUID.fromString(dto.id)),
            auctionId = AuctionId(UUID.fromString(dto.auctionId)),
            userId = UserId(UUID.fromString(dto.userId)),
            amount = Amount(dto.amount.bigDecimalValue()),
            limit = Amount(dto.limit.bigDecimalValue()),
            enabled = dto.enabled
        )
    }
}
