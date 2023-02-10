package auction.acceptance

import auction.fixtures.consumeAndAssert
import auction.infrastructure.out.events.AuctionIntegrationEvent.AuctionOpenedEvent
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime

@Tag("acceptance")
class StartAuctionAcceptanceTest : BaseAcceptanceTest() {

    @Test
    fun `start an auction successfully`() = runBlocking {
        val openingAt = LocalDateTime.now(clock).minusMinutes(1)
        val id = mongo.givenAnAuctionExists(openingAt)
        mongo.givenATaskToOpenAnAuctionExists(openingAt, id)

        delay(3000)

        kafkaConsumer.consumeAndAssert(stream = "public.auctions") { record ->
            assertDoesNotThrow { mapper.readValue<AuctionOpenedEvent>(record.value()) }
        }
    }
}
