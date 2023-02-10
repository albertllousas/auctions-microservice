package auction.infrastructure.out.db

import auction.Mongo
import auction.domain.model.AuctionId
import auction.fixtures.MongoSessionHolderForTest
import auction.infrastructure.out.db.AuctionTask.*
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.litote.kmongo.findOneById
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime.*
import java.time.ZoneId
import java.util.UUID.*

@Tag("integration")
class AuctionTasksMongoRepositoryTest {

    private val mongo = Mongo()

    private val openTasks = mongo.getCollection<BucketOfOpenTasks>("tasks.auction.open")

    private val endTasks = mongo.getCollection<BucketOfEndTasks>("tasks.auction.open")

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:45.01Z"), ZoneId.of("UTC"))

    private val sessionHolder = MongoSessionHolderForTest(mongo.client)

    private val uuid = randomUUID()

    private val repository = AuctionTasksMongoRepository(
        openTasks, endTasks, sessionHolder, clock, generateId = { uuid }
    )

    @AfterEach
    fun `tear down`() = mongo.container.stop()

    @Test
    fun `should save a task to open an auction`() {
        val startAt = now(clock).plusHours(1)
        val task = OpenAuctionTask(startAt, AuctionId(randomUUID()))

        repository.save(task)

        openTasks.findOneById(uuid.toString()) shouldBe BucketOfOpenTasks(
            id = uuid.toString(),
            bucket = listOf(task),
            time = parse("2007-12-03T11:15:40")
        )
    }

    @Test
    fun `should find and remove the pending auctions to start`() {
        val repository = AuctionTasksMongoRepository(openTasks, endTasks, sessionHolder, clock)
        val firstMessage = OpenAuctionTask(parse("2007-12-03T10:14:40"), AuctionId(randomUUID())).also(repository::save)
        val secondMessage = OpenAuctionTask(parse("2007-12-03T10:14:45"), AuctionId(randomUUID())).also(repository::save)
        val thirdMessage = OpenAuctionTask(parse("2007-12-03T10:15:31"), AuctionId(randomUUID())).also(repository::save)

        val firstResult = repository.findPendingToOpenAndRemove()
        val secondResult = repository.findPendingToOpenAndRemove()

        firstResult shouldBe listOf(firstMessage, secondMessage)
        secondResult shouldBe listOf(thirdMessage)
    }

    @Test
    fun `should save a task to end an auction`() {
        val expiringAt = now(clock).plusHours(1)
        val task = EndAuctionTask(expiringAt, AuctionId(randomUUID()))

        repository.save(task)

        endTasks.findOneById(uuid.toString()) shouldBe BucketOfEndTasks(
            id = uuid.toString(),
            bucket = listOf(task),
            time = parse("2007-12-03T11:15:40")
        )
    }

    @Test
    fun `should find and remove the pending auctions to end`() {
        val repository = AuctionTasksMongoRepository(openTasks, endTasks, sessionHolder, clock)
        val firstMessage = EndAuctionTask(parse("2007-12-03T10:14:40"), AuctionId(randomUUID())).also(repository::save)
        val secondMessage = EndAuctionTask(parse("2007-12-03T10:14:45"), AuctionId(randomUUID())).also(repository::save)
        val thirdMessage = EndAuctionTask(parse("2007-12-03T10:15:31"), AuctionId(randomUUID())).also(repository::save)

        val firstResult = repository.findPendingToEndAndRemove()
        val secondResult = repository.findPendingToEndAndRemove()

        firstResult shouldBe listOf(firstMessage, secondMessage)
        secondResult shouldBe listOf(thirdMessage)
    }
}
