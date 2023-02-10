package auction.infrastructure.out.schedule

import auction.domain.model.WithinTransaction
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.stream.PublishOutboxMessageToKafka
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import java.lang.invoke.MethodHandles
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.*

private const val FAIL_COUNTER = "outbox.publishing.fail"

private const val SUCCESS_COUNTER = "outbox.publishing.success"

class PollOutboxForPublishing (
    private val outboxMongoRepository: OutboxMongoRepository,
    private val publishOutboxMessageToKafka: PublishOutboxMessageToKafka,
    scheduler: TaskScheduler,
    private val withinTransaction: WithinTransaction,
    private val meterRegistry: MeterRegistry,
    pollingIntervalMs: Long = 50L,
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass()),
) {

    init {
        scheduler.scheduleWithFixedDelay(this::invoke, pollingIntervalMs)
    }

    operator fun invoke() {
        try {
            withinTransaction {
                outboxMongoRepository.findAndRemove()
                    .also { publishOutboxMessageToKafka(it) }
                    .also { meterRegistry.counter(SUCCESS_COUNTER).increment(it.size.toDouble()) }
                    .forEach { logger.info("Message '${it.aggregateId}' published to '${it.stream}'") }
            }
        } catch (exception: Exception) {
            meterRegistry.counter(FAIL_COUNTER).increment()
            logger.error("Message batch publishing failed, will be retried", exception)
            throw exception
        }
    }
}
