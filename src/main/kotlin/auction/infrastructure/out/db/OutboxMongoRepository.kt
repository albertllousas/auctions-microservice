package auction.infrastructure.out.db

import auction.infrastructure.out.db.OutboxMongoRepository.BucketingTime.MILLIS_100
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.push
import com.mongodb.client.model.Updates.setOnInsert
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.ascendingSort
import org.litote.kmongo.eq
import org.litote.kmongo.updateOne
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoField.MILLI_OF_SECOND
import java.util.UUID

private val upserting = UpdateOptions().upsert(true)

class OutboxMongoRepository(
    private val outboxMessages: MongoCollection<BucketOfMessages>,
    private val sessionHolder: MongoSessionHolder,
    private val clock: Clock,
    private val bucketingTimeWindow: BucketingTime = MILLIS_100,
    private val generateId: () -> UUID = { UUID.randomUUID() }
) {

    fun save(outboxMessage: OutboxMessage) {
        val now = now(clock)
        val millis = (now.get(MILLI_OF_SECOND) / bucketingTimeWindow.millis) * bucketingTimeWindow.millis
        val time = now.with(MILLI_OF_SECOND, millis.toLong())
        outboxMessages.updateOne(
            clientSession = sessionHolder.get(),
            filter = BucketOfMessages::time eq time,
            target = combine(
                push("bucket", outboxMessage),
                setOnInsert("_id", generateId().toString()),
                setOnInsert("time", time)
            ),
            options = upserting
        )
    }

    fun findAndRemove(): List<OutboxMessage> =
        outboxMessages.find(sessionHolder.get()).ascendingSort(BucketOfMessages::time).limit(1).first()
            ?.let { outboxMessages.findOneAndDelete(sessionHolder.get(), BucketOfMessages::id eq it.id) }
            ?.bucket
            ?: emptyList()

    enum class BucketingTime(val millis: Int) {
        MILLIS_500(500), MILLIS_250(250), MILLIS_200(200), MILLIS_100(100), MILLIS_50(50)
    }
}


data class BucketOfMessages(
    @BsonId val id: String,
    val time: LocalDateTime,
    val bucket: List<OutboxMessage>
)

enum class MessagingSystem { KAFKA }

data class OutboxMessage(
    val id: UUID,
    val aggregateId: UUID,
    val messagingSystem: MessagingSystem,
    val target: String,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutboxMessage

        if (id != other.id) return false
        if (aggregateId != other.aggregateId) return false
        if (messagingSystem != other.messagingSystem) return false
        if (target != other.target) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + aggregateId.hashCode()
        result = 31 * result + messagingSystem.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

}