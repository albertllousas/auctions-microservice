package auction.infrastructure.`in`.messaging

import auction.application.service.DisableAutoBidCommand
import auction.application.service.DisableAutoBidService
import auction.application.service.PlaceAutoBidCommand
import auction.application.service.PlaceAutoBidService
import auction.domain.model.AuctionId
import auction.domain.model.FindAutoBidsByAuction
import auction.infrastructure.cfg.GeneralConfig
import auction.infrastructure.out.events.AuctionIntegrationEvent
import auction.infrastructure.out.events.AuctionIntegrationEvent.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component
class ConsumeBidPlacedEvents(
    val mapper: ObjectMapper = GeneralConfig.defaultObjectMapper,
    val findAutoBidsByAuction: FindAutoBidsByAuction,
    val placeAutoBid: PlaceAutoBidService,
    val disableAutoBid: DisableAutoBidService,
    val noop: () -> Unit = {}
) {

    @KafkaListener(topics = ["public.auctions"], groupId = "auction-service")
    fun onMessage(message: Message<ByteArray>) {
        when(val event = mapper.readValue<AuctionIntegrationEvent>(message.payload)) {
            is BidPlacedEvent ->
                findAutoBidsByAuction(AuctionId(event.auction.id))
                    .map { Pair(it, placeAutoBid(PlaceAutoBidCommand(it.id.value))) }
                    .filter { it.second.isLeft() }
                    .forEach { disableAutoBid(DisableAutoBidCommand(it.first.id.value)) }
            else -> noop()
        }
    }
}
