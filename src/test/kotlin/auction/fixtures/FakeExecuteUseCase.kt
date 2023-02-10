package auction.fixtures

import arrow.core.Either
import auction.application.service.ExecuteUseCase
import auction.domain.model.DomainError
import auction.domain.model.DomainEvent

object FakeExecuteUseCase : ExecuteUseCase {
    override fun <E : DomainError> invoke(block: () -> Either<E, DomainEvent>): Either<E, Unit> = block().map {  }
}