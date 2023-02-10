package auction.infrastructure.cfg

import auction.domain.model.FindItem
import auction.domain.model.FindUser
import auction.infrastructure.out.http.ItemsHttpClient
import auction.infrastructure.out.http.UsersHttpClient
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit.MILLISECONDS

@Configuration
class HttpClientsConfig {

    @Bean
    fun itemsHttpClient(
        defaultObjectMapper: ObjectMapper,
        meterRegistry: MeterRegistry,
        @Value("\${http.clients.items.url}") url: String,
        @Value("\${http.clients.items.connectTimeoutMillis}") connectTimeout: Long,
        @Value("\${http.clients.items.readTimeoutMillis}") readTimeout: Long,
        @Value("\${http.clients.items.callTimeoutMillis}") callTimeout: Long
    ): ItemsHttpClient = ItemsHttpClient(
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, MILLISECONDS)
            .readTimeout(readTimeout, MILLISECONDS)
            .callTimeout(callTimeout, MILLISECONDS)
            .build(),
        baseUrl = url,
        mapper = defaultObjectMapper
    )

    @Bean
    fun findItem(itemsHttpClient: ItemsHttpClient): FindItem = itemsHttpClient.findItem

    @Bean
    fun usersHttpClient(
        defaultObjectMapper: ObjectMapper,
        meterRegistry: MeterRegistry,
        @Value("\${http.clients.users.url}") url: String,
        @Value("\${http.clients.users.connectTimeoutMillis}") connectTimeout: Long,
        @Value("\${http.clients.users.readTimeoutMillis}") readTimeout: Long,
        @Value("\${http.clients.users.callTimeoutMillis}") callTimeout: Long
    ): UsersHttpClient = UsersHttpClient(
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, MILLISECONDS)
            .readTimeout(readTimeout, MILLISECONDS)
            .callTimeout(callTimeout, MILLISECONDS)
            .build(),
        baseUrl = url,
        mapper = defaultObjectMapper
    )

    @Bean
    fun findUser(usersHttpClient: UsersHttpClient): FindUser = usersHttpClient.findUser
}
