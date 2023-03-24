package auction

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin.NewTopic
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class Kafka {
    val container = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"))
        .also { it.start() }
        .also {
            System.setProperty("spring.kafka.producer.bootstrap-servers", it.bootstrapServers)
            System.setProperty("spring.kafka.consumer.bootstrap-servers", it.bootstrapServers)
        }
        .also { createTopics(it) }

    private fun createTopics(kafka: KafkaContainer) =
        listOf(NewTopic("public.auctions", 1, 1), NewTopic("public.autobids", 1, 1))
            .let {
                println(">>>>>>${kafka.bootstrapServers}")
                AdminClient
                    .create(mapOf(Pair(BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)))
                    .createTopics(it)
            }
}
