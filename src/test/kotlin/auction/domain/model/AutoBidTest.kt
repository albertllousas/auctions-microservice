package auction.domain.model

import arrow.core.left
import arrow.core.right
import auction.domain.model.AuctionStatus.Expired
import auction.domain.model.AuctionStatus.OnPreview
import auction.domain.model.AuctionStatus.Opened
import auction.fixtures.Builders
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime.now
import java.time.ZoneId
import java.util.UUID

class AutoBidTest {

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("UTC"))

    @Nested
    inner class CreatingAnAutoBid {

        @Test
        fun `should create an auto-bid`() {
            val bidder = Builders.buildUser()
            val auction = Builders.buildAuction(status = OnPreview)
            val id = UUID.randomUUID()

            val result = AutoBid.create(id, bidder, auction, ONE, BigDecimal("100"))

            assertThat(result).isEqualTo(
                AutoBidCreated(
                    auction = auction,
                    autoBid = AutoBid(
                        id = AutoBidId(id),
                        auctionId = auction.id,
                        userId = bidder.id,
                        amount = Amount(ONE),
                        limit = Amount(BigDecimal("100")),
                        enabled = true
                    )
                ).right()
            )
        }

        @Test
        fun `should fail creating an auto-bid for an already finished auction`() {
            val auction = Builders.buildAuction(status = Expired)

            val result = AutoBid.create(UUID.randomUUID(), Builders.buildUser(), auction, ONE, BigDecimal("100"))

            assertThat(result).isEqualTo(AuctionHasFinished.left())
        }

        @Test
        fun `should fail creating an auto-bid if the initial amount already bigger than the limit`() {
            val auction = Builders.buildAuction(
                currentBid = Bid(UserId(UUID.randomUUID()), Amount(BigDecimal("90")), now())
            )
            val result =
                AutoBid.create(UUID.randomUUID(), Builders.buildUser(), auction, BigDecimal("100"), ONE)

            assertThat(result).isEqualTo(AutoBidLimitReached.left())
        }

        @Test
        fun `should fail creating an auto-bid with an invalid amount`() {
            val result =
                AutoBid.create(UUID.randomUUID(), Builders.buildUser(), Builders.buildAuction(), BigDecimal("-1"), ONE)

            assertThat(result).isEqualTo(TooLowAmount.left())
        }

        @Nested
        inner class Bidding {

            @Test
            fun `should auto bid an ongoing auction`() {
                val currentBidder = Builders.buildUser()
                val newBidder = Builders.buildUser()
                val auction = Builders.buildAuction(
                    currentBid = Bid(currentBidder.id, Amount(TEN), now(clock)),
                    status = Opened,
                    sellToHighestBidWaitingPeriod = Duration.ofMinutes(1)
                )
                val autoBid = Builders.buildAutoBid(
                    auctionId = auction.id,
                    userId = newBidder.id,
                    amount = Amount(TEN),
                    limit = Amount(BigDecimal("100.00"))
                )

                val result = AutoBid.placeAutoBid(autoBid, auction, clock)

                assertThat(result).isEqualTo(
                    AutoBidPlaced(
                        auction = auction.copy(
                            currentBid = Bid(newBidder.id, Amount(BigDecimal("20")), now(clock)),
                            endAt = now(clock).plus(auction.sellToHighestBidPeriod)
                        ),
                        autoBid = autoBid
                    ).right()
                )
            }

            @Test
            fun `should fail auto bidding where is no current bid to auto bid`() {
                val newBidder = Builders.buildUser()
                val auction = Builders.buildAuction(
                    currentBid = null,
                    status = Opened,
                    sellToHighestBidWaitingPeriod = Duration.ofMinutes(1)
                )
                val autoBid = Builders.buildAutoBid(
                    auctionId = auction.id,
                    userId = newBidder.id,
                    amount = Amount(TEN),
                    limit = Amount(BigDecimal("100.00"))
                )

                val result = AutoBid.placeAutoBid(autoBid, auction, clock)

                assertThat(result).isEqualTo(NoBidToAutoBid.left())
            }

            @Test
            fun `should fail auto bidding when limit is reached`() {
                val currentBidder = Builders.buildUser()
                val newBidder = Builders.buildUser()
                val auction = Builders.buildAuction(
                    currentBid = Bid(currentBidder.id, Amount(TEN), now(clock)),
                    status = Opened,
                    sellToHighestBidWaitingPeriod = Duration.ofMinutes(1)
                )
                val autoBid = Builders.buildAutoBid(
                    auctionId = auction.id,
                    userId = newBidder.id,
                    amount = Amount(TEN),
                    limit = Amount(BigDecimal("15.00"))
                )

                val result = AutoBid.placeAutoBid(autoBid, auction, clock)

                assertThat(result).isEqualTo(AutoBidLimitReached.left())
            }

            @Test
            fun `should fail auto bidding when it is disablede`() {
                val currentBidder = Builders.buildUser()
                val newBidder = Builders.buildUser()
                val auction = Builders.buildAuction(
                    currentBid = Bid(currentBidder.id, Amount(TEN), now(clock)),
                    status = Opened,
                    sellToHighestBidWaitingPeriod = Duration.ofMinutes(1)
                )
                val autoBid = Builders.buildAutoBid(
                    auctionId = auction.id,
                    userId = newBidder.id,
                    amount = Amount(TEN),
                    limit = Amount(BigDecimal("100")),
                    enabled = false
                )

                val result = AutoBid.placeAutoBid(autoBid, auction, clock)

                assertThat(result).isEqualTo(AutoBidIsDisabled.left())
            }

            @Test
            fun `should fail auto bidding when auction is not matching with the one to auto bid`() {
                val currentBidder = Builders.buildUser()
                val newBidder = Builders.buildUser()
                val auction = Builders.buildAuction(
                    currentBid = Bid(currentBidder.id, Amount(TEN), now(clock)),
                    status = Opened,
                    sellToHighestBidWaitingPeriod = Duration.ofMinutes(1)
                )
                val autoBid = Builders.buildAutoBid(
                    auctionId = AuctionId(UUID.randomUUID()),
                    userId = newBidder.id,
                    amount = Amount(TEN),
                    limit = Amount(BigDecimal("100.00"))
                )

                val result = AutoBid.placeAutoBid(autoBid, auction, clock)

                assertThat(result).isEqualTo(AuctionNotMatching.left())
            }

            @Test
            fun `should fail auto bidding when placing the bid fail in the auction side`() {
                val currentBidder = Builders.buildUser()
                val newBidder = Builders.buildUser()
                val auction = Builders.buildAuction(
                    currentBid = Bid(currentBidder.id, Amount(TEN), now(clock)),
                    status = Expired,
                    sellToHighestBidWaitingPeriod = Duration.ofMinutes(1)
                )
                val autoBid = Builders.buildAutoBid(
                    auctionId = auction.id,
                    userId = newBidder.id,
                    amount = Amount(TEN),
                    limit = Amount(BigDecimal("100.00"))
                )

                val result = AutoBid.placeAutoBid(autoBid, auction, clock)

                assertThat(result.isLeft()).isTrue()
            }
        }
    }
}
