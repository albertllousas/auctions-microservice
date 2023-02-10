package auction.infrastructure.out.schedule

import auction.fixtures.Builders
import auction.fixtures.FakeWithinTransaction
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.stream.PublishOutboxMessageToKafka
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

class PollOutboxForPublishingTest {

    private val outboxRepository = mockk<OutboxMongoRepository>(relaxed = true)

    private val publishOutboxMessageToKafka = mockk<PublishOutboxMessageToKafka>(relaxed = true)

    private val meterRegistry = SimpleMeterRegistry()

    private val taskScheduler = ThreadPoolTaskScheduler().also { it.initialize() }

    init {
        PollOutboxForPublishing(
            outboxRepository, publishOutboxMessageToKafka, taskScheduler, FakeWithinTransaction, meterRegistry
        )
    }

    @Test
    fun `should poll and publish outbox messages`() {
        val outboxEvent = Builders.buildOutboxMessage()
        every { outboxRepository.findAndRemove() } returns listOf(outboxEvent)

        verify(timeout = 1000, atLeast = 5) {
            publishOutboxMessageToKafka.invoke(listOf(outboxEvent))
        }
        assertThat(meterRegistry.counter("outbox.publishing.fail").count()).isEqualTo(0.0)
        assertThat(meterRegistry.counter("outbox.publishing.success").count()).isGreaterThan(1.0)
    }

    @Test
    fun `should report and keep polling when process crashes`() {
        val outboxEvent = Builders.buildOutboxMessage()
        every { outboxRepository.findAndRemove() } throws Exception("boom!") andThen listOf(outboxEvent)

        verify(timeout = 1000, atLeast = 5) {
            publishOutboxMessageToKafka.invoke(listOf(outboxEvent))
        }
        assertThat(meterRegistry.counter("outbox.publishing.fail").count()).isEqualTo(1.0)
        assertThat(meterRegistry.counter("outbox.publishing.success").count()).isGreaterThan(1.0)
    }
}