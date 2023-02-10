package auction.domain.model

import java.util.UUID

data class ItemId(val value: UUID)

sealed class ItemStatus {
    object Available:  ItemStatus()
    data class Other(val status: String): ItemStatus()
}

data class Item(val id: ItemId, val status: ItemStatus, val sellerId: UserId) {

    fun isAvailable(): Boolean = status == ItemStatus.Available

    fun isOwnedBy(user: User): Boolean = sellerId == user.id
}
