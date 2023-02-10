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
import java.util.UUID

@Tag("acceptance")
class PlaceBidAcceptanceTest : BaseAcceptanceTest() {

    @Test
    fun `place a bid`() = runBlocking {
        val id = mongo.givenAnAuctionExists(status = OPENED)
        val bidderId = UUID.randomUUID()
        wireMockServer.stubHttpEndpointForFindUserSucceeded(bidderId)

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                """{
                 "amount": 2,
                 "highest_bid_number": 0,
                 "bidder_id": "$bidderId"                
                }"""
            )
            .port(servicePort)
            .`when`()
            .post("/auctions/$id/bids")
            .then()

        delay(3000)

        assertThat(response.extract().statusCode()).isEqualTo(201)
        kafkaConsumer.consumeAndAssert(stream = "public.auctions") { record ->
            assertDoesNotThrow { mapper.readValue<BidPlacedEvent>(record.value()) }
        }
    }
}
