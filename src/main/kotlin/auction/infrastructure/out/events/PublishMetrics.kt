package auction.infrastructure.out.events

import auction.domain.model.DomainEvent
import auction.domain.model.HandleEvent
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

class PublishMetrics(private val metrics: MeterRegistry) : HandleEvent {

    override operator fun invoke(domainEvent: DomainEvent) {
        val tags = listOf(Tag.of("type", domainEvent::class.simpleName!!))
        metrics.counter("domain.event", tags).increment()
    }
}
