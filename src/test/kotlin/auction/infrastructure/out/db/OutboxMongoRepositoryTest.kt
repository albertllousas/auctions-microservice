package auction.infrastructure.out.db

import auction.Mongo
import auction.fixtures.Builders
import auction.fixtures.MongoSessionHolderForTest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.litote.kmongo.findOneById
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime.parse
import java.time.ZoneId
import java.util.UUID

@Tag("integration")
class OutboxMongoRepositoryTest {

    private val mongo = Mongo()

    private val outboxMessages = mongo.getCollection<BucketOfMessages>("outbox.messages")

    private val sessionHolder = MongoSessionHolderForTest(mongo.client)

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.950Z"), ZoneId.of("UTC"))

    private val uuid = UUID.randomUUID()

    private val repository = OutboxMongoRepository(
        outboxMessages = outboxMessages,
        sessionHolder = sessionHolder,
        clock = clock,
        generateId = { uuid }
    )

    @AfterEach
    fun `tear down`() = mongo.container.stop()


    @Test
    fun `should save an outbox message`() {
        val outboxMessage = Builders.buildOutboxMessage()

        repository.save(outboxMessage)

        outboxMessages.findOneById(uuid.toString()) shouldBe BucketOfMessages(
            id = uuid.toString(),
            time = parse("2007-12-03T10:15:30.900"),
            bucket = listOf(outboxMessage),
        )
    }

    @Test
    fun `should find and remove the most recent outboxMessages`() {
        val firstMessage = Builders.buildOutboxMessage()
        val secondMessage = Builders.buildOutboxMessage()
        val thirdMessage = Builders.buildOutboxMessage()
        val repository = OutboxMongoRepository(outboxMessages, sessionHolder, Clock.systemUTC())
        repository.save(firstMessage)
        Thread.sleep(1000)
        repository.save(secondMessage)
        repository.save(thirdMessage)

        val firstResult = repository.findAndRemove()
        val secondResult = repository.findAndRemove()

        firstResult shouldBe listOf(firstMessage)
        secondResult shouldBe listOf(secondMessage, thirdMessage)
    }
}