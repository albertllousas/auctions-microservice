package auction.domain.model

sealed interface DomainError

// usecase/ application-service

sealed interface CreateAuctionUseCaseError : DomainError

sealed interface OpenAuctionUseCaseError : DomainError

sealed interface PlaceBidUseCaseError : DomainError

sealed interface EndAuctionUseCaseError : DomainError

// domain model

sealed interface CreateAuctionError : CreateAuctionUseCaseError

sealed interface CreateAmountError : CreateAuctionError, PlaceBidError

sealed interface OpenAuctionError : OpenAuctionUseCaseError

sealed interface PlaceBidError : PlaceBidUseCaseError

sealed interface EndAuctionError : EndAuctionUseCaseError

// errors

object UserNotFound : CreateAuctionUseCaseError, PlaceBidUseCaseError

object ItemNotFound : CreateAuctionUseCaseError

object ItemDoesNotBelongToTheSeller : CreateAuctionError

object ItemNotAvailable : CreateAuctionError

object TooLowAmount : CreateAmountError, PlaceBidError

object InvalidOpeningDate : CreateAuctionError

object AuctionNotFound : OpenAuctionUseCaseError, PlaceBidUseCaseError, EndAuctionUseCaseError

object AuctionAlreadyOpened : OpenAuctionError

object AuctionHasFinished : OpenAuctionError

object TooEarlyToOpen : OpenAuctionError

object HighestBidHasChanged : PlaceBidError

object AuctionIsNotOpened : PlaceBidError, EndAuctionUseCaseError

object TooEarlyToEnd : EndAuctionError
