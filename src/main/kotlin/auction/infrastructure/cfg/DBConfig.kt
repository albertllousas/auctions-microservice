package auction.infrastructure.cfg

import auction.domain.model.FindAuction
import auction.domain.model.FindAutoBid
import auction.domain.model.FindAutoBidsByAuction
import auction.domain.model.SaveAuction
import auction.domain.model.SaveAutoBid
import auction.infrastructure.out.db.AuctionDBDto
import auction.infrastructure.out.db.AuctionTasksMongoRepository
import auction.infrastructure.out.db.AuctionsMongoRepository
import auction.infrastructure.out.db.AutoBidDBDto
import auction.infrastructure.out.db.AutoBidsMongoRepository
import auction.infrastructure.out.db.BucketOfEndTasks
import auction.infrastructure.out.db.BucketOfMessages
import auction.infrastructure.out.db.BucketOfOpenTasks
import auction.infrastructure.out.db.IsAnActiveTransaction
import auction.infrastructure.out.db.MongoSessionHolder
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.db.ThreadLocalMongoSessionHolder
import auction.infrastructure.out.db.WithinMongoTransaction
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.UuidRepresentation.STANDARD
import org.litote.kmongo.KMongo
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import org.litote.kmongo.getCollection as getColl

@Configuration
class DBConfig {

    @Bean
    fun mongoClient(@Value("\${db.mongo.connection-string}") connectionString: String) =
        MongoClientSettings.builder()
            .uuidRepresentation(STANDARD)
            .applyConnectionString(ConnectionString(connectionString))
            .build()
            .let(KMongo::createClient)

    @Bean
    fun mongoDatabase(@Value("\${db.mongo.database}") database: String, mongoClient: MongoClient): MongoDatabase =
        mongoClient.getDatabase(database)

    @Bean
    fun auctionsCollection(mongoDatabase: MongoDatabase) =
        mongoDatabase.getColl<AuctionDBDto>("auctions")

    @Bean
    fun autoBidsCollection(mongoDatabase: MongoDatabase) =
        mongoDatabase.getColl<AutoBidDBDto>("autobids")

    @Bean
    fun openTasksCollection(mongoDatabase: MongoDatabase): MongoCollection<BucketOfOpenTasks> =
        mongoDatabase.getColl("tasks.auction.open")

    @Bean
    fun endTasksCollection(mongoDatabase: MongoDatabase): MongoCollection<BucketOfEndTasks> =
        mongoDatabase.getColl("tasks.auction.end")

    @Bean
    fun checkWinnersTasksCollection(mongoDatabase: MongoDatabase): MongoCollection<BucketOfMessages> =
        mongoDatabase.getColl("outbox.messages")

    @Bean
    fun auctionsMongoRepository(
        auctionsCollection: MongoCollection<AuctionDBDto>,
        sessionHolder: MongoSessionHolder,
    ) = AuctionsMongoRepository(auctionsCollection, sessionHolder)

    @Bean
    fun saveAuction(auctionsMongoRepository: AuctionsMongoRepository): SaveAuction = auctionsMongoRepository.save

    @Bean
    fun findAuction(auctionsMongoRepository: AuctionsMongoRepository): FindAuction = auctionsMongoRepository.find

    @Bean
    fun autoBidsMongoRepository(
        autoBidsCollection: MongoCollection<AutoBidDBDto>,
        sessionHolder: MongoSessionHolder,
    ) = AutoBidsMongoRepository(autoBidsCollection, sessionHolder)

    @Bean
    fun saveAutoBid(autoBidsMongoRepository: AutoBidsMongoRepository): SaveAutoBid = autoBidsMongoRepository.save

    @Bean
    fun findAutoBid(autoBidsMongoRepository: AutoBidsMongoRepository): FindAutoBid = autoBidsMongoRepository.find

    @Bean
    fun findAutoBidsByAuction(autoBidsMongoRepository: AutoBidsMongoRepository): FindAutoBidsByAuction =
        autoBidsMongoRepository.findAutoBidsByAuction

    @Bean
    fun sessionHolder(mongoClient: MongoClient) = ThreadLocalMongoSessionHolder(mongoClient)

    @Bean
    fun withinTransaction(sessionHolder: MongoSessionHolder) = WithinMongoTransaction(sessionHolder)

    @Bean
    fun isInActiveTransaction(sessionHolder: MongoSessionHolder) = IsAnActiveTransaction(sessionHolder)

    @Bean
    fun auctionTasksMongoRepository(
        openTasksCollection: MongoCollection<BucketOfOpenTasks>,
        endTasksCollection: MongoCollection<BucketOfEndTasks>,
        sessionHolder: MongoSessionHolder,
        clock: Clock
    ) = AuctionTasksMongoRepository(
        openTasksCollection,
        endTasksCollection,
        sessionHolder,
        clock
    )

    @Bean
    fun outboxMongoRepository(
        outboxMessagesCollection: MongoCollection<BucketOfMessages>,
        sessionHolder: MongoSessionHolder,
        clock: Clock
    ) = OutboxMongoRepository(outboxMessagesCollection, sessionHolder, clock)
}