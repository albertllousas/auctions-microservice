package auction.infrastructure.out.events

import auction.application.service.CreateAuctionService
import auction.domain.model.UserNotFound
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger
import java.lang.Exception

internal class MonitorErrorsTest {

    private val metrics = SimpleMeterRegistry()

    private val logger = spyk(NOPLogger.NOP_LOGGER)

    private val monitorErrors = MonitorErrors(metrics, logger)

    @Test
    fun `should report a domain error`() {
        monitorErrors.reportError(UserNotFound, CreateAuctionService::class)

        verify { logger.warn("domain-error: 'UserNotFound', originator: 'CreateAuctionService'") }
        metrics.counter(
            "domain.error",
            listOf(Tag.of("type", "UserNotFound"), Tag.of("originator", "CreateAuctionService"))
        ).count() shouldBe 1.0
    }

    @Test
    fun `should report a crash`() {
        val boom = Exception("boom!")

        monitorErrors.reportCrash(boom, CreateAuctionService::class)

        verify { logger.error("originator: 'CreateAuctionService'", boom) }
        metrics.counter(
            "app.crash",
            listOf(Tag.of("type", "Exception"), Tag.of("originator", "CreateAuctionService"))
        ).count() shouldBe 1.0
    }
}