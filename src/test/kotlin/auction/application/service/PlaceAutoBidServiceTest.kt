package auction.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auction.domain.model.Auction
import auction.domain.model.AuctionNotFound
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidId
import auction.domain.model.AutoBidNotFound
import auction.domain.model.AutoBidPlaced
import auction.domain.model.FindAuction
import auction.domain.model.FindAutoBid
import auction.domain.model.NoBidToAutoBid
import auction.domain.model.PlaceAutoBidError
import auction.domain.model.SaveAuction
import auction.fixtures.Builders
import auction.fixtures.FakeExecuteUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.util.UUID
import org.junit.jupiter.api.Test

class PlaceAutoBidServiceTest {
    private val findAuction = mockk<FindAuction>()

    private val findAutoBid = mockk<FindAutoBid>()

    private val saveAuction = mockk<SaveAuction>(relaxed = true)

    private val placeAutoBid = mockk<(AutoBid, Auction, Clock) -> Either<PlaceAutoBidError, AutoBidPlaced>>()

    private val clock = Clock.systemUTC()

    private val placeAutoBidService = PlaceAutoBidService(
        findAuction, findAutoBid, saveAuction, FakeExecuteUseCase, clock, placeAutoBid
    )

    @Test
    fun `should place an auto bid`() {
        val command = PlaceAutoBidCommand(UUID.randomUUID())
        val autoBid = Builders.buildAutoBid(id = AutoBidId(command.autoBidId))
        val auction = Builders.buildAuction()
        every { findAutoBid(AutoBidId(command.autoBidId)) } returns autoBid.right()
        every { findAuction(autoBid.auctionId) } returns auction.right()
        every { placeAutoBid(autoBid, auction, clock) } returns AutoBidPlaced(auction, autoBid).right()

        val result = placeAutoBidService.invoke(command)

        result shouldBe Unit.right()
        verify { saveAuction(auction) }
    }

    @Test
    fun `should fail bidding when auto-bid does not exists`() {
        val command = PlaceAutoBidCommand(UUID.randomUUID())
        every { findAutoBid(AutoBidId(command.autoBidId)) } returns AutoBidNotFound.left()
        val result = placeAutoBidService.invoke(command)

        result shouldBe AutoBidNotFound.left()
    }

    @Test
    fun `should fail bidding when auction does not exists`() {
        val command = PlaceAutoBidCommand(UUID.randomUUID())
        val autoBid = Builders.buildAutoBid(id = AutoBidId(command.autoBidId))
        every { findAutoBid(AutoBidId(command.autoBidId)) } returns autoBid.right()
        every { findAuction(autoBid.auctionId) } returns AuctionNotFound.left()

        val result = placeAutoBidService.invoke(command)

        result shouldBe AuctionNotFound.left()
    }

    @Test
    fun `should fail bidding when there is a domain error`() {
        val command = PlaceAutoBidCommand(UUID.randomUUID())
        val autoBid = Builders.buildAutoBid(id = AutoBidId(command.autoBidId))
        val auction = Builders.buildAuction()
        every { findAutoBid(AutoBidId(command.autoBidId)) } returns autoBid.right()
        every { findAuction(autoBid.auctionId) } returns auction.right()
        every { placeAutoBid(autoBid, auction, clock) } returns NoBidToAutoBid.left()

        val result = placeAutoBidService.invoke(command)

        result shouldBe NoBidToAutoBid.left()
    }
}