package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import auction.domain.model.Auction
import auction.domain.model.AuctionEnded
import auction.domain.model.AuctionId
import auction.domain.model.EndAuctionUseCaseError
import auction.domain.model.FindAuction
import auction.domain.model.SaveAuction
import java.time.Clock
import java.util.UUID

class EndAuctionService(
    private val findAuction: FindAuction,
    private val saveAuction: SaveAuction,
    private val executeUseCase: ExecuteUseCase,
    private val clock: Clock,
    private val end: (Auction, Clock) -> Either<EndAuctionUseCaseError, AuctionEnded> = Auction.Companion::end
) {

    operator fun invoke(request: EndAuctionCommand): Either<EndAuctionUseCaseError, Unit> = executeUseCase {
        findAuction(AuctionId(request.auctionId))
            .flatMap { end(it, clock) }
            .tap { saveAuction(it.auction) }
    }
}

data class EndAuctionCommand(val auctionId: UUID)
