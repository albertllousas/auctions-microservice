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
import auction.infrastructure.out.db.AuctionTask.EndAuctionTask
import auction.infrastructure.out.db.AuctionTask.OpenAuctionTask
import auction.infrastructure.out.db.AuctionTasksMongoRepository


class ScheduleAuctionTask(private val auctionTaskRepository: AuctionTasksMongoRepository) : HandleEvent {

    override fun invoke(event: DomainEvent) {
        when (event) {
            is AuctionCreated -> OpenAuctionTask(event.auction.openingAt, event.auction.id)
            is AuctionOpened -> EndAuctionTask(event.auction.endAt, event.auction.id)
            is BidPlaced -> EndAuctionTask(event.auction.endAt, event.auction.id)
            is AutoBidPlaced -> EndAuctionTask(event.auction.endAt, event.auction.id)
            is AuctionEnded -> null
            is AutoBidCreated -> null
            is AutoBidDisabled -> null
        }?.also(auctionTaskRepository::save)
    }
}