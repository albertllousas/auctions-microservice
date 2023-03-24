package auction.infrastructure.`in`.http

import auction.application.service.CreateAuctionCommand
import auction.application.service.CreateAuctionService
import auction.application.service.CreateAutoBidCommand
import auction.application.service.CreateAutoBidService
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.accepted
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

private val created: ResponseEntity<Unit> = ResponseEntity(HttpStatus.CREATED)

@RestController
class CreateAutoBidHttpResource(private val createAutoBid: CreateAutoBidService) {

    @PostMapping("/auto-bids")
    fun create(@RequestBody request: CreateAutoBidHttpRequest): ResponseEntity<Unit> =
        createAutoBid(request.asCommand()).fold(ifLeft = { it.asHttpError() }, ifRight = { created })

    private fun CreateAutoBidHttpRequest.asCommand() =
        CreateAutoBidCommand(auctionId, userId, bid, limit)
}

@JsonNaming(SnakeCaseStrategy::class)
data class CreateAutoBidHttpRequest(val auctionId: UUID, val userId: UUID, val bid: BigDecimal, val limit: BigDecimal)
