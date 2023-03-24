package auction.fixtures

import auction.application.service.CreateAuctionCommand
import auction.application.service.CreateAutoBidCommand
import auction.application.service.PlaceBidCommand
import auction.domain.model.AggregateVersion
import auction.domain.model.Amount
import auction.domain.model.Auction
import auction.domain.model.AuctionId
import auction.domain.model.AuctionStatus
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidId
import auction.domain.model.Bid
import auction.domain.model.Item
import auction.domain.model.ItemId
import auction.domain.model.ItemStatus
import auction.domain.model.User
import auction.domain.model.UserId
import auction.infrastructure.out.db.MessagingSystem
import auction.infrastructure.out.db.OutboxMessage
import auction.infrastructure.out.events.AuctionIntegrationDto
import auction.infrastructure.out.events.AutoBidIntegrationDto
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
        messagingSystem: MessagingSystem = MessagingSystem.KAFKA,
        target: String = faker.beer().name(),
        payload: ByteArray = faker.dragonBall().character().toByteArray()
    ) = OutboxMessage(id, aggregateId, messagingSystem, target, payload)

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

    fun buildCreateAutoBidCommand(
        auctionId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        amount: BigDecimal = 10.toBigDecimal(),
        limit: BigDecimal = 100.toBigDecimal()
    ) = CreateAutoBidCommand(auctionId, userId, amount, limit)

    fun buildAutoBid(
        id: AutoBidId = AutoBidId(UUID.randomUUID()),
        auctionId: AuctionId = AuctionId(UUID.randomUUID()),
        userId: UserId = UserId(UUID.randomUUID()),
        amount: Amount = Amount(10.toBigDecimal()),
        limit: Amount = Amount(100.toBigDecimal()),
        enabled: Boolean = true
    ) = AutoBid(id = id, auctionId = auctionId, userId = userId, amount = amount, limit = limit, enabled = enabled)

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
        sellToHighestBidWaitingPeriod: Duration = Duration.ofMinutes(20)
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
        sellToHighestBidWaitingPeriod
    )

    fun buildUser(id: UUID = UUID.randomUUID()) = User(UserId(id))

    fun buildItem(
        id: UUID = UUID.randomUUID(),
        status: ItemStatus = ItemStatus.Available,
        sellerId: UUID = UUID.randomUUID()
    ) = Item(ItemId(id), status, UserId(sellerId))


    fun buildBidIntegrationDto(
        bidderId: UUID = UUID.randomUUID(),
        amount: BigDecimal = BigDecimal.TEN,
        ts: LocalDateTime = LocalDateTime.now()
    ) = AuctionIntegrationDto.BidDto(bidderId = bidderId, amount = amount, ts = ts)

    fun buildAutoBidIntegrationDto(
        autoBidId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        auctionId: UUID = UUID.randomUUID(),
        amount: BigDecimal = BigDecimal.ONE,
        limit: BigDecimal = BigDecimal.TEN
    ) = AutoBidIntegrationDto(
        autoBidId = autoBidId,
        userId = userId,
        auctionId = auctionId,
        amount = amount,
        limit = limit
    )

    fun buildAuctionIntegrationDto(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        itemId: UUID = UUID.randomUUID(),
        openingBid: BigDecimal = BigDecimal.TEN,
        minimalBid: BigDecimal = BigDecimal.ONE,
        openingAt: LocalDateTime = LocalDateTime.now(),
        createdAt: LocalDateTime = LocalDateTime.now(),
        currentBid: AuctionIntegrationDto.BidDto? = null,
        status: AuctionIntegrationDto.AuctionStatus = AuctionIntegrationDto.AuctionStatus.ON_PREVIEW
    ) = AuctionIntegrationDto(
        id = id,
        userId = userId,
        itemId = itemId,
        openingBid = openingBid,
        minimalBid = minimalBid,
        openingAt = openingAt,
        createdAt = createdAt,
        currentBid = currentBid,
        status = status
    )
}