package auction.infrastructure.out.http

import arrow.core.left
import arrow.core.right
import auction.domain.model.Item
import auction.domain.model.ItemId
import auction.domain.model.ItemNotFound
import auction.domain.model.ItemStatus
import auction.domain.model.UserId
import auction.fixtures.stubHttpEndpointForFindItemNonSucceeded
import auction.fixtures.stubHttpEndpointForFindItemNotFound
import auction.fixtures.stubHttpEndpointForFindItemSucceeded
import auction.infrastructure.cfg.GeneralConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.startWith
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID


@Tag("integration")
class ItemsHttpClientTest {

    private val itemsExternalService = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }

    private val okHttpClient = OkHttpClient.Builder().build()

    private val adapter = ItemsHttpClient(okHttpClient, itemsExternalService.baseUrl(), GeneralConfig().defaultObjectMapper())

    @Test
    fun `find an item`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        itemsExternalService.stubHttpEndpointForFindItemSucceeded(itemId = itemId, userId = userId)

        val result = adapter.findItem(ItemId(itemId))

        result shouldBe Item(ItemId(itemId), ItemStatus.Available, UserId(userId)).right()
    }

    @Test
    fun `fail when item does not exists`() {
        val itemId = UUID.randomUUID()
        itemsExternalService.stubHttpEndpointForFindItemNotFound(itemId)

        val result = adapter.findItem(ItemId(itemId))

        result shouldBe ItemNotFound.left()
    }

    @Test
    fun `crash when there is a non successful http response`() {
        val itemId = UUID.randomUUID()
        itemsExternalService.stubHttpEndpointForFindItemNonSucceeded(itemId)

        val exception = shouldThrow<HttpClientFailureException> {
            adapter.findItem(ItemId(itemId))
        }
        exception.message should startWith("Http call with 'ItemsHttpClient' to")
    }
}
