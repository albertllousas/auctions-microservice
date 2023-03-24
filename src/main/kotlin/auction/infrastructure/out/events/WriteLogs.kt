package auction.infrastructure.out.events

import auction.domain.model.AuctionCreated
import auction.domain.model.AuctionEnded
import auction.domain.model.AuctionOpened
import auction.domain.model.AutoBidCreated
import auction.domain.model.AutoBidDisabled
import auction.domain.model.AutoBidPlaced
import auction.domain.model.BidPlaced
import auction.domain.model.DomainEvent
import auction.domain.model.HandleEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

class WriteLogs(
    private val logger: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass()),
) : HandleEvent {

    override fun invoke(domainEvent: DomainEvent) {
        logger.info(domainEvent.logMessage())
    }

    private fun DomainEvent.logMessage(): String {
        val common = "domain-event: '${this::class.simpleName}'"
        val detail = when (this) {
            is AuctionCreated -> "opening-date: '${this.auction.openingAt}', id: '${this.auction.id.value}'"
            is AuctionOpened -> "id: '${this.auction.id.value}'"
            is BidPlaced -> "id: '${this.auction.id.value}', bidder-id:'${auction.currentBid!!.bidderId.value}'"
            is AuctionEnded -> "id: '${this.auction.id.value}'"
            is AutoBidCreated -> "id: '${this.auction.id.value}', bidder-id: '${this.autoBid.userId.value}'"
            is AutoBidDisabled -> "id: '${this.auction.id.value}', bidder-id: '${this.autoBid.userId.value}'"
            is AutoBidPlaced -> "id: '${this.auction.id.value}', bidder-id: '${this.autoBid.userId.value}'"
        }
        return (listOf(common) + listOf(detail)).joinToString(", ")
    }
}
