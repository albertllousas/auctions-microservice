package auction.domain.model

import arrow.core.Either
import kotlin.reflect.KClass

typealias FindAuction = (auctionId: AuctionId) -> Either<AuctionNotFound, Auction>

typealias SaveAuction = (auction: Auction) -> Unit

typealias FindAutoBid = (autoBidId: AutoBidId) -> Either<AutoBidNotFound, AutoBid>

typealias FindAutoBidsByAuction = (auctionId: AuctionId) -> List<AutoBid>

typealias SaveAutoBid = (autoBid: AutoBid) -> Unit

typealias FindUser = (UserId) -> Either<UserNotFound, User>

typealias FindItem = (ItemId) -> Either<ItemNotFound, Item>

typealias PublishEvent = (DomainEvent) -> Unit

typealias HandleEvent = (DomainEvent) -> Unit

typealias ReportError = (error: DomainError, originator: KClass<*>) -> Unit

typealias ReportCrash = (crash: Exception, originator: KClass<*>) -> Unit

interface WithinTransaction {
    operator fun <T> invoke(transactionalBlock: () -> T): T
}
