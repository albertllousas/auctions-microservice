package auction.infrastructure.out.db

import auction.domain.model.AuctionId
import auction.domain.model.exhaustive
import auction.infrastructure.out.db.AuctionTask.EndAuctionTask
import auction.infrastructure.out.db.AuctionTask.OpenAuctionTask
import auction.infrastructure.out.db.AuctionTasksMongoRepository.BucketingTime.SECONDS_10
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.push
import com.mongodb.client.model.Updates.setOnInsert
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.and
import org.litote.kmongo.ascendingSort
import org.litote.kmongo.eq
import org.litote.kmongo.lte
import org.litote.kmongo.updateOne
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoField.MILLI_OF_SECOND
import java.time.temporal.ChronoField.SECOND_OF_MINUTE
import java.util.UUID

private val upserting = UpdateOptions().upsert(true)

class AuctionTasksMongoRepository(
    private val openAuctionTasks: MongoCollection<BucketOfOpenTasks>,
    private val endAuctionTasks: MongoCollection<BucketOfEndTasks>,
    private val sessionHolder: MongoSessionHolder,
    private val clock: Clock,
    private val bucketingTimeWindow: BucketingTime = SECONDS_10,
    private val generateId: () -> UUID = { UUID.randomUUID() }
) {

    init {
        openAuctionTasks.createIndex(Indexes.ascending("time"))
        endAuctionTasks.createIndex(Indexes.ascending("time"))
    }

    fun save(task: AuctionTask) {
        when (task) {
            is OpenAuctionTask ->
                openAuctionTasks.updateOne(
                    clientSession = sessionHolder.get(),
                    filter = BucketOfOpenTasks::time eq fitIntoBucketingTimeWindow(task.openingAt),
                    target = combine(
                        push("bucket", task),
                        setOnInsert("_id", generateId().toString()),
                        setOnInsert("time", fitIntoBucketingTimeWindow(task.openingAt))
                    ),
                    options = upserting
                )

            is EndAuctionTask -> endAuctionTasks.updateOne(
                clientSession = sessionHolder.get(),
                filter = BucketOfEndTasks::time eq fitIntoBucketingTimeWindow(task.endAt),
                target = combine(
                    push("bucket", task),
                    setOnInsert("_id", generateId().toString()),
                    setOnInsert("time", fitIntoBucketingTimeWindow(task.endAt))
                ),
                options = upserting
            )
        }.exhaustive
    }

    fun findPendingToOpenAndRemove(): List<OpenAuctionTask> {
        val now = now(clock)
        return openAuctionTasks.find(
            sessionHolder.get(),
            and(BucketOfOpenTasks::time lte now)
        )
            .ascendingSort(BucketOfOpenTasks::time)
            .limit(1)
            .first()
            ?.let { openAuctionTasks.findOneAndDelete(sessionHolder.get(), BucketOfOpenTasks::id eq it.id) }
            ?.bucket
            ?: emptyList()
    }

    fun findPendingToEndAndRemove(): List<EndAuctionTask> {
        val now = now(clock)
        return endAuctionTasks.find(
            sessionHolder.get(),
            and(BucketOfEndTasks::time lte now)
        )
            .ascendingSort(BucketOfEndTasks::time)
            .limit(1)
            .first()
            ?.let { endAuctionTasks.findOneAndDelete(sessionHolder.get(), BucketOfEndTasks::id eq it.id) }
            ?.bucket
            ?: emptyList()
    }

    private fun fitIntoBucketingTimeWindow(time: LocalDateTime): LocalDateTime {
        val seconds = (time.get(SECOND_OF_MINUTE) / bucketingTimeWindow.seconds) * bucketingTimeWindow.seconds
        return time.with(SECOND_OF_MINUTE, seconds.toLong()).with(MILLI_OF_SECOND, 0)
    }

    enum class BucketingTime(val seconds: Int) {
        SECONDS_30(30), SECONDS_15(15), SECONDS_10(10), SECONDS_5(5), SECONDS_1(1)
    }
}

sealed class AuctionTask {
    data class OpenAuctionTask(val openingAt: LocalDateTime, val auctionId: AuctionId) : AuctionTask()
    data class EndAuctionTask(val endAt: LocalDateTime, val auctionId: AuctionId) : AuctionTask()
}

data class BucketOfOpenTasks(@BsonId val id: String, val bucket: List<OpenAuctionTask>, val time: LocalDateTime)

data class BucketOfEndTasks(@BsonId val id: String, val bucket: List<EndAuctionTask>, val time: LocalDateTime)
