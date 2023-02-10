package auction.infrastructure.out.stream

import auction.infrastructure.out.db.OutboxMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class PublishOutboxMessageToKafka(private val producer: KafkaProducer<String, ByteArray>) {

    operator fun invoke(batch: List<OutboxMessage>) =
        batch.map(::toKafkaRecord)
            .forEach(producer::send)
            .also { if (batch.isNotEmpty()) producer.flush() }

    private fun toKafkaRecord(outboxMessage: OutboxMessage) =
        ProducerRecord(outboxMessage.stream, outboxMessage.aggregateId.toString(), outboxMessage.payload)

}
