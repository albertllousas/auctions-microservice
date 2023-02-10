package auction.fixtures

import auction.domain.model.UserId
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.status
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import java.util.UUID

fun WireMockServer.stubHttpEndpointForFindItemNonSucceeded(
    itemId: UUID,
    responseCode: Int = 400,
    responseErrorBody: String = """{"status":400,"detail":"Some problem"}""",
): StubMapping =
    this.stubFor(
        get(urlEqualTo("/catalog/items/$itemId"))
            .willReturn(status(responseCode).withBody(responseErrorBody))
    )

fun WireMockServer.stubHttpEndpointForFindItemNotFound(itemId: UUID): StubMapping =
    this.stubHttpEndpointForFindItemNonSucceeded(
        itemId, 404, """ {"status":404,"detail":"Item not found: $itemId"} """
    )

fun WireMockServer.stubHttpEndpointForFindItemSucceeded(
    itemId: UUID? = null,
    userId: UUID =UUID.randomUUID()
): StubMapping =
    this.stubFor(
        get(itemId?.let { urlEqualTo("/catalog/items/$itemId") } ?: urlPathMatching("/catalog/items/.*"))
            .willReturn(
                status(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                            {
                              "item_id": "${itemId ?: UUID.randomUUID()}",
                              "status": "AVAILABLE",
                              "user_id": "$userId"
                            }
                        """
                    )
            )
    )
