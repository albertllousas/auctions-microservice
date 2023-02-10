package auction.infrastructure.out.events

import auction.domain.model.Amount
import auction.domain.model.AuctionCreated
import auction.domain.model.AuctionEnded
import auction.domain.model.AuctionOpened
import auction.domain.model.Bid
import auction.domain.model.BidPlaced
import auction.domain.model.UserId
import auction.fixtures.Builders
import auction.infrastructure.out.db.AuctionTask
import auction.infrastructure.out.db.AuctionTask.OpenAuctionTask
import auction.infrastructure.out.db.AuctionTasksMongoRepository
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.*
import java.time.LocalDateTime
import java.time.LocalDateTime.*
import java.util.UUID
import java.util.UUID.*

class ScheduleAuctionTaskTest {

    private val auctionTasksRepository = mockk<AuctionTasksMongoRepository>(relaxed = true)

    private val scheduleAuctionTask = ScheduleAuctionTask(auctionTasksRepository)

    @Test
    fun `should schedule an start auction task when an auction is created`() {
        val event = AuctionCreated(Builders.buildAuction())

        scheduleAuctionTask(event)

        verify { auctionTasksRepository.save(OpenAuctionTask(event.auction.openingAt, event.auction.id)) }
    }

    @Test
    fun `should schedule an end auction task when an auction is started`() {
        val event = AuctionOpened(Builders.buildAuction())

        scheduleAuctionTask(event)

        verify {
            auctionTasksRepository.save(
                AuctionTask.EndAuctionTask(event.auction.endAt, event.auction.id)
            )
        }
    }

    @Test
    fun `should schedule an end auction task when a bid is placed`() {
        val event = BidPlaced(
            Builders.buildAuction(currentBid = Bid(UserId(randomUUID()), Amount(TEN), now()))
        )

        scheduleAuctionTask(event)

        verify {
            auctionTasksRepository.save(
                AuctionTask.EndAuctionTask(event.auction.endAt, event.auction.id)
            )
        }
    }

    @Test
    fun `should not schedule any task when an auction is ended`() {
        val event = AuctionEnded(Builders.buildAuction())

        scheduleAuctionTask(event)

        verify { auctionTasksRepository wasNot Called}
    }
}