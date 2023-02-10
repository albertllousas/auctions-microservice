package auction.infrastructure.out.events

import auction.domain.model.DomainEvent
import auction.domain.model.HandleEvent
import auction.domain.model.PublishEvent

class NotifyInMemoryEventHandlers(private val handlers: List<HandleEvent>) : PublishEvent {

    override operator fun invoke(domainEvent: DomainEvent) = handlers.forEach { handle -> handle(domainEvent) }
}
