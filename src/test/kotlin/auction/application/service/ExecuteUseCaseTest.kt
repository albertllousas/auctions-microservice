package auction.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auction.domain.model.AuctionCreated
import auction.domain.model.DomainError
import auction.domain.model.DomainEvent
import auction.domain.model.PublishEvent
import auction.domain.model.ReportCrash
import auction.domain.model.ReportError
import auction.domain.model.UserNotFound
import auction.fixtures.Builders
import auction.fixtures.FakeWithinTransaction
import io.kotest.assertions.throwables.shouldThrowAny
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ExecuteUseCaseTest {

    private val publishEvent = mockk<PublishEvent>(relaxed = true)

    private val reportError = mockk<ReportError>(relaxed = true)

    private val reportCrash = mockk<ReportCrash>(relaxed = true)

    private val executeUseCase = UseCaseExecutionBuilder(
        FakeWithinTransaction, publishEvent, reportError, reportCrash
    ).build(this::class)

    @Test
    fun `should publish an event when usecase is executed successfully`() {
        val event = AuctionCreated(Builders.buildAuction())

        executeUseCase { event.right() }

        verify { publishEvent(event) }
    }

    @Test
    fun `should report an error when usecase fails with a domain error`() {

        val userNotFound: Either<UserNotFound, AuctionCreated> = UserNotFound.left()

        executeUseCase { userNotFound }

        verify { reportError(UserNotFound, ExecuteUseCaseTest::class) }
    }

    @Test
    fun `should report a crash when usecase crash with an exception`() {
        val boom = Exception("Boom!")

        shouldThrowAny { executeUseCase<DomainError> { throw boom } }

        verify { reportCrash(boom, ExecuteUseCaseTest::class) }
    }
}