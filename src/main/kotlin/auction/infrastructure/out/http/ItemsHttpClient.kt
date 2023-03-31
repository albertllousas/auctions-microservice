package auction.infrastructure.out.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auction.domain.model.FindItem
import auction.domain.model.Item
import auction.domain.model.ItemId
import auction.domain.model.ItemNotFound
import auction.domain.model.ItemStatus.Available
import auction.domain.model.ItemStatus.Other
import auction.domain.model.UserId
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.UUID

data class ItemsHttpClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val mapper: ObjectMapper
) {

    private val getItemUrl = "$baseUrl/catalog/items"

    val findItem: FindItem = { itemId ->
        Request.Builder().url("$getItemUrl/${itemId.value}").build()
            .let(okHttpClient::newCall)
            .let(Call::execute)
            .let(::parse)
            .map(::mapToDomain)
    }

    private fun parse(response: Response): Either<ItemNotFound, ItemHttpDto> =
        when {
            response.isSuccessful -> mapper.readValue<ItemHttpDto>(response.body!!.bytes()).right()
            response.code == 404 -> ItemNotFound.left()
            else -> throw HttpClientFailureException(
                httpClient = ItemsHttpClient::class.simpleName!!,
                method = "GET",
                url = "$getItemUrl/{itemId}",
                httpStatus = response.code,
                errorBody = response.body?.string()
            )
        }

    private fun mapToDomain(dto: ItemHttpDto) =
        Item(
            id = ItemId(dto.itemId),
            status = when (dto.status) {
                ItemHttpStatus.AVAILABLE -> Available
                else -> Other(dto.status.name)
            },
            sellerId = UserId(dto.userId)
        )
}

data class ItemHttpDto(
    @JsonProperty("item_id") val itemId: UUID,
    val status: ItemHttpStatus,
    @JsonProperty("user_id") val userId: UUID
)

enum class ItemHttpStatus {
    SOLD, ON_AUCTION, ON_PREVIEW, AVAILABLE, DISABLED
}


