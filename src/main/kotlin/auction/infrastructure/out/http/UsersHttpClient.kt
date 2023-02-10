package auction.infrastructure.out.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auction.domain.model.FindUser
import auction.domain.model.User
import auction.domain.model.UserId
import auction.domain.model.UserNotFound
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.UUID

@Deprecated(
    message = "Http calls have been replaced by stream consuming",
    replaceWith = ReplaceWith("ReplicatedUsersMongoDBRepository")
)
data class UsersHttpClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val mapper: ObjectMapper
    ) {

    private val getUsersUrl = "$baseUrl/users"

    val findUser : FindUser = { userId ->
        Request.Builder().url("$getUsersUrl/${userId.value}").build()
            .let(okHttpClient::newCall)
            .let(Call::execute)
            .let(::parse)
            .map(::mapToDomain)
    }

    private fun parse(response: Response): Either<UserNotFound, UserHttpDto> =
        when {
            response.isSuccessful -> mapper.readValue<UserHttpDto>(response.body!!.bytes()).right()
            response.code == 404 -> UserNotFound.left()
            else -> throw HttpClientFailureException(
                httpClient = ItemsHttpClient::class.simpleName!!,
                method = "GET",
                url = "$baseUrl/users/{userId}",
                httpStatus = response.code,
                errorBody = response.body?.string()
            )
        }

    private fun mapToDomain(dto: UserHttpDto) = User(UserId(dto.userId))
}

data class UserHttpDto(val userId: UUID)


