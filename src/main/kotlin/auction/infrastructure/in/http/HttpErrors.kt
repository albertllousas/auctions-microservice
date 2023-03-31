package auction.infrastructure.`in`.http

import auction.domain.model.AuctionHasFinished
import auction.domain.model.AuctionIsNotOpened
import auction.domain.model.AuctionNotFound
import auction.domain.model.AutoBidAlreadyExists
import auction.domain.model.AutoBidLimitReached
import auction.domain.model.CreateAuctionUseCaseError
import auction.domain.model.CreateAutoBidUseCaseError
import auction.domain.model.HighestBidHasChanged
import auction.domain.model.InvalidOpeningDate
import auction.domain.model.ItemDoesNotBelongToTheSeller
import auction.domain.model.ItemNotAvailable
import auction.domain.model.ItemNotFound
import auction.domain.model.PlaceBidUseCaseError
import auction.domain.model.TooLowAmount
import auction.domain.model.UserNotFound
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity


fun <T> CreateAuctionUseCaseError.asHttpError(): ResponseEntity<T> = when (this) {
    InvalidOpeningDate -> unprocessableEntity("Invalid opening date")
    ItemDoesNotBelongToTheSeller -> unprocessableEntity("Item does not belong to the seller")
    ItemNotAvailable -> unprocessableEntity("Item is not available")
    UserNotFound -> notFound("User not found")
    ItemNotFound -> notFound("Item not found")
    TooLowAmount -> unprocessableEntity("Too low amount")
} as ResponseEntity<T>

fun <T> PlaceBidUseCaseError.asHttpError(): ResponseEntity<T> = when (this) {
    AuctionNotFound -> notFound("Auction not found")
    TooLowAmount -> unprocessableEntity("Too low amount")
    HighestBidHasChanged -> conflict("Highest bid has changed")
    UserNotFound -> notFound("User not found")
    AuctionIsNotOpened -> unprocessableEntity("Auction not open for bidding")
} as ResponseEntity<T>

fun <T> CreateAutoBidUseCaseError.asHttpError(): ResponseEntity<T> = when (this) {
    AuctionNotFound -> notFound("Auction not found")
    TooLowAmount -> unprocessableEntity("Too low amount")
    UserNotFound -> notFound("User not found")
    AuctionHasFinished -> unprocessableEntity("Auction has finished")
    AutoBidAlreadyExists -> conflict("Auto bid already exists")
    AutoBidLimitReached -> unprocessableEntity("Auto bid limit reached")
} as ResponseEntity<T>

private fun unprocessableEntity(msg: String) = ResponseEntity(HttpError(msg), UNPROCESSABLE_ENTITY)

private fun notFound(msg: String) = ResponseEntity(HttpError(msg), NOT_FOUND)

private fun conflict(msg: String) = ResponseEntity(HttpError(msg), CONFLICT)

data class HttpError(val detail: String)
