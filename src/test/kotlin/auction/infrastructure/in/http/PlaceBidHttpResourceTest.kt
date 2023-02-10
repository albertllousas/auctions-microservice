package auction.infrastructure.`in`.http

import arrow.core.right
import auction.application.service.PlaceBidCommand
import auction.application.service.PlaceBidService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.util.UUID

@Tag("integration")
@WebFluxTest(PlaceBidHttpResource::class)
class PlaceBidHttpResourceTest(@Autowired val webTestClient: WebTestClient) {

    @MockkBean
    private lateinit var placeBid: PlaceBidService

    private val auctionId = UUID.randomUUID()
    private val amount = 100.toBigDecimal()
    private val highestBidNumber = 11L
    private val bidderId = UUID.randomUUID()
    private val command = PlaceBidCommand(auctionId, amount, highestBidNumber, bidderId)

    @Test
    fun `should place a bid`() {
        every { placeBid(command) } returns Unit.right()

        println("$auctionId $amount $bidderId, $highestBidNumber")

        val response = webTestClient
            .post()
            .uri("/auctions/$auctionId/bids")
            .contentType(APPLICATION_JSON)
            .body(
                BodyInserters.fromValue(
                    """{
                         "amount": $amount,
                         "current_bid_counter": $highestBidNumber,
                         "bidder_id": "$bidderId"                
                        }"""
                )
            )
            .exchange()

        response.expectStatus().isCreated
    }

//    @Test
//    fun `should fail if create auction usecase fails`() {
//        every { createAuction(any()) } returns InvalidOpeningDate.left()
//
//        val response = webTestClient
//            .post()
//            .uri("/auctions")
//            .contentType(APPLICATION_JSON)
//            .body(
//                BodyInserters.fromValue(
//                    """{
//                 "seller_id": "$sellerId",
//                 "item_id": "$itemId",
//                 "opening_bid": $openingBid,
//                 "minimal_bid": $minimalBid,
//                 "opening_date": "$openingDate"
//                }"""
//                )
//            )
//            .exchange()
//
//        response.expectStatus().is4xxClientError
//    }
}