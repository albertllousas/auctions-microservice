package auction.domain.model

sealed interface DomainEvent

data class AuctionCreated(val auction: Auction) : DomainEvent

data class AuctionOpened(val auction: Auction) : DomainEvent

data class BidPlaced(val auction: Auction) : DomainEvent

data class AuctionEnded(val auction: Auction) : DomainEvent
