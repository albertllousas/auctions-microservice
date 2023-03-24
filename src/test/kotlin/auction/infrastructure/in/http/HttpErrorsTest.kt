package auction.infrastructure.`in`.http

import auction.domain.model.AuctionHasFinished
import auction.domain.model.AuctionNotFound
import auction.domain.model.AutoBidAlreadyExists
import auction.domain.model.InvalidOpeningDate
import auction.domain.model.ItemDoesNotBelongToTheSeller
import auction.domain.model.ItemNotAvailable
import auction.domain.model.ItemNotFound
import auction.domain.model.TooLowAmount
import auction.domain.model.UserNotFound
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity

class HttpErrorsTest {

    @TestFactory
    fun `should map create auction usecase domain errors to http errors`() =
        listOf(
            Pair(InvalidOpeningDate, ResponseEntity(HttpError("Invalid opening date"), UNPROCESSABLE_ENTITY)),
            Pair(
                ItemDoesNotBelongToTheSeller,
                ResponseEntity(HttpError("Item does not belong to the seller"), UNPROCESSABLE_ENTITY)
            ),
            Pair(ItemNotAvailable, ResponseEntity(HttpError("Item is not available"), UNPROCESSABLE_ENTITY)),
            Pair(UserNotFound, ResponseEntity(HttpError("User not found"), NOT_FOUND)),
            Pair(ItemNotFound, ResponseEntity(HttpError("Item not found"), NOT_FOUND)),
            Pair(TooLowAmount, ResponseEntity(HttpError("Too low amount"), UNPROCESSABLE_ENTITY)),
//            Pair(AutoBidAlreadyExists, ResponseEntity(HttpError("Auto bid already exists"), CONFLICT)),
//            Pair(AuctionHasFinished, ResponseEntity(HttpError("Auction has finished"), UNPROCESSABLE_ENTITY)),
//            Pair(AuctionNotFound, ResponseEntity(HttpError("Too low amount"), UNPROCESSABLE_ENTITY))
        ).map { (domainError, httpError) ->
            dynamicTest("map ${domainError::class.simpleName} to ${httpError.statusCode}") {
                domainError.asHttpError<HttpError>() shouldBe httpError
            }
        }

    @TestFactory
    fun `should map create auto bid usecase domain errors to http errors`() =
        listOf(
            Pair(UserNotFound, ResponseEntity(HttpError("User not found"), NOT_FOUND)),
            Pair(TooLowAmount, ResponseEntity(HttpError("Too low amount"), UNPROCESSABLE_ENTITY)),
            Pair(AutoBidAlreadyExists, ResponseEntity(HttpError("Auto bid already exists"), CONFLICT)),
            Pair(AuctionHasFinished, ResponseEntity(HttpError("Auction has finished"), UNPROCESSABLE_ENTITY)),
            Pair(AuctionNotFound, ResponseEntity(HttpError("Auction not found"), NOT_FOUND))
        ).map { (domainError, httpError) ->
            dynamicTest("map ${domainError::class.simpleName} to ${httpError.statusCode}") {
                domainError.asHttpError<HttpError>() shouldBe httpError
            }
        }
}
