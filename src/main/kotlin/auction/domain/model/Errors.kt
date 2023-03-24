package auction.domain.model

sealed interface DomainError

// usecase/ application-service

sealed interface CreateAuctionUseCaseError : DomainError

sealed interface OpenAuctionUseCaseError : DomainError

sealed interface PlaceBidUseCaseError : DomainError

sealed interface EndAuctionUseCaseError : DomainError

sealed interface SubscribeUseCaseError : DomainError

sealed interface CreateAutoBidUseCaseError : DomainError

sealed interface PlaceAutoBidUseCaseError : DomainError

sealed interface DisableAutoBidUseCaseError : DomainError

// domain model

sealed interface CreateAuctionError : CreateAuctionUseCaseError

sealed interface CreateAmountError : CreateAuctionError, PlaceBidError

sealed interface OpenAuctionError : OpenAuctionUseCaseError

sealed interface PlaceBidError : PlaceBidUseCaseError, PlaceAutoBidError

sealed interface EndAuctionError : EndAuctionUseCaseError

sealed interface CreateAutoBidError : CreateAutoBidUseCaseError

sealed interface PlaceAutoBidError : PlaceAutoBidUseCaseError

// errors

object UserNotFound : CreateAuctionUseCaseError, PlaceBidUseCaseError, CreateAutoBidUseCaseError

object ItemNotFound : CreateAuctionUseCaseError

object ItemDoesNotBelongToTheSeller : CreateAuctionError

object ItemNotAvailable : CreateAuctionError

object TooLowAmount : CreateAmountError, PlaceBidError, CreateAutoBidError

object InvalidOpeningDate : CreateAuctionError

object AuctionNotFound : OpenAuctionUseCaseError, PlaceBidUseCaseError, EndAuctionUseCaseError, CreateAutoBidUseCaseError, PlaceAutoBidUseCaseError, DisableAutoBidUseCaseError

object AuctionAlreadyOpened : OpenAuctionError

object AuctionHasFinished : OpenAuctionError, CreateAutoBidError

object TooEarlyToOpen : OpenAuctionError

object HighestBidHasChanged : PlaceBidError

object AuctionIsNotOpened : PlaceBidError, EndAuctionError, PlaceAutoBidError

object TooEarlyToEnd : EndAuctionError

object AutoBidAlreadyExists : CreateAutoBidError

object AutoBidNotFound : PlaceAutoBidUseCaseError, DisableAutoBidUseCaseError

object NoBidToAutoBid : PlaceAutoBidError

object AutoBidLimitReached : PlaceAutoBidError, CreateAutoBidError

object AuctionNotMatching : PlaceAutoBidError

object AutoBidIsDisabled : PlaceAutoBidError

object AutoBidAlreadyDisabled : DisableAutoBidUseCaseError
