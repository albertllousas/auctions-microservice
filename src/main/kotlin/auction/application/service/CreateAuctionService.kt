package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.zip
import auction.domain.model.Auction
import auction.domain.model.AuctionCreated
import auction.domain.model.CreateAuctionError
import auction.domain.model.CreateAuctionUseCaseError
import auction.domain.model.FindItem
import auction.domain.model.FindUser
import auction.domain.model.Item
import auction.domain.model.ItemId
import auction.domain.model.SaveAuction
import auction.domain.model.User
import auction.domain.model.UserId
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

typealias CreateAuction = (
    id: UUID, user: User,
    item: Item,
    openingBid: BigDecimal,
    minimalBid: BigDecimal,
    openingDate: LocalDateTime,
    auctionExpirationPeriod: Duration,
    sellToHighestBidPeriod: Duration,
    clock: Clock
) -> Either<CreateAuctionError, AuctionCreated>

class CreateAuctionService(
    private val findUser: FindUser,
    private val findItem: FindItem,
    private val saveAuction: SaveAuction,
    private val clock: Clock,
    private val executeUseCase: ExecuteUseCase,
    private val newId: () -> UUID = { UUID.randomUUID() },
    private val auctionExpirationPeriod: Duration,
    private val sellToHighestBidPeriod: Duration,
    private val create: CreateAuction = Auction.Companion::create,
) {

    operator fun invoke(request: CreateAuctionCommand): Either<CreateAuctionUseCaseError, Unit> = executeUseCase {
        findUser(UserId(request.sellerId))
            .zip(findItem(ItemId(request.itemId)))
            .flatMap { (user, item) ->
                create(
                    newId(),
                    user,
                    item,
                    request.openingBid,
                    request.minimalBid,
                    request.openingDate,
                    auctionExpirationPeriod,
                    sellToHighestBidPeriod,
                    clock
                )
            }
            .tap { saveAuction(it.auction) }
    }
}

data class CreateAuctionCommand(
    val sellerId: UUID,
    val itemId: UUID,
    val openingBid: BigDecimal,
    val minimalBid: BigDecimal,
    val openingDate: LocalDateTime
)
