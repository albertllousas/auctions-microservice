package auction.application.service

import arrow.core.left
import arrow.core.right
import auction.domain.model.AuctionId
import auction.domain.model.AuctionNotFound
import auction.domain.model.BidPlaced
import auction.domain.model.FindAuction
import auction.domain.model.FindUser
import auction.domain.model.SaveAuction
import auction.domain.model.TooLowAmount
import auction.domain.model.UserId
import auction.domain.model.UserNotFound
import auction.fixtures.Builders
import auction.fixtures.FakeExecuteUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock

class PlaceBidServiceTest {

    private val findAuction = mockk<FindAuction>()

    private val findUser = mockk<FindUser>()

    private val saveAuction = mockk<SaveAuction>(relaxed = true)

    private val placeBid = mockk<PlaceBid>()

    private val clock = Clock.systemUTC()

    private val placeBidService = PlaceBidService(
        findAuction, findUser, saveAuction, FakeExecuteUseCase, clock, placeBid
    )

    @Test
    fun `should place a bid`() {
        val command = Builders.buildPlaceBidCommand()
        val auction = Builders.buildAuction()
        val bidder = Builders.buildUser()
        every { findAuction(AuctionId(command.auctionId)) } returns auction.right()
        every { findUser(UserId(command.bidderId)) } returns bidder.right()
        every {
            placeBid(auction, command.amount, bidder, command.currentBidCounter, clock)
        } returns BidPlaced(auction).right()

        val result = placeBidService.invoke(command)

        result shouldBe Unit.right()
        verify { saveAuction(auction) }
    }

    @Test
    fun `should fail bidding when auction does not exists`() {
        val command = Builders.buildPlaceBidCommand()
        val bidder = Builders.buildUser()
        every { findAuction(any()) } returns AuctionNotFound.left()
        every { findUser(UserId(command.bidderId)) } returns bidder.right()

        val result = placeBidService.invoke(command)

        result shouldBe AuctionNotFound.left()
    }

    @Test
    fun `should fail bidding when bidder does not exists`() {
        val command = Builders.buildPlaceBidCommand()
        val auction = Builders.buildAuction()
        every { findAuction(any()) } returns auction.right()
        every { findUser(any()) } returns UserNotFound.left()

        val result = placeBidService.invoke(command)

        result shouldBe UserNotFound.left()
    }

    @Test
    fun `should fail bidding when there is a domain error`() {
        val command = Builders.buildPlaceBidCommand()
        val auction = Builders.buildAuction()
        val bidder = Builders.buildUser()
        every { findAuction(any()) } returns auction.right()
        every { findUser(any()) } returns bidder.right()
        every { placeBid(any(), any(), any(), any(), any()) } returns TooLowAmount.left()

        val result = placeBidService.invoke(command)

        result shouldBe TooLowAmount.left()
    }
}