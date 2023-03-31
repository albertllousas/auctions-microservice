package auction.infrastructure.out.events

import auction.domain.model.AuctionCreated
import auction.domain.model.HandleEvent
import auction.fixtures.Builders
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class NotifyInMemoryEventHandlersTest {

    @Test
    fun `notify domain event to the handlers`() {
        val event = AuctionCreated(Builders.buildAuction())
        val firstHandler = mockk<HandleEvent>(relaxed = true)
        val secondHandler = mockk<HandleEvent>(relaxed = true)
        val thirdHandler = mockk<HandleEvent>(relaxed = true)
        val publish = NotifyInMemoryEventHandlers(
            listOf(firstHandler, secondHandler, thirdHandler)
        )

        publish(event)

        verify {
            firstHandler.invoke(event)
            secondHandler.invoke(event)
            thirdHandler.invoke(event)
        }
    }
}