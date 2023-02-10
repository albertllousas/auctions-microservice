package auction.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auction.domain.model.Auction
import auction.domain.model.AuctionAlreadyOpened
import auction.domain.model.AuctionId
import auction.domain.model.AuctionNotFound
import auction.domain.model.AuctionOpened
import auction.domain.model.FindAuction
import auction.domain.model.SaveAuction
import auction.domain.model.OpenAuctionError
import auction.fixtures.Builders
import auction.fixtures.FakeExecuteUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.util.UUID

class OpenAuctionServiceTest {

    private val findAuction = mockk<FindAuction>()

    private val saveAuction = mockk<SaveAuction>(relaxed = true)

    private val start = mockk<(Auction, Clock) -> Either<OpenAuctionError, AuctionOpened>>()

    private val clock = Clock.systemUTC()

    private val openAuctionService = OpenAuctionService(
        findAuction, saveAuction, FakeExecuteUseCase, clock, start
    )

    @Test
    fun `should open an auction`() {
        val command = OpenAuctionCommand(UUID.randomUUID())
        val auction = Builders.buildAuction()
        every { findAuction(AuctionId(command.auctionId)) } returns auction.right()
        every { start(auction, clock) } returns AuctionOpened(auction).right()

        val result = openAuctionService.invoke(command)

        result shouldBe Unit.right()
        verify { saveAuction(auction) }
    }

    @Test
    fun `should fail opening an auction when auction does not exists`() {
        val command = OpenAuctionCommand(UUID.randomUUID())
        every { findAuction(AuctionId(command.auctionId)) } returns AuctionNotFound.left()

        val result = openAuctionService.invoke(command)

        result shouldBe AuctionNotFound.left()
    }

    @Test
    fun `should fail opening an auction when there is a domain error`() {
        val command = OpenAuctionCommand(UUID.randomUUID())
        val auction = Builders.buildAuction()
        every { findAuction(AuctionId(command.auctionId)) } returns auction.right()
        every { start(auction, clock) } returns AuctionAlreadyOpened.left()

        val result = openAuctionService.invoke(command)

        result shouldBe AuctionAlreadyOpened.left()
    }
}
