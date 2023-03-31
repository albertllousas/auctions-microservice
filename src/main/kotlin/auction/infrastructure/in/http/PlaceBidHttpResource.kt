package auction.infrastructure.`in`.http

import auction.application.service.PlaceBidCommand
import auction.application.service.PlaceBidService
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

private val created: ResponseEntity<Unit> = ResponseEntity(HttpStatus.CREATED)

@RestController
class PlaceBidHttpResource(private val placeBid: PlaceBidService) {

    @PostMapping("/auctions/{auctionId}/bids")
    fun post(
        @PathVariable("auctionId") auctionId: UUID,
        @RequestBody request: PlaceBidHttpRequest
    ): ResponseEntity<Unit> =
        placeBid(request.asCommand(auctionId)).fold(ifLeft = { it.asHttpError() }, ifRight = { created })

    private fun PlaceBidHttpRequest.asCommand(auctionId: UUID) =
        PlaceBidCommand(auctionId, amount, currentBidCounter, bidderId)
}

@JsonNaming(SnakeCaseStrategy::class)
data class PlaceBidHttpRequest(val bidderId: UUID, val amount: BigDecimal, val currentBidCounter: Long)
