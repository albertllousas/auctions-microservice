package auction.fixtures

import auction.application.service.CreateAuctionCommand
import auction.application.service.PlaceBidCommand
import auction.domain.model.AggregateVersion
import auction.domain.model.Amount
import auction.domain.model.Auction
import auction.domain.model.AuctionId
import auction.domain.model.AuctionStatus
import auction.domain.model.Bid
import auction.domain.model.Item
import auction.domain.model.ItemId
import auction.domain.model.ItemStatus
import auction.domain.model.User
import auction.domain.model.UserId
import auction.infrastructure.out.db.OutboxMessage
import com.github.javafaker.Faker
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

private val faker = Faker()

object Builders {

    fun buildOutboxMessage(
        id: UUID = UUID.randomUUID(),
        aggregateId: UUID = UUID.randomUUID(),
        stream: String = faker.beer().name(),
        payload: ByteArray = faker.dragonBall().character().toByteArray()
    ) = OutboxMessage(id, aggregateId, stream, payload)

    fun buildPlaceBidCommand(
        auctionId: UUID = UUID.randomUUID(),
        amount: BigDecimal = faker.number().randomNumber().toBigDecimal(),
        previousBidNumber: Long = faker.number().randomNumber(),
        bidderId: UUID = UUID.randomUUID()
    ) = PlaceBidCommand(
        auctionId = auctionId,
        amount = amount,
        currentBidCounter = previousBidNumber,
        bidderId = bidderId
    )

    fun buildCreateAuctionCommand(
        sellerId: UUID = UUID.randomUUID(),
        openingBid: BigDecimal = 100.toBigDecimal(),
        itemId: UUID = UUID.randomUUID(),
        minimalBid: BigDecimal = 10.toBigDecimal(),
        openingDate: LocalDateTime = LocalDateTime.now().plusDays(1)
    ) = CreateAuctionCommand(sellerId, itemId, openingBid, minimalBid, openingDate)

    fun buildAuction(
        id: AuctionId = AuctionId(UUID.randomUUID()),
        userId: UserId = UserId(UUID.randomUUID()),
        itemId: ItemId = ItemId(UUID.randomUUID()),
        openingAmount: Amount = Amount(BigDecimal.TEN),
        minimalAmount: Amount = Amount(BigDecimal.ONE),
        openingAt: LocalDateTime = LocalDateTime.now(),
        createdAt: LocalDateTime = LocalDateTime.now().plusDays(8),
        status: AuctionStatus = AuctionStatus.OnPreview,
        version: AggregateVersion = AggregateVersion.new(),
        currentBidBNumber: Long = 0L,
        currentBid: Bid? = null,
        endAt: LocalDateTime = LocalDateTime.now(),
        sellToHighestBidPeriod: Duration = Duration.ofMinutes(20)

    ) = Auction.reconstitute(
        id,
        userId,
        itemId,
        openingAmount,
        minimalAmount,
        openingAt,
        createdAt,
        status,
        version,
        currentBidBNumber,
        currentBid,
        endAt,
        sellToHighestBidPeriod
    )

    fun buildUser(id: UUID = UUID.randomUUID()) = User(UserId(id))

    fun buildItem(
        id: UUID = UUID.randomUUID(),
        status: ItemStatus = ItemStatus.Available,
        sellerId: UUID = UUID.randomUUID()
    ) = Item(ItemId(id), status, UserId(sellerId))
}