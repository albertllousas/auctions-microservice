package auction.domain.model

sealed interface DomainEvent

data class AuctionCreated(val auction: Auction) : DomainEvent

data class AuctionOpened(val auction: Auction) : DomainEvent

data class BidPlaced(val auction: Auction) : DomainEvent

data class AuctionEnded(val auction: Auction) : DomainEvent

data class AutoBidCreated(val auction: Auction, val autoBid: AutoBid) : DomainEvent

data class AutoBidPlaced(val auction: Auction, val autoBid: AutoBid) : DomainEvent

data class AutoBidDisabled(val auction: Auction, val autoBid: AutoBid) : DomainEvent
