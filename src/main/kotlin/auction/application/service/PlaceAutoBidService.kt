package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import auction.domain.model.Auction
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidId
import auction.domain.model.AutoBidPlaced
import auction.domain.model.FindAuction
import auction.domain.model.FindAutoBid
import auction.domain.model.PlaceAutoBidError
import auction.domain.model.PlaceAutoBidUseCaseError
import auction.domain.model.SaveAuction
import java.time.Clock
import java.util.UUID

class PlaceAutoBidService(
    private val findAuction: FindAuction,
    private val findAutoBid: FindAutoBid,
    private val saveAuction: SaveAuction,
    private val executeUseCase: ExecuteUseCase,
    private val clock: Clock,
    private val placeAutoBid: (AutoBid, Auction, Clock) -> Either<PlaceAutoBidError, AutoBidPlaced> =
        AutoBid.Companion::placeAutoBid
) {

    operator fun invoke(request: PlaceAutoBidCommand): Either<PlaceAutoBidUseCaseError, Unit> = executeUseCase {
        findAutoBid(AutoBidId(request.autoBidId))
            .flatMap { autoBid -> findAuction(autoBid.auctionId).map { Pair(it, autoBid) } }
            .flatMap { (auction, autoBid) -> placeAutoBid(autoBid, auction, clock) }
            .tap { saveAuction(it.auction) }
    }
}

data class PlaceAutoBidCommand(val autoBidId: UUID)
