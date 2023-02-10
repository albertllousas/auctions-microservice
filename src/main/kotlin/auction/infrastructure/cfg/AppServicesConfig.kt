package auction.infrastructure.cfg

import auction.application.service.CreateAuctionService
import auction.application.service.EndAuctionService
import auction.application.service.OpenAuctionService
import auction.application.service.PlaceBidService
import auction.application.service.UseCaseExecutionBuilder
import auction.domain.model.FindAuction
import auction.domain.model.FindItem
import auction.domain.model.FindUser
import auction.domain.model.PublishEvent
import auction.domain.model.ReportCrash
import auction.domain.model.ReportError
import auction.domain.model.SaveAuction
import auction.domain.model.WithinTransaction
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Configuration
class AppServicesConfig {

    @Bean
    fun useCaseExecutionBuilder(
        withinTransaction: WithinTransaction,
        publishEvent: PublishEvent,
        reportError: ReportError,
        reportCrash: ReportCrash
    ): UseCaseExecutionBuilder = UseCaseExecutionBuilder(
        withinTransaction,
        publishEvent,
        reportError,
        reportCrash
    )

    @Bean
    fun createAuctionService(
        findItem: FindItem,
        findUser: FindUser,
        saveAuction: SaveAuction,
        useCaseExecutionBuilder: UseCaseExecutionBuilder,
        @Value("\${auction.expiration.period:60m}") auctionExpirationPeriod: Duration,
        @Value("\${auction.sell-to-highest-bid.period:20m}") sellToHighestBidPeriod: Duration,
        clock: Clock,
    ) = CreateAuctionService(
        findUser = findUser,
        findItem = findItem,
        saveAuction = saveAuction,
        clock = clock,
        executeUseCase = useCaseExecutionBuilder.build(CreateAuctionService::class),
        newId = { UUID.randomUUID() },
        auctionExpirationPeriod = auctionExpirationPeriod,
        sellToHighestBidPeriod = sellToHighestBidPeriod
    )

    @Bean
    fun openAuctionService(
        findAuction: FindAuction,
        saveAuction: SaveAuction,
        useCaseExecutionBuilder: UseCaseExecutionBuilder,
        clock: Clock,
    ) = OpenAuctionService(
        findAuction, saveAuction, useCaseExecutionBuilder.build(OpenAuctionService::class), clock
    )

    @Bean
    fun placeBidService(
        findAuction: FindAuction,
        findUser: FindUser,
        saveAuction: SaveAuction,
        useCaseExecutionBuilder: UseCaseExecutionBuilder,
        clock: Clock
    ) = PlaceBidService(
        findAuction, findUser, saveAuction, useCaseExecutionBuilder.build(PlaceBidService::class), clock
    )

    @Bean
    fun endAuctionService(
        findAuction: FindAuction,
        saveAuction: SaveAuction,
        useCaseExecutionBuilder: UseCaseExecutionBuilder,
        clock: Clock,
    ) = EndAuctionService(
        findAuction, saveAuction, useCaseExecutionBuilder.build(EndAuctionService::class), clock
    )
}
