package auction.infrastructure.out.http

import arrow.core.left
import arrow.core.right
import auction.domain.model.User
import auction.domain.model.UserId
import auction.domain.model.UserNotFound
import auction.fixtures.stubHttpEndpointForFindUserNonSucceeded
import auction.fixtures.stubHttpEndpointForFindUserNotFound
import auction.fixtures.stubHttpEndpointForFindUserSucceeded
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
class UsersHttpClientTest {

    private val usersExternalService = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }

    private val okHttpClient = OkHttpClient.Builder().build()

    private val adapter = UsersHttpClient(okHttpClient, usersExternalService.baseUrl(), GeneralConfig().defaultObjectMapper())

    @Test
    fun `find a user`() {
        val userId = UUID.randomUUID()
        usersExternalService.stubHttpEndpointForFindUserSucceeded(userId = userId)

        val result = adapter.findUser(UserId(userId))

        result shouldBe User(UserId(userId)).right()
    }

    @Test
    fun `fail when user does not exists`() {
        val userId = UUID.randomUUID()
        usersExternalService.stubHttpEndpointForFindUserNotFound(userId)

        val result = adapter.findUser(UserId(userId))

        result shouldBe UserNotFound.left()
    }

    @Test
    fun `crash when there is a non successful http response`() {
        val userId = UUID.randomUUID()
        usersExternalService.stubHttpEndpointForFindUserNonSucceeded(userId)

        val exception = shouldThrow<HttpClientFailureException> {
            adapter.findUser(UserId(userId))
        }
        exception.message should startWith("Http call with 'ItemsHttpClient' to")
    }
}
