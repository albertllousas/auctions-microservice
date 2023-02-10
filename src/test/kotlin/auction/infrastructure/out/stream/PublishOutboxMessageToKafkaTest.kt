package auction.infrastructure.out.stream

import auction.Kafka
import auction.fixtures.Builders
import auction.fixtures.buildKafkaConsumer
import auction.fixtures.buildKafkaProducer
import auction.fixtures.consumeAndAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class PublishOutboxMessageToKafkaTest {

    private val kafka = Kafka()

    private val firstKafkaConsumer = buildKafkaConsumer(kafka.container.bootstrapServers, "consumer-1")
        .also { it.subscribe(listOf("topic")) }

    private val secondKafkaConsumer = buildKafkaConsumer(kafka.container.bootstrapServers, "consumer-2")
        .also { it.subscribe(listOf("topic-2")) }

    private val kafkaProducer = buildKafkaProducer(kafka.container.bootstrapServers)

    private val publishOutboxMessageToKafka = PublishOutboxMessageToKafka(kafkaProducer)

    @Test
    fun `send a batch of outbox messages successfully to different streams`() {
        val oneOutboxEvent = Builders.buildOutboxMessage(stream = "topic")
        val anotherOutboxEvent = Builders.buildOutboxMessage(stream = "topic-2")

        publishOutboxMessageToKafka(listOf(oneOutboxEvent, anotherOutboxEvent))

        firstKafkaConsumer.consumeAndAssert(stream = "topic") { record ->
            assertThat(record.key()).isEqualTo(oneOutboxEvent.aggregateId.toString())
            assertThat(record.value()).isEqualTo(oneOutboxEvent.payload)
        }
        secondKafkaConsumer.consumeAndAssert(stream = "topic-2") { record ->
            assertThat(record.key()).isEqualTo(anotherOutboxEvent.aggregateId.toString())
            assertThat(record.value()).isEqualTo(anotherOutboxEvent.payload)
        }
    }
}
