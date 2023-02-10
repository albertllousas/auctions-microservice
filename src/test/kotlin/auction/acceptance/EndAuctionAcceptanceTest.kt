package auction.acceptance

import auction.fixtures.consumeAndAssert
import auction.fixtures.stubHttpEndpointForFindUserSucceeded
import auction.infrastructure.out.db.AuctionDBStatus.*
import auction.infrastructure.out.events.AuctionIntegrationEvent.*
import com.fasterxml.jackson.module.kotlin.readValue
import io.restassured.RestAssured
import io.restassured.http.ContentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

@Tag("acceptance")
class EndAuctionAcceptanceTest : BaseAcceptanceTest() {

    @Test
    fun `end an auction`() = runBlocking {
        val endAt = LocalDateTime.now(clock).minusMinutes(1)
        val id = mongo.givenAnAuctionExists(endAt = endAt, status = OPENED)
        mongo.givenATaskToEndAnAuctionExists(endAt, id)

        delay(3000)

        kafkaConsumer.consumeAndAssert(stream = "public.auctions") { record ->
            assertDoesNotThrow { mapper.readValue<AuctionEndedEvent>(record.value()) }
        }
    }
}
