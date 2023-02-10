package auction.domain.model

import auction.domain.model.ItemStatus.Other
import auction.fixtures.Builders
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class ItemTest {

    @Test
    fun `should check if an tem is available`() {
        Builders.buildItem().isAvailable() shouldBe true
    }

    @Test
    fun `should check if an tem is not available`() {
        Builders.buildItem(status = Other("other")).isAvailable() shouldBe false
    }

    @Test
    fun `should check if an item belongs to a user`() {
        val sellerId = UUID.randomUUID()
        val user = Builders.buildUser(id = sellerId)
        val item = Builders.buildItem(sellerId = sellerId)

        item.isOwnedBy(user) shouldBe true
    }

    @Test
    fun `should check if an item does not belong to a user`() {
        val user = Builders.buildUser()
        val item = Builders.buildItem()

        item.isOwnedBy(user) shouldBe false
    }
}