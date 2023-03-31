package auction.infrastructure.out.db

import arrow.core.left
import arrow.core.right
import auction.Mongo
import auction.domain.model.AuctionId
import auction.domain.model.AutoBidId
import auction.domain.model.AutoBidNotFound
import auction.fixtures.Builders
import auction.fixtures.MongoSessionHolderForTest
import io.kotest.matchers.shouldBe
import org.bson.types.Decimal128
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.litote.kmongo.findOneById
import java.util.UUID


@Tag("integration")
class AutoBidsMongoRepositoryTest {

    private val mongo = Mongo()

    private val mongoCollection = mongo.getCollection<AutoBidDBDto>("autobids")

    private val sessionHolder = MongoSessionHolderForTest(mongo.client)

    private val adapter = AutoBidsMongoRepository(mongoCollection, sessionHolder)

    @AfterEach
    fun `tear down`() = mongo.container.stop()

    @Test
    fun `should save a new auto-bid to mongo db autobids collection`() {
        val autoBid = Builders.buildAutoBid()

        adapter.save(autoBid)

        mongoCollection.findOneById(autoBid.id.value.toString()) shouldBe AutoBidDBDto(
            id = autoBid.id.value.toString(),
            auctionId = autoBid.auctionId.value.toString(),
            userId = autoBid.userId.value.toString(),
            amount = Decimal128(autoBid.amount.value),
            limit = Decimal128(autoBid.limit.value),
            enabled = autoBid.enabled
        )
    }

    @Test
    fun `should update an existent auction to mongo db auctions collection`() {
        val autoBid = Builders.buildAutoBid().also { adapter.save(it) }
        val updatedAutoBid = Builders.buildAutoBid(id = autoBid.id, enabled = false)

        adapter.save(updatedAutoBid)

        mongoCollection.findOneById(autoBid.id.value.toString()) shouldBe AutoBidDBDto(
            id = updatedAutoBid.id.value.toString(),
            auctionId = updatedAutoBid.auctionId.value.toString(),
            userId = updatedAutoBid.userId.value.toString(),
            amount = Decimal128(updatedAutoBid.amount.value),
            limit = Decimal128(updatedAutoBid.limit.value),
            enabled = false
        )
    }

    @Test
    fun `should find an auction`() {
        val autoBid = Builders.buildAutoBid().also { adapter.save(it) }

        val result = adapter.find(autoBid.id)

        result shouldBe autoBid.right()
    }

    @Test
    fun `should not find a non existent auction`() {
        val result = adapter.find(AutoBidId(UUID.randomUUID()))

        result shouldBe AutoBidNotFound.left()
    }

    @Test
    fun `should find a list of auto bids for an auction`() {
        val auctionId = AuctionId(UUID.randomUUID())
        val fstAutoBid = Builders.buildAutoBid(auctionId = auctionId).also { adapter.save(it) }
        Builders.buildAutoBid().also { adapter.save(it) }
        val sndAutoBid = Builders.buildAutoBid(auctionId = auctionId).also { adapter.save(it) }

        val result = adapter.findAutoBidsByAuction(auctionId)

        result shouldBe listOf(fstAutoBid, sndAutoBid)
    }
}
