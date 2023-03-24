package auction.application.service

import arrow.core.left
import arrow.core.right
import auction.domain.model.AuctionCreated
import auction.domain.model.FindItem
import auction.domain.model.FindUser
import auction.domain.model.ItemDoesNotBelongToTheSeller
import auction.domain.model.ItemId
import auction.domain.model.ItemNotFound
import auction.domain.model.SaveAuction
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
import java.time.Duration.*
import java.util.UUID

class CreateAuctionServiceTest {

    private val findItem = mockk<FindItem>()

    private val findUser = mockk<FindUser>()

    private val saveAuction = mockk<SaveAuction>(relaxed = true)

    private val createAuction = mockk<CreateAuction>()

    private val clock = Clock.systemUTC()

    private val uuid = UUID.randomUUID()

    private val createAuctionService = CreateAuctionService(
        findUser,
        findItem,
        saveAuction,
        clock,
        FakeExecuteUseCase,
        { uuid },
        ofMinutes(20),
        ofMinutes(20),
        createAuction
    )

    @Test
    fun `should create an auction`() {
        val command = Builders.buildCreateAuctionCommand()
        val user = Builders.buildUser()
        val item = Builders.buildItem()
        val auction = Builders.buildAuction()
        every { findUser(UserId(command.sellerId)) } returns user.right()
        every { findItem(ItemId(command.itemId)) } returns item.right()
        every {
            createAuction(uuid, user, item, command.openingBid, command.minimalBid, command.openingDate, ofMinutes(20), ofMinutes(20), clock)
        } returns AuctionCreated(auction).right()

        val result = createAuctionService.invoke(command)

        result shouldBe Unit.right()
        verify { saveAuction(auction) }
    }

    @Test
    fun `should fail creating an auction when user does not exists`() {
        val command = Builders.buildCreateAuctionCommand()
        every { findUser(any()) } returns UserNotFound.left()
        every { findItem(any()) } returns Builders.buildItem().right()

        val result = createAuctionService.invoke(command)

        result shouldBe UserNotFound.left()
    }

    @Test
    fun `should fail creating an auction when item does not exists`() {
        val command = Builders.buildCreateAuctionCommand()
        every { findUser(any()) } returns Builders.buildUser().right()
        every { findItem(any()) } returns ItemNotFound.left()

        val result = createAuctionService.invoke(command)

        result shouldBe ItemNotFound.left()
    }

    @Test
    fun `should fail creating an auction when there is a domain error`() {
        val command = Builders.buildCreateAuctionCommand()
        every { findUser(any()) } returns Builders.buildUser().right()
        every { findItem(any()) } returns Builders.buildItem().right()
        every {
            createAuction(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns ItemDoesNotBelongToTheSeller.left()

        val result = createAuctionService.invoke(command)

        result shouldBe ItemDoesNotBelongToTheSeller.left()
    }
}
