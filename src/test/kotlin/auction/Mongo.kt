package auction

import auction.domain.model.AuctionId
import auction.infrastructure.out.db.AuctionDBDto
import auction.infrastructure.out.db.AuctionDBStatus
import auction.infrastructure.out.db.AuctionDBStatus.ON_PREVIEW
import auction.infrastructure.out.db.AuctionTask
import auction.infrastructure.out.db.AuctionTask.*
import auction.infrastructure.out.db.BucketOfEndTasks
import auction.infrastructure.out.db.BucketOfOpenTasks
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoDatabase
import org.bson.UuidRepresentation
import org.bson.types.Decimal128
import org.litote.kmongo.KMongo
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import org.litote.kmongo.getCollection as getColl

class Mongo {

    val container = MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
        .also {
            it.start()
            println(it.logs)
            it.waitingFor(Wait.forListeningPort())
        }

    val client = MongoClientSettings.builder()
        .uuidRepresentation(UuidRepresentation.STANDARD)
        .applyConnectionString(
            ConnectionString("mongodb://${container.host}:${container.firstMappedPort}") //try to pass only conn string from config, no user or password
        )
        .build()
        .let(KMongo::createClient)

    val db: MongoDatabase = client.getDatabase("test")
        .apply {
            createCollection("auctions")
            createCollection("tasks.auction.open")
            createCollection("tasks.auction.end")
            createCollection("outbox.messages")
        }


    inline fun <reified T : Any> getCollection(name: String) = db.getColl<T>(name)

    fun givenAnAuctionExists(
        openingAt: LocalDateTime = LocalDateTime.now(),
        endAt: LocalDateTime = LocalDateTime.now().plusMinutes(60),
        status: AuctionDBStatus = ON_PREVIEW,
    ): UUID {
        val id = UUID.randomUUID()
        db.getColl<AuctionDBDto>("auctions")
            .insertOne(
                AuctionDBDto(
                    id.toString(),
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    Decimal128(BigDecimal.TEN),
                    Decimal128(BigDecimal.ONE),
                    openingAt,
                    LocalDateTime.now(),
                    status,
                    version = 1,
                    0,
                    null,
                    endAt,
                    Duration.ofMinutes(20).toMillis()
                )
            )
        return id
    }

    fun givenATaskToOpenAnAuctionExists(startingAt: LocalDateTime, auctionId: UUID): UUID {
        val id = UUID.randomUUID()
        db.getColl<BucketOfOpenTasks>("tasks.auction.open")
            .insertOne(
                BucketOfOpenTasks(
                    id = UUID.randomUUID().toString(),
                    bucket = listOf(OpenAuctionTask(startingAt, AuctionId(auctionId))),
                    time = startingAt
                )
            )
        return id
    }

    fun givenATaskToEndAnAuctionExists(endAt: LocalDateTime, auctionId: UUID): UUID {
        val id = UUID.randomUUID()
        db.getColl<BucketOfEndTasks>("tasks.auction.end")
            .insertOne(
                BucketOfEndTasks(
                    id = UUID.randomUUID().toString(),
                    bucket = listOf(EndAuctionTask(endAt, AuctionId(auctionId))),
                    time = endAt
                )
            )
        return id
    }
}
