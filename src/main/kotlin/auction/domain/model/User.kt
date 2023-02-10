package auction.domain.model

import java.util.UUID

data class UserId(val value: UUID)

data class User(val id: UserId)
