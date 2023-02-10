package auction.application.service

import arrow.core.Either
import auction.domain.model.DomainError
import auction.domain.model.DomainEvent
import auction.domain.model.PublishEvent
import auction.domain.model.ReportCrash
import auction.domain.model.ReportError
import auction.domain.model.WithinTransaction
import kotlin.reflect.KClass

interface ExecuteUseCase  {
    operator fun <E: DomainError> invoke(block: () -> Either<E, DomainEvent>) : Either<E, Unit>
}

class UseCaseExecutionBuilder(
    private val withinTransaction: WithinTransaction,
    private val publishEvent: PublishEvent,
    private val reportError: ReportError,
    private val reportCrash: ReportCrash
) {
    fun build(originator: KClass<*>): ExecuteUseCase = object : ExecuteUseCase {

        override fun <E: DomainError> invoke(block: () -> Either<E, DomainEvent>): Either<E, Unit> = try {
            withinTransaction {
                block()
                    .map { publishEvent(it) }
                    .tapLeft { reportError(it, originator) }
            }
        } catch (exception: Exception) {
            reportCrash(exception, originator)
            throw exception
        }
    }
}
