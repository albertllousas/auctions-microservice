package auction.infrastructure.`in`.http

import auction.application.service.CreateAuctionCommand
import auction.application.service.CreateAuctionService
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.accepted
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

private val accepted: ResponseEntity<Unit> = accepted().build()

@RestController
class CreateAuctionHttpResource(private val createAuction: CreateAuctionService) {

    @PostMapping("/auctions")
    fun create(@RequestBody request: CreateAuctionHttpRequest): ResponseEntity<Unit> =
        createAuction(request.asCommand()).fold(ifLeft = { it.asHttpError() }, ifRight = { accepted })

    private fun CreateAuctionHttpRequest.asCommand() =
        CreateAuctionCommand(sellerId, itemId, openingBid, minimalBid, openingDate)
}

@JsonNaming(SnakeCaseStrategy::class)
data class CreateAuctionHttpRequest(
    val sellerId: UUID,
    val itemId: UUID,
    val openingBid: BigDecimal,
    val minimalBid: BigDecimal,
    val openingDate: LocalDateTime
)
