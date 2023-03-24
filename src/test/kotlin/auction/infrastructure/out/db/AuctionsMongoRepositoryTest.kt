package auction.infrastructure.out.db

import arrow.core.left
import arrow.core.right
import auction.Mongo
import auction.domain.model.AggregateVersion
import auction.domain.model.AuctionId
import auction.domain.model.AuctionNotFound
import auction.fixtures.Builders
import auction.fixtures.MongoSessionHolderForTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.bson.types.Decimal128
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.litote.kmongo.findOneById
import java.time.LocalDateTime
import java.util.UUID

@Tag("integration")
class AuctionsMongoRepositoryTest {

    private val mongo = Mongo()

    private val mongoCollection = mongo.getCollection<AuctionDBDto>("auctions")

    private val sessionHolder = MongoSessionHolderForTest(mongo.client)

    private val adapter = AuctionsMongoRepository(mongoCollection, sessionHolder)

    @AfterEach
    fun `tear down`() = mongo.container.stop()

    @Test
    fun `should save a new auction to mongo db auctions collection`() {
        val auction = Builders.buildAuction(
            openingAt = LocalDateTime.parse("2022-12-25T07:12:21.695"),
            createdAt = LocalDateTime.parse("2022-12-16T07:12:21.697"),
            endAt = LocalDateTime.parse("2022-12-16T07:12:21.696"),
        )

        adapter.save(auction)

        mongoCollection.findOneById(auction.id.value.toString()) shouldBe AuctionDBDto(
            auction.id.value.toString(),
            auction.userId.value.toString(),
            auction.itemId.value.toString(),
            Decimal128(auction.openingAmount.value),
            Decimal128(auction.minimalAmount.value),
            auction.openingAt,
            auction.createdAt,
            AuctionDBStatus.ON_PREVIEW,
            1,
            0,
            null,
            auction.endAt,
            auction.sellToHighestBidPeriod.toMillis()
        )
    }

    @Test
    fun `should update an existent auction to mongo db auctions collection`() {
        val auction = Builders.buildAuction(
            openingAt = LocalDateTime.parse("2022-12-25T07:12:21.695"),
            createdAt = LocalDateTime.parse("2022-12-16T07:12:21.697"),
            endAt = LocalDateTime.parse("2022-12-16T07:12:21.696"),
        ).also { adapter.save(it) }
        val updatedAuction = Builders.buildAuction(
            id = auction.id,
            openingAt = LocalDateTime.parse("2022-12-25T08:12:21.695"),
            createdAt = LocalDateTime.parse("2022-12-16T08:12:21.697"),
            endAt = LocalDateTime.parse("2022-12-16T07:12:21.696"),
            version = AggregateVersion(1)
        )

        adapter.save(updatedAuction)

        mongoCollection.findOneById(auction.id.value.toString()) shouldBe AuctionDBDto(
            updatedAuction.id.value.toString(),
            updatedAuction.userId.value.toString(),
            updatedAuction.itemId.value.toString(),
            Decimal128(updatedAuction.openingAmount.value),
            Decimal128(updatedAuction.minimalAmount.value),
            updatedAuction.openingAt,
            updatedAuction.createdAt,
            AuctionDBStatus.ON_PREVIEW,
            2,
            0,
            null,
            updatedAuction.endAt,
            updatedAuction.sellToHighestBidPeriod.toMillis()
        )
    }

    @Test
    fun `should fail updating an existent auction when there is a concurrency issue`() {
        val auction = Builders.buildAuction().also { adapter.save(it) }

        shouldThrow<OptimisticLockException> { adapter.save(auction) }
    }

    @Test
    fun `should find an auction`() {
        val auction = Builders.buildAuction(
            openingAt = LocalDateTime.parse("2022-12-25T07:12:21.695"),
            createdAt = LocalDateTime.parse("2022-12-16T07:12:21.697"),
            endAt = LocalDateTime.parse("2022-12-16T07:12:21.699"),
        ).also { adapter.save(it) }

        val result = adapter.find(auction.id)

        result shouldBe auction.copy(version = AggregateVersion(1)).right()
    }

    @Test
    fun `should not find a non existent auction`() {
        val result = adapter.find(AuctionId(UUID.randomUUID()))

        result shouldBe AuctionNotFound.left()
    }
}
