package auction.domain.model

import arrow.core.left
import arrow.core.right
import auction.domain.model.AuctionStatus.Expired
import auction.domain.model.AuctionStatus.ItemSold
import auction.domain.model.AuctionStatus.OnPreview
import auction.domain.model.AuctionStatus.Opened
import auction.domain.model.ItemStatus.Other
import auction.fixtures.Builders
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.TEN
import java.math.BigDecimal.ZERO
import java.time.Clock
import java.time.Duration.ofMinutes
import java.time.Instant
import java.time.LocalDateTime.now
import java.time.LocalDateTime.parse
import java.time.ZoneId.of
import java.util.UUID.randomUUID

class AuctionTest {

    private val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), of("UTC"))

    @Nested
    inner class CreatingAnAuction {

        private val newId = randomUUID()
        private val user = Builders.buildUser()
        private val item = Builders.buildItem(sellerId = user.id.value)
        private val openingDate = now(clock).plusDays(8)
        private val openingBid = TEN
        private val minimalBid = TEN
        private val oneMinute = ofMinutes(1)

        @Test
        fun `should create an auction`() {
            val result =
                Auction.create(newId, user, item, openingBid, minimalBid, openingDate, oneMinute, oneMinute, clock)

            result shouldBe AuctionCreated(
                Auction.reconstitute(
                    id = AuctionId(newId),
                    userId = user.id,
                    itemId = item.id,
                    openingAmount = Amount(openingBid),
                    minimalAmount = Amount(minimalBid),
                    openingAt = openingDate,
                    createdAt = now(clock),
                    status = OnPreview,
                    version = AggregateVersion(0),
                    currentBidNumber = 0,
                    currentBid = null,
                    endAt = openingDate.plusMinutes(1),
                    sellToHighestBidPeriod = oneMinute,

                    )
            ).right()
        }

        @Test
        fun `should fail creating an auction when item does not belong to the seller`() {
            val item = Builders.buildItem()
            val result =
                Auction.create(newId, user, item, openingBid, minimalBid, openingDate, oneMinute, oneMinute, clock)

            result shouldBe ItemDoesNotBelongToTheSeller.left()
        }

        @Test
        fun `should fail creating an auction when item is not available`() {
            val item = Builders.buildItem(sellerId = user.id.value, status = Other("sold"))
            val result =
                Auction.create(newId, user, item, openingBid, minimalBid, openingDate, oneMinute, oneMinute, clock)

            result shouldBe ItemNotAvailable.left()
        }

        @Test
        fun `should fail creating an auction when opening date is less than one week after creation`() {
            val result =
                Auction.create(
                    newId,
                    user,
                    item,
                    openingBid,
                    minimalBid,
                    now(clock).plusDays(1),
                    oneMinute,
                    oneMinute,
                    clock
                )

            result shouldBe InvalidOpeningDate.left()
        }

        @Test
        fun `should fail creating an auction when opening bid is invalid`() {
            val result =
                Auction.create(newId, user, item, BigDecimal("-1"), minimalBid, openingDate, oneMinute, oneMinute, clock)

            result shouldBe TooLowAmount.left()
        }

        @Test
        fun `should fail creating an auction when minimal bid is invalid`() {
            val result =
                Auction.create(newId, user, item, openingBid, BigDecimal("-1"), openingDate, oneMinute, oneMinute, clock)

            result shouldBe TooLowAmount.left()
        }
    }

    @Nested
    inner class OpeningAnAuction {

        @Test
        fun `should open an auction`() {
            val auction = Builders.buildAuction(
                status = OnPreview, openingAt = parse("2007-12-02T10:29:31")
            )

            val result = Auction.open(auction, clock)

            result shouldBe AuctionOpened(auction.copy(status = Opened)).right()
        }

        @Test
        fun `should fail opening an auction when it is too early to open (opening datetime hasn't arrived yet)`() {
            val auction = Builders.buildAuction(
                status = OnPreview, openingAt = parse("2007-12-03T10:31:29")
            )

            val result = Auction.open(auction, clock)

            result shouldBe TooEarlyToOpen.left()
        }

        @Test
        fun `should fail opening an auction when it was already opened`() {
            val auction = Builders.buildAuction(
                status = Opened, openingAt = parse("2007-12-03T10:31:29")
            )

            val result = Auction.open(auction, clock)

            result shouldBe AuctionAlreadyOpened.left()
        }

        @Test
        fun `should fail opening an auction when it has expired`() {
            val auction = Builders.buildAuction(
                status = Expired, openingAt = parse("2007-12-03T10:31:29")
            )

            val result = Auction.open(auction, clock)

            result shouldBe AuctionHasFinished.left()
        }

        @Test
        fun `should fail opening an auction when it has finished`() {
            val auction = Builders.buildAuction(
                status = ItemSold(winner = UserId(randomUUID())), openingAt = parse("2007-12-03T10:31:29")
            )

            val result = Auction.open(auction, clock)

            result shouldBe AuctionHasFinished.left()
        }
    }

    @Nested
    inner class PlacingBids {

        @Test
        fun `should place the first bid on an auction`() {
            val bidder = Builders.buildUser()
            val auction = Builders.buildAuction(status = Opened)

            val result = Auction.placeBid(auction, TEN, bidder.id, 0, clock)

            val bid = Bid(bidder.id, Amount(TEN), now(clock))
            result shouldBe BidPlaced(
                auction.copy(
                    currentBid = bid,
                    endAt = now(clock).plus(auction.sellToHighestBidPeriod)
                )
            ).right()
        }

        @Test
        fun `should outbid on an auction`() {
            val newBidder = Builders.buildUser()
            val firstBidder = Builders.buildUser()
            val bid = Bid(firstBidder.id, Amount(TEN), now(clock))
            val auction = Builders.buildAuction(currentBid = bid, status = Opened, currentBidBNumber = 2)

            val result = Auction.placeBid(auction, TEN, newBidder.id, 2, clock)

            val newBid = Bid(newBidder.id, Amount(BigDecimal("20")), now(clock))
            result shouldBe BidPlaced(
                auction.copy(
                    currentBid = newBid,
                    endAt = now(clock).plus(auction.sellToHighestBidPeriod)
                )
            ).right()
        }

        @Test
        fun `should fail placing a bid in an auction that is not open for bidding`() {
            val bidder = Builders.buildUser()
            val auction = Builders.buildAuction(status = OnPreview)

            val result = Auction.placeBid(auction, TEN, bidder.id, 0, clock)

            result shouldBe AuctionIsNotOpened.left()
        }

        @Test
        fun `should fail placing a bid when current bid counter is wrong`() {
            val bidder = Builders.buildUser()
            val bid = Bid(bidder.id, Amount(TEN), now(clock))
            val auction = Builders.buildAuction(currentBid = bid, status = Opened)

            val result = Auction.placeBid(auction, TEN, bidder.id, 4, clock)

            result shouldBe HighestBidHasChanged.left()
        }
    }

    @Nested
    inner class EndingAuctions {

        @Test
        fun `should sell an item`() {
            val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), of("UTC"))
            val bidderId = UserId(randomUUID())
            val auction = Builders.buildAuction(
                status = Opened,
                endAt = parse("2007-12-03T10:14:31"),
                currentBid = Bid(bidderId = bidderId, amount = Amount(TEN), ts = now(clock))
            )

            val result = Auction.end(auction, clock)

            result shouldBe AuctionEnded(auction.copy(status = ItemSold(winner = bidderId))).right()
        }

        @Test
        fun `should expire an auction when there are no bids`() {
            val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), of("UTC"))
            val auction = Builders.buildAuction(
                status = Opened, endAt = parse("2007-12-03T10:14:31")
            )

            val result = Auction.end(auction, clock)

            result shouldBe AuctionEnded(auction.copy(status = Expired)).right()
        }

        @Test
        fun `should failing ending an auction when it is too early`() {
            val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), of("UTC"))
            val auction = Builders.buildAuction(
                status = OnPreview, endAt = parse("2007-12-03T10:29:31")
            )

            val result = Auction.end(auction, clock)

            result shouldBe TooEarlyToEnd.left()
        }

        @Test
        fun `should failing ending an auction when it is not open`() {
            val clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), of("UTC"))
            val auction = Builders.buildAuction(
                status = Expired, endAt = parse("2007-12-03T10:14:31")
            )

            val result = Auction.end(auction, clock)

            result shouldBe AuctionIsNotOpened.left()
        }
    }

}
