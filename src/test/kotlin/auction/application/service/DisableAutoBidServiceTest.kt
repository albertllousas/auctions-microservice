package auction.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auction.domain.model.Auction
import auction.domain.model.AuctionNotFound
import auction.domain.model.AutoBid
import auction.domain.model.AutoBidAlreadyDisabled
import auction.domain.model.AutoBidDisabled
import auction.domain.model.AutoBidNotFound
import auction.domain.model.DisableAutoBidUseCaseError
import auction.domain.model.FindAuction
import auction.domain.model.FindAutoBid
import auction.domain.model.SaveAutoBid
import auction.fixtures.Builders
import auction.fixtures.FakeExecuteUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class DisableAutoBidServiceTest {

    private val findAutoBid = mockk<FindAutoBid>()

    private val findAuction = mockk<FindAuction>()

    private val saveAutoBid = mockk<SaveAutoBid>(relaxed = true)

    private val disable = mockk<(AutoBid, Auction) -> Either<DisableAutoBidUseCaseError, AutoBidDisabled>>()


    private val disableAutoBid = DisableAutoBidService(
        findAutoBid, findAuction, saveAutoBid, FakeExecuteUseCase, disable
    )

    @Test
    fun `should disable an auto-bid`() {
        val auction = Builders.buildAuction()
        val autoBid = Builders.buildAutoBid( auctionId = auction.id)
        every { findAutoBid(autoBid.id) } returns autoBid.right()
        every { findAuction(auction.id) } returns auction.right()
        every { disable(autoBid, auction) } returns AutoBidDisabled(auction, autoBid).right()

        val result = disableAutoBid.invoke(DisableAutoBidCommand(autoBid.id.value))

        result shouldBe Unit.right()
        verify { saveAutoBid(autoBid) }
    }

    @Test
    fun `should fail disabling an auto-bid when it does not exists`() {
        val autoBid = Builders.buildAutoBid()
        every { findAutoBid(autoBid.id) } returns AutoBidNotFound.left()

        val result = disableAutoBid.invoke(DisableAutoBidCommand(autoBid.id.value))

        result shouldBe AutoBidNotFound.left()
    }

    @Test
    fun `should fail disabling an auto-bid when auction does not exists`() {
        val auction = Builders.buildAuction()
        val autoBid = Builders.buildAutoBid( auctionId = auction.id)
        every { findAutoBid(autoBid.id) } returns autoBid.right()
        every { findAuction(auction.id) } returns AuctionNotFound.left()

        val result = disableAutoBid.invoke(DisableAutoBidCommand(autoBid.id.value))

        result shouldBe AuctionNotFound.left()
    }

    @Test
    fun `should fail bidding when there is a domain error`() {
        val auction = Builders.buildAuction()
        val autoBid = Builders.buildAutoBid( auctionId = auction.id)
        every { findAutoBid(autoBid.id) } returns autoBid.right()
        every { findAuction(auction.id) } returns auction.right()
        every { disable(autoBid, auction) } returns AutoBidAlreadyDisabled.left()

        val result = disableAutoBid.invoke(DisableAutoBidCommand(autoBid.id.value))

        result shouldBe AutoBidAlreadyDisabled.left()
    }
}