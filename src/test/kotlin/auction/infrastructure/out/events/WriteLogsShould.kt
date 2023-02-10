package auction.infrastructure.out.events

import auction.domain.model.Amount
import auction.domain.model.AuctionCreated
import auction.domain.model.AuctionOpened
import auction.domain.model.Bid
import auction.domain.model.BidPlaced
import auction.domain.model.UserId
import auction.fixtures.Builders.buildAuction
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.helpers.NOPLogger.*
import java.math.BigDecimal.*
import java.time.LocalDateTime.*
import java.util.UUID

class WriteLogsShould {

    private val logger = spyk(NOP_LOGGER)

    private val writeLogs = WriteLogs(logger)

    @Test
    fun `should handle an auction created event writing a log`() {
        val event = AuctionCreated(buildAuction())

        writeLogs(event)

        verify {
            logger.info(
                """domain-event: 'AuctionCreated', opening-date: '${event.auction.openingAt}', id: '${event.auction.id.value}'"""
            )
        }
    }

    @Test
    fun `should handle an auction started event writing a log`() {
        val event = AuctionOpened(buildAuction())

        writeLogs(event)

        verify {
            logger.info(
                """domain-event: 'AuctionOpened', id: '${event.auction.id.value}'"""
            )
        }
    }

    @Test
    fun `should handle a bid placed created event writing a log`() {
        val event = BidPlaced(buildAuction(currentBid = Bid(UserId(UUID.randomUUID()), Amount(TEN), now())))

        writeLogs(event)

        verify {
            logger.info(
                """domain-event: 'BidPlaced', id: '${event.auction.id.value}', bidder-id:'${event.auction.currentBid!!.bidderId.value}'"""
            )
        }
    }
}
