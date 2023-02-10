package auction.infrastructure.out.http

data class HttpClientFailureException(
    val httpClient: String,
    val method: String,
    val url: String,
    val errorBody: String?,
    val httpStatus: Int,
) : RuntimeException(
    "Http call with '$httpClient' to '$method $url' failed with status '$httpStatus' and body '$errorBody'"
)
