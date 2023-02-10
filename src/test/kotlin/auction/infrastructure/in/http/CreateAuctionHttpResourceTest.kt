package auction.infrastructure.`in`.http

import arrow.core.left
import arrow.core.right
import auction.application.service.CreateAuctionCommand
import auction.application.service.CreateAuctionService
import auction.domain.model.InvalidOpeningDate
import auction.fixtures.Builders
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import java.util.UUID

@Tag("integration")
@WebFluxTest(CreateAuctionHttpResource::class)
class CreateAuctionHttpResourceTest(@Autowired val webTestClient: WebTestClient) {

    @MockkBean
    private lateinit var createAuction: CreateAuctionService

    private val sellerId = UUID.randomUUID()
    private val openingBid = 100.toBigDecimal()
    private val itemId = UUID.randomUUID()
    private val minimalBid = 10.toBigDecimal()
    private val openingDate = LocalDateTime.now().plusDays(1)
    private val command = CreateAuctionCommand(sellerId, itemId, openingBid, minimalBid, openingDate)

    @Test
    fun `should create an auction`() {
        every { createAuction(command) } returns Unit.right()

        val response = webTestClient
            .post()
            .uri("/auctions")
            .contentType(APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                """{
                 "seller_id": "$sellerId",
                 "item_id": "$itemId",
                 "opening_bid": $openingBid,
                 "minimal_bid": $minimalBid,
                 "opening_date": "$openingDate"                 
                }"""))
            .exchange()

        response.expectStatus().isAccepted
    }

    @Test
    fun `should fail if create auction usecase fails`() {
        every { createAuction(any()) } returns InvalidOpeningDate.left()

        val response = webTestClient
            .post()
            .uri("/auctions")
            .contentType(APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                """{
                 "seller_id": "$sellerId",
                 "item_id": "$itemId",
                 "opening_bid": $openingBid,
                 "minimal_bid": $minimalBid,
                 "opening_date": "$openingDate"                 
                }"""))
            .exchange()

        response.expectStatus().is4xxClientError
    }
}