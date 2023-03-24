package auction.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.zip
import auction.domain.model.Auction
import auction.domain.model.AuctionId
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidCreated
import auction.domain.model.CreateAutoBidError
import auction.domain.model.CreateAutoBidUseCaseError
import auction.domain.model.FindAuction
import auction.domain.model.FindUser
import auction.domain.model.SaveAutoBid
import auction.domain.model.User
import auction.domain.model.UserId
import java.math.BigDecimal
import java.util.UUID

typealias CreateAutoBid = (
    id: UUID,
    user: User,
    auction: Auction,
    bid: BigDecimal,
    limit: BigDecimal
) -> Either<CreateAutoBidError, AutoBidCreated>

class CreateAutoBidService(
    private val findAuction: FindAuction,
    private val findUser: FindUser,
    private val saveAutoBid: SaveAutoBid,
    private val executeUseCase: ExecuteUseCase,
    private val newId: () -> UUID = { UUID.randomUUID() },
    private val create: CreateAutoBid = AutoBid.Companion::create
) {

    operator fun invoke(request: CreateAutoBidCommand): Either<CreateAutoBidUseCaseError, Unit> = executeUseCase {
        findUser(UserId(request.userId)).zip(findAuction(AuctionId(request.auctionId)))
            .flatMap { (user, auction) -> create(newId(), user, auction, request.bid, request.limit) }
            .tap { saveAutoBid(it.autoBid) }
    }
}

data class CreateAutoBidCommand(val auctionId: UUID, val userId: UUID, val bid: BigDecimal, val limit: BigDecimal)
