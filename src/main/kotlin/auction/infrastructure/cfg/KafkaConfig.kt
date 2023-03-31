package auction.infrastructure.cfg

import auction.infrastructure.out.messaging.PublishOutboxMessageToKafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.ConsumerRecordRecoverer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries


@Configuration
@EnableKafka
class KafkaConfig {

    @Bean
    fun kafkaProducer(
        @Value("\${kafka.bootstrap.servers}") bootstrapServers: String
    ): KafkaProducer<String, ByteArray> = KafkaProducer<String, ByteArray>(configMap(bootstrapServers))

    @Bean
    fun producerFactory(
        @Value("\${kafka.bootstrap.servers}") bootstrapServers: String
    ): ProducerFactory<String, ByteArray> = DefaultKafkaProducerFactory(configMap(bootstrapServers))

    @Bean
    fun nativeProducerFactory(
        @Value("\${kafka.bootstrap.servers}") bootstrapServers: String
    ): ProducerFactory<Object, Object> = DefaultKafkaProducerFactory(configMap(bootstrapServers))

    private fun configMap(bootstrapServers: String) = mapOf(
        BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
        KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
        VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name
    )

    @Bean
    fun publishOutboxMessageToKafka(kafkaProducer: KafkaProducer<String, ByteArray>) =
        PublishOutboxMessageToKafka(kafkaProducer)

    @Bean
    fun kafkaContainerFactory(
        errorHandler: CommonErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray> =
        ConcurrentKafkaListenerContainerFactory<String, ByteArray>().apply {
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
            setCommonErrorHandler(errorHandler)
            afterPropertiesSet()
        }


    @Bean
    fun defaultErrorHandler(
        consumerRecordRecoverer: ConsumerRecordRecoverer
    ): CommonErrorHandler {
        val exponentialBackOff = ExponentialBackOffWithMaxRetries(15).apply {
            initialInterval = 100
            multiplier = 2.toDouble()
            maxInterval = 10_000L
        }
        val errorHandler = DefaultErrorHandler(consumerRecordRecoverer, exponentialBackOff)
//        errorHandler.addNotRetryableExceptions(JsonProcessingException::class.java)
        return errorHandler
    }

    @Bean
    fun deadLetterPublishingRecoverer(producerFactory: ProducerFactory<String, ByteArray>): ConsumerRecordRecoverer =
        DeadLetterPublishingRecoverer(KafkaTemplate(producerFactory))
            .let {
                ConsumerRecordRecoverer { record, exception ->
                    // missing metrics and logs
                    it.accept(record, exception)
                }
            }
}
