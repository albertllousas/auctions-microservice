package auction.infrastructure.out.events

import auction.domain.model.AuctionCreated
import auction.fixtures.Builders
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test

class PublishMetricsShould {

    private val metrics = SimpleMeterRegistry()

    private val publishMetrics = PublishMetrics(metrics)

    @Test
    fun `should handle a domain event publishing a metric`() {
        val event = AuctionCreated(Builders.buildAuction())

        publishMetrics(event)

        metrics.counter("domain.event", listOf(Tag.of("type", "AuctionCreated"))).count() shouldBe 1.0
    }
}
