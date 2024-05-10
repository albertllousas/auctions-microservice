package auction.domain.model

import java.util.UUID

data class User(val id: UserId)

data class UserId(val value: UUID)
