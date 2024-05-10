package auction.domain.model

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.zip
import auction.domain.model.AuctionStatus.Expired
import auction.domain.model.AuctionStatus.ItemSold
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID

data class AutoBid(
    val id: AutoBidId,
    val auctionId: AuctionId,
    val userId: UserId,
    val amount: Amount,
    val limit: Amount,
    val enabled: Boolean
) {
    companion object {
        fun create(
            id: UUID,
            user: User,
            auction: Auction,
            amount: BigDecimal,
            limit: BigDecimal
        ): Either<CreateAutoBidError, AutoBidCreated> = when {
            auction.status is Expired || auction.status is ItemSold -> AuctionHasFinished.left()
            isLimitReached(auction, amount, limit) -> AutoBidLimitReached.left()
            else -> Amount.create(amount).zip(Amount.create(limit))
        }
            .map { AutoBid(AutoBidId(id), auction.id, user.id, it.first, it.second, true) }
            .map { AutoBidCreated(auction, it) }

        private fun isLimitReached(auction: Auction, amount: BigDecimal, limit: BigDecimal) =
            auction.currentBid != null && auction.currentBid.amount.value + amount >= limit

        fun placeAutoBid(autoBid: AutoBid, auction: Auction, clock: Clock): Either<PlaceAutoBidError, AutoBidPlaced> =
            when {
                auction.currentBid == null -> NoBidToAutoBid.left()
                isLimitReached(auction, autoBid.amount.value, autoBid.limit.value) -> AutoBidLimitReached.left()
                auction.id != autoBid.auctionId -> AuctionNotMatching.left()
                !autoBid.enabled -> AutoBidIsDisabled.left()
                else -> autoBid.right()
            }
                .flatMap { Auction.placeBid(auction, it.amount.value, it.userId, auction.bidsCounter, clock) }
                .map { AutoBidPlaced(it.auction, autoBid) }

        fun disable(autoBid: AutoBid, auction: Auction): Either<DisableAutoBidUseCaseError, AutoBidDisabled> =
            when {
                !autoBid.enabled -> AutoBidAlreadyDisabled.left()
                autoBid.auctionId != auction.id -> AuctionNotMatching.left()
                else -> autoBid.copy(enabled = false).right()
            }.map { AutoBidDisabled(auction, it) }
    }
}

data class AutoBidId(val value: UUID)
