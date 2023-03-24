package auction.application.service

import arrow.core.left
import arrow.core.right
import auction.domain.model.AuctionId
import auction.domain.model.AuctionNotFound
import auction.domain.model.AutoBidAlreadyExists
import auction.domain.model.AutoBidCreated
import auction.domain.model.FindAuction
import auction.domain.model.FindUser
import auction.domain.model.ItemDoesNotBelongToTheSeller
import auction.domain.model.SaveAutoBid
import auction.domain.model.UserId
import auction.domain.model.UserNotFound
import auction.fixtures.Builders
import auction.fixtures.FakeExecuteUseCase
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateAutoBidServiceTest {

    private val findAuction = mockk<FindAuction>()

    private val findUser = mockk<FindUser>()

    private val saveAutoBid = mockk<SaveAutoBid>(relaxed = true)

    private val createAutoBid = mockk<CreateAutoBid>()

    private val uuid = UUID.randomUUID()

    private val createAutoBidService = CreateAutoBidService(
        findAuction = findAuction,
        findUser = findUser,
        saveAutoBid = saveAutoBid,
        executeUseCase = FakeExecuteUseCase,
        newId = { uuid },
        create = createAutoBid
    )

    @Test
    fun `should create an auto bid for an auction`() {
        val command = Builders.buildCreateAutoBidCommand()
        val user = Builders.buildUser()
        val auction = Builders.buildAuction()
        val autoBid = Builders.buildAutoBid()
        every { findAuction(AuctionId(command.auctionId)) } returns auction.right()
        every { findUser(UserId(command.userId)) } returns user.right()
        every {
            createAutoBid(uuid, user, auction, command.bid, command.limit)
        } returns AutoBidCreated(auction, autoBid).right()
        every { saveAutoBid(autoBid) } returns Unit

        val result = createAutoBidService.invoke(command)

        result shouldBe Unit.right()
    }

    @Test
    fun `should fail creating an auto bid when user does not exists`() {
        val command = Builders.buildCreateAutoBidCommand()
        val auction = Builders.buildAuction()
        every { findAuction(any()) } returns auction.right()
        every { findUser(any()) } returns UserNotFound.left()

        val result = createAutoBidService.invoke(command)

        result shouldBe UserNotFound.left()
    }

    @Test
    fun `should fail creating an auto bid when auction does not exists`() {
        val command = Builders.buildCreateAutoBidCommand()
        val user = Builders.buildUser()
        every { findAuction(any()) } returns AuctionNotFound.left()
        every { findUser(any()) } returns user.right()

        val result = createAutoBidService.invoke(command)

        result shouldBe AuctionNotFound.left()
    }

    @Test
    fun `should fail creating an auto bid when there is a domain error`() {
        val command = Builders.buildCreateAutoBidCommand()
        val user = Builders.buildUser()
        val auction = Builders.buildAuction()
        every { findAuction(any()) } returns auction.right()
        every { findUser(any()) } returns user.right()
        every { createAutoBid(any(),any(),any(),any(),any()) } returns AutoBidAlreadyExists.left()

        val result = createAutoBidService.invoke(command)

        result shouldBe AutoBidAlreadyExists.left()
    }
}
