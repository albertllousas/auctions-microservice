package auction.infrastructure.cfg

import auction.domain.model.HandleEvent
import auction.domain.model.PublishEvent
import auction.infrastructure.out.db.AuctionTasksMongoRepository
import auction.infrastructure.out.db.IsAnActiveTransaction
import auction.infrastructure.out.db.OutboxMongoRepository
import auction.infrastructure.out.events.NotifyInMemoryEventHandlers
import auction.infrastructure.out.events.PublishMetrics
import auction.infrastructure.out.events.MonitorErrors
import auction.infrastructure.out.events.ScheduleAuctionTask
import auction.infrastructure.out.events.PublishAuctionChanges
import auction.infrastructure.out.events.WriteLogs
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class EventsConfig {

    @Bean
    fun publishMetrics(meterRegistry: MeterRegistry): HandleEvent = PublishMetrics(meterRegistry)

    @Bean
    fun writeLogs(): HandleEvent = WriteLogs()

    @Bean
    fun scheduleAuctionTask(auctionTasksMongoRepository: AuctionTasksMongoRepository): HandleEvent =
        ScheduleAuctionTask(auctionTasksMongoRepository)

    @Bean
    fun storeOutboxMessage(
        @Value("\${kafka.producer.auctions.topic}") auctionsStream: String,
        outboxMongoRepository: OutboxMongoRepository,
        isAnActiveTransaction: IsAnActiveTransaction,
        clock: Clock,
        defaultObjectMapper: ObjectMapper
    ): HandleEvent =
        PublishAuctionChanges(
            isAnActiveTransaction::invoke,
            auctionsStream,
            outboxMongoRepository,
            clock,
            defaultObjectMapper
        )

    @Bean
    fun publishEvent(eventHandlers: List<HandleEvent>): PublishEvent = NotifyInMemoryEventHandlers(eventHandlers)

    @Bean
    fun monitorErrors(meterRegistry: MeterRegistry) = MonitorErrors(meterRegistry)

    @Bean
    fun reportError(monitorErrors: MonitorErrors) = monitorErrors.reportError

    @Bean
    fun reportCrash(monitorErrors: MonitorErrors) = monitorErrors.reportCrash
}
