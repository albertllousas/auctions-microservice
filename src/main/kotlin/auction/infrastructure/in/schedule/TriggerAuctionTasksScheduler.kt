package auction.infrastructure.`in`.schedule

import auction.application.service.EndAuctionCommand
import auction.application.service.EndAuctionService
import auction.application.service.OpenAuctionCommand
import auction.application.service.OpenAuctionService
import auction.domain.model.WithinTransaction
import auction.infrastructure.out.db.AuctionTasksMongoRepository
import org.springframework.scheduling.TaskScheduler

class TriggerAuctionTasksScheduler(
    private val endAuction: EndAuctionService,
    private val openAuction: OpenAuctionService,
    private val auctionTasksMongoRepository: AuctionTasksMongoRepository,
    private val withinTransaction: WithinTransaction,
    scheduler: TaskScheduler,
    openTasksIntervalMs: Long = 200,
    endTasksIntervalMs: Long = 200
) {

    init {
        scheduler.scheduleWithFixedDelay(this::runOpenTasks, openTasksIntervalMs)
        scheduler.scheduleWithFixedDelay(this::runEndTasks, endTasksIntervalMs)
    }

    private fun runOpenTasks() = withinTransaction {
        auctionTasksMongoRepository
            .findPendingToOpenAndRemove()
            .forEach {
                runIgnoringExceptions {
                    openAuction(OpenAuctionCommand(it.auctionId.value))
                }
            }
    }

    private fun runEndTasks() = withinTransaction {
        auctionTasksMongoRepository
            .findPendingToEndAndRemove()
            .forEach {
                runIgnoringExceptions {
                    endAuction(EndAuctionCommand(it.auctionId.value))
                }
            }
    }

    private fun <T> runIgnoringExceptions(block: () -> T) = try { block() } catch (_: Exception) { }
}
