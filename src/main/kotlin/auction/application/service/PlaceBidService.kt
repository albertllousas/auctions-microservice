package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.zip
import auction.domain.model.Auction
import auction.domain.model.AuctionId
import auction.domain.model.BidPlaced
import auction.domain.model.FindAuction
import auction.domain.model.FindUser
import auction.domain.model.PlaceBidUseCaseError
import auction.domain.model.SaveAuction
import auction.domain.model.User
import auction.domain.model.UserId
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID

typealias PlaceBid = (
    auction: Auction,
    amount: BigDecimal,
    bidderId: User,
    prevBidNumber: Long,
    clock: Clock
) -> Either<PlaceBidUseCaseError, BidPlaced>

class PlaceBidService(
    private val findAuction: FindAuction,
    private val findUser: FindUser,
    private val saveAuction: SaveAuction,
    private val executeUseCase: ExecuteUseCase,
    private val clock: Clock,
    private val placeBid: PlaceBid = Auction.Companion::placeBid
) {

    operator fun invoke(request: PlaceBidCommand) : Either<PlaceBidUseCaseError, Unit> = executeUseCase {
        findAuction(AuctionId(request.auctionId))
            .zip(findUser(UserId(request.bidderId)))
            .flatMap { (auction, bidder) ->
                placeBid(auction, request.amount, bidder, request.currentBidCounter, clock)
            }
            .tap { saveAuction(it.auction) }
    }
}

data class PlaceBidCommand(val auctionId: UUID, val amount: BigDecimal, val currentBidCounter: Long, val bidderId: UUID)
