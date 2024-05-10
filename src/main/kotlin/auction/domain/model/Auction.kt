package auction.domain.model

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.zip
import auction.domain.model.AuctionStatus.Expired
import auction.domain.model.AuctionStatus.ItemSold
import auction.domain.model.AuctionStatus.OnPreview
import auction.domain.model.AuctionStatus.Opened
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

data class Auction private constructor(
    val id: AuctionId,
    val userId: UserId,
    val itemId: ItemId,
    val openingAmount: Amount,
    val minimalAmount: Amount,
    val openingAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val status: AuctionStatus,
    val version: AggregateVersion,
    val bidsCounter: Long,
    val currentBid: Bid?,
    val endAt: LocalDateTime,
    val sellToHighestBidPeriod: Duration
) {

    companion object {

        private const val sevenDaysInHours = 24 * 7 // could be parametrized

        fun create(
            newId: UUID,
            user: User,
            item: Item,
            openingBid: BigDecimal,
            minimalBid: BigDecimal,
            openingDate: LocalDateTime,
            auctionExpirationPeriod: Duration,
            sellToHighestBidWaitingPeriod: Duration,
            clock: Clock,
        ): Either<CreateAuctionError, AuctionCreated> =
            checkOwnerShip(user, item)
                .flatMap { check(item) }
                .flatMap { check(openingDate, clock) }
                .flatMap { Amount.create(openingBid) }
                .zip(Amount.create(minimalBid)) { openBid, minBid ->
                    Auction(
                        id = AuctionId(newId),
                        userId = user.id,
                        itemId = item.id,
                        openingAmount = openBid,
                        minimalAmount = minBid,
                        openingAt = openingDate,
                        createdAt = now(clock),
                        status = OnPreview,
                        version = AggregateVersion.new(),
                        bidsCounter = 0,
                        currentBid = null,
                        endAt = openingDate.plus(auctionExpirationPeriod),
                        sellToHighestBidPeriod = sellToHighestBidWaitingPeriod
                    )
                }.map(::AuctionCreated)

        fun open(auction: Auction, clock: Clock): Either<OpenAuctionError, AuctionOpened> =
            when (auction.status) {
                OnPreview -> auction.right()
                Opened -> AuctionAlreadyOpened.left()
                is ItemSold, Expired -> AuctionHasFinished.left()
            }.flatMap { if (now(clock) < auction.openingAt) TooEarlyToOpen.left() else auction.right() }
                .map { it.copy(status = Opened) }
                .map(::AuctionOpened)

        fun placeBid(
            auction: Auction,
            amount: BigDecimal,
            bidderId: UserId,
            currentBidCounter: Long,
            clock: Clock
        ): Either<PlaceBidError, BidPlaced> =
            when {
                auction.bidsCounter != currentBidCounter -> HighestBidHasChanged.left()
                auction.status != Opened -> AuctionIsNotOpened.left()
                else -> Amount.create(amount, auction.minimalAmount.value)
            }.map { if (auction.currentBid != null) auction.currentBid.amount.plus(it) else it }
                .map { Bid(bidderId, it, now(clock)) }
                .map { bid -> auction.copy(currentBid = bid, endAt = bid.ts.plus(auction.sellToHighestBidPeriod)) }
                .map(::BidPlaced)

        fun end(auction: Auction, clock: Clock): Either<EndAuctionUseCaseError, AuctionEnded> =
            when {
                auction.endAt > now(clock) -> TooEarlyToEnd.left()
                auction.status != Opened -> AuctionIsNotOpened.left()
                auction.currentBid == null -> auction.copy(status = Expired).right()
                else -> auction.copy(status = ItemSold(winner = auction.currentBid.bidderId)).right()
            }.map(::AuctionEnded)

        fun increaseVersion(auction: Auction) = auction.copy(version = auction.version.inc())

        fun reconstitute(
            id: AuctionId,
            userId: UserId,
            itemId: ItemId,
            openingAmount: Amount,
            minimalAmount: Amount,
            openingAt: LocalDateTime,
            createdAt: LocalDateTime,
            status: AuctionStatus,
            version: AggregateVersion,
            currentBidNumber: Long,
            currentBid: Bid?,
            endAt: LocalDateTime,
            sellToHighestBidPeriod: Duration
        ) = Auction(
            id, userId, itemId, openingAmount, minimalAmount, openingAt, createdAt, status, version, currentBidNumber, currentBid, endAt, sellToHighestBidPeriod
        )

        private fun check(
            openingDate: LocalDateTime,
            clock: Clock
        ): Either<InvalidOpeningDate, LocalDateTime> {
            return if (Duration.between(now(clock), openingDate).toHours() > sevenDaysInHours) openingDate.right()
            else InvalidOpeningDate.left()
        }

        private fun check(item: Item) =
            if (item.isAvailable()) item.right() else ItemNotAvailable.left()

        private fun checkOwnerShip(user: User, item: Item) =
            if (item.isOwnedBy(user)) user.right() else ItemDoesNotBelongToTheSeller.left()
    }
}

data class Bid(val bidderId: UserId, val amount: Amount, val ts: LocalDateTime)

data class AuctionId(val value: UUID)

data class AggregateVersion(val value: Long) {

    companion object {
        fun new() = AggregateVersion(0)
    }

    fun inc() = AggregateVersion(this.value.inc())
}

sealed class AuctionStatus {
    object OnPreview : AuctionStatus()
    object Opened : AuctionStatus()
    object Expired : AuctionStatus()
    data class ItemSold(val winner: UserId) : AuctionStatus()
}
