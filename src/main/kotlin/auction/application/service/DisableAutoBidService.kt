package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import auction.domain.model.Auction
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidDisabled
import auction.domain.model.AutoBidId
import auction.domain.model.DisableAutoBidUseCaseError
import auction.domain.model.FindAuction
import auction.domain.model.FindAutoBid
import auction.domain.model.SaveAutoBid
import java.util.UUID

class DisableAutoBidService(
    private val findAutoBid: FindAutoBid,
    private val findAuction: FindAuction,
    private val saveAutoBid: SaveAutoBid,
    private val executeUseCase: ExecuteUseCase,
    private val disable: (AutoBid, Auction) -> Either<DisableAutoBidUseCaseError, AutoBidDisabled> =
        AutoBid.Companion::disable
) {

    operator fun invoke(request: DisableAutoBidCommand): Either<DisableAutoBidUseCaseError, Unit> = executeUseCase {
        findAutoBid(AutoBidId(request.autoBidId))
            .flatMap { autoBid -> findAuction(autoBid.auctionId).map { Pair(autoBid, it) } }
            .flatMap { (autoBid, auction) -> disable(autoBid, auction) }
            .tap { saveAutoBid(it.autoBid) }
    }
}

data class DisableAutoBidCommand(val autoBidId: UUID)
