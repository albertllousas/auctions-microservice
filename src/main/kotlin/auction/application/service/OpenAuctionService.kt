package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import auction.domain.model.Auction
import auction.domain.model.AuctionId
import auction.domain.model.AuctionOpened
import auction.domain.model.FindAuction
import auction.domain.model.SaveAuction
import auction.domain.model.OpenAuctionUseCaseError
import java.time.Clock
import java.util.UUID

class OpenAuctionService(
    private val findAuction: FindAuction,
    private val saveAuction: SaveAuction,
    private val executeUseCase: ExecuteUseCase,
    private val clock: Clock,
    private val open: (Auction, Clock) -> Either<OpenAuctionUseCaseError, AuctionOpened> = Auction.Companion::open
) {

    operator fun invoke(request: OpenAuctionCommand) = executeUseCase {
        findAuction(AuctionId(request.auctionId))
            .flatMap { open(it, clock) }
            .tap { saveAuction(it.auction) }
    }
}

data class OpenAuctionCommand(val auctionId: UUID)
