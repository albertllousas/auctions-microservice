package auction.acceptance

import auction.fixtures.consumeAndAssert
import auction.fixtures.stubHttpEndpointForFindItemSucceeded
import auction.fixtures.stubHttpEndpointForFindUserSucceeded
import auction.infrastructure.out.events.AuctionIntegrationEvent.*
import com.fasterxml.jackson.module.kotlin.readValue
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

@Tag("acceptance")
class CreateAuctionAcceptanceTest : BaseAcceptanceTest() {

    @Test
    fun `create an auction successfully`() {
        val sellerId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        wireMockServer.stubHttpEndpointForFindItemSucceeded(itemId = itemId, userId = sellerId)
        wireMockServer.stubHttpEndpointForFindUserSucceeded(userId = sellerId)

        val response = given()
            .contentType(JSON)
            .body(
                """{
                 "seller_id": "$sellerId",
                 "item_id": "$itemId",
                 "opening_bid": ${100.toBigDecimal()},
                 "minimal_bid": ${10.toBigDecimal()},
                 "opening_date": "${LocalDateTime.now().plusDays(8)}"                 
                }"""
            )
            .port(servicePort)
            .`when`()
            .post("/auctions")
            .then()

        assertThat(response.extract().statusCode()).isEqualTo(202)
        kafkaConsumer.consumeAndAssert(stream = "public.auctions") { record ->
            assertDoesNotThrow { mapper.readValue<AuctionCreatedEvent>(record.value()) }
        }
    }
}
