package auction.infrastructure.`in`.http

import arrow.core.left
import arrow.core.right
import auction.application.service.CreateAuctionCommand
import auction.application.service.CreateAuctionService
import auction.application.service.CreateAutoBidCommand
import auction.application.service.CreateAutoBidService
import auction.domain.model.AuctionNotFound
import auction.domain.model.AutoBidNotFound
import auction.domain.model.InvalidOpeningDate
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
@WebFluxTest(CreateAutoBidHttpResource::class)
class CreateAutoBidHttpResourceTest(@Autowired val webTestClient: WebTestClient) {

    @MockkBean
    private lateinit var createAutoBid: CreateAutoBidService

    private val auctionId = UUID.randomUUID()
    private val bid = 10.toBigDecimal()
    private val userId = UUID.randomUUID()
    private val limit = 100.toBigDecimal()
    private val command = CreateAutoBidCommand(auctionId, userId, bid, limit)

    @Test
    fun `should create an auction`() {
        every { createAutoBid(command) } returns Unit.right()

        val response = webTestClient
            .post()
            .uri("/auto-bids")
            .contentType(APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                """{
                 "auction_id": "$auctionId",
                 "user_id": "$userId",
                 "bid": $bid,
                 "limit": $limit
                }"""))
            .exchange()

        response.expectStatus().isCreated
    }

    @Test
    fun `should fail if create auto bid usecase fails`() {
        every { createAutoBid(any()) } returns AuctionNotFound.left()

        val response = webTestClient
            .post()
            .uri("/auto-bids")
            .contentType(APPLICATION_JSON)
            .body(BodyInserters.fromValue(
                """{
                 "auction_id": "$auctionId",
                 "user_id": "$userId",
                 "bid": $bid,
                 "limit": $limit
                }"""))
            .exchange()

        response.expectStatus().is4xxClientError
    }
}