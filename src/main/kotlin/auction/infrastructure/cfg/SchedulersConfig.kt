package auction.infrastructure.cfg

import auction.application.service.EndAuctionService
import auction.application.service.OpenAuctionService
import auction.domain.model.WithinTransaction
import auction.infrastructure.`in`.schedule.TriggerAuctionTasksScheduler
import auction.infrastructure.out.db.AuctionTasksMongoRepository
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.schedule.PollOutboxForPublishing
import auction.infrastructure.out.stream.PublishOutboxMessageToKafka
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler

@Configuration
class SchedulersConfig() {

    @Bean
    fun pollOutboxForPublishing(
        withinTransaction: WithinTransaction,
        outboxMongoRepository: OutboxMongoRepository,
        publishOutboxMessageToKafka: PublishOutboxMessageToKafka,
        scheduler: TaskScheduler,
        meterRegistry: MeterRegistry
    ) = PollOutboxForPublishing(
        outboxMongoRepository,
        publishOutboxMessageToKafka,
        scheduler,
        withinTransaction,
        meterRegistry
    )

    @Bean
    fun triggerAuctionTasksScheduler(
        openAuctionService: OpenAuctionService,
        endAuctionService: EndAuctionService,
        auctionTasksMongoRepository: AuctionTasksMongoRepository,
        withinTransaction: WithinTransaction,
        scheduler: TaskScheduler,
    ) = TriggerAuctionTasksScheduler(
        endAuctionService, openAuctionService, auctionTasksMongoRepository, withinTransaction, scheduler
    )
}