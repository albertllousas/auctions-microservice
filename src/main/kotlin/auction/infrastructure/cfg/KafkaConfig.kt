package auction.infrastructure.cfg

import auction.infrastructure.out.stream.PublishOutboxMessageToKafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Properties

@Configuration
class KafkaConfig {

    @Bean
    fun kafkaProducer(
        @Value("\${kafka.bootstrap.servers}") bootstrapServers: String
    ): KafkaProducer<String, ByteArray> = KafkaProducer<String, ByteArray>(
        Properties().apply {
            this["key.serializer"] = StringSerializer::class.java.name
            this["value.serializer"] = ByteArraySerializer::class.java.name
            this["bootstrap.servers"] = bootstrapServers
        }
    )

    @Bean
    fun publishOutboxMessageToKafka(kafkaProducer: KafkaProducer<String, ByteArray>) =
        PublishOutboxMessageToKafka(kafkaProducer)
}
