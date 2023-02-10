package auction.infrastructure.`in`.schedule

import arrow.core.right
import auction.application.service.EndAuctionCommand
import auction.application.service.EndAuctionService
import auction.application.service.OpenAuctionCommand
import auction.application.service.OpenAuctionService
import auction.domain.model.AuctionId
import auction.fixtures.FakeWithinTransaction
import auction.infrastructure.out.db.AuctionTask.EndAuctionTask
import auction.infrastructure.out.db.AuctionTask.OpenAuctionTask
import auction.infrastructure.out.db.AuctionTasksMongoRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.LocalDateTime
import java.util.UUID

class TriggerAuctionTasksSchedulerTest {

    private val openAuctionService = mockk<OpenAuctionService>(relaxed = true)

    private val endAuctionService = mockk<EndAuctionService>(relaxed = true)

    private val auctionTasksRepository = mockk<AuctionTasksMongoRepository>()

    private val taskScheduler = ThreadPoolTaskScheduler().also { it.initialize() }

    init {
        TriggerAuctionTasksScheduler(
            endAuctionService,
            openAuctionService,
            auctionTasksRepository,
            FakeWithinTransaction,
            taskScheduler
        )
    }

    @Test
    fun `should open pending auctions`() {
        val fstTask = OpenAuctionTask(LocalDateTime.parse("2007-12-03T10:14:40"), AuctionId(UUID.randomUUID()))
        val sndTask = OpenAuctionTask(LocalDateTime.parse("2007-12-03T10:14:41"), AuctionId(UUID.randomUUID()))
        every {
            auctionTasksRepository.findPendingToOpenAndRemove()
        } returns listOf(fstTask, sndTask) andThen emptyList()

        verify(timeout = 1000) {
            openAuctionService.invoke(OpenAuctionCommand(fstTask.auctionId.value))
            openAuctionService.invoke(OpenAuctionCommand(sndTask.auctionId.value))
        }
    }

    @Test
    fun `should open pending auctions although some of them crash`() {
        val fstTask = OpenAuctionTask(LocalDateTime.parse("2007-12-03T10:14:40"), AuctionId(UUID.randomUUID()))
        val sndTask = OpenAuctionTask(LocalDateTime.parse("2007-12-03T10:14:41"), AuctionId(UUID.randomUUID()))
        every {
            auctionTasksRepository.findPendingToOpenAndRemove()
        } returns listOf(fstTask, sndTask) andThen emptyList()
        every {
            openAuctionService.invoke(OpenAuctionCommand(fstTask.auctionId.value))
        } throws Exception("Boom!") andThen Unit.right()

        verify(timeout = 1000) {
            openAuctionService.invoke(OpenAuctionCommand(sndTask.auctionId.value))
        }
    }

    @Test
    fun `should end pending auctions`() {
        val fstTask = EndAuctionTask(LocalDateTime.parse("2007-12-03T10:14:40"), AuctionId(UUID.randomUUID()))
        val sndTask = EndAuctionTask(LocalDateTime.parse("2007-12-03T10:14:41"), AuctionId(UUID.randomUUID()))
        every {
            auctionTasksRepository.findPendingToEndAndRemove()
        } returns listOf(fstTask, sndTask) andThen emptyList()

        verify(timeout = 1000) {
            endAuctionService.invoke(EndAuctionCommand(fstTask.auctionId.value))
            endAuctionService.invoke(EndAuctionCommand(sndTask.auctionId.value))
        }
    }

    @Test
    fun `should end pending auctions although some of them crash`() {
        val fstTask = EndAuctionTask(LocalDateTime.parse("2007-12-03T10:14:40"), AuctionId(UUID.randomUUID()))
        val sndTask = EndAuctionTask(LocalDateTime.parse("2007-12-03T10:14:41"), AuctionId(UUID.randomUUID()))
        every {
            auctionTasksRepository.findPendingToEndAndRemove()
        } returns listOf(fstTask, sndTask) andThen emptyList()
        every {
            endAuctionService.invoke(EndAuctionCommand(fstTask.auctionId.value))
        } throws Exception("Boom!") andThen Unit.right()

        verify(timeout = 1000) {
            endAuctionService.invoke(EndAuctionCommand(sndTask.auctionId.value))
        }
    }
}
