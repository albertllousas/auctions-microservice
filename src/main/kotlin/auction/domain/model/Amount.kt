package auction.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

data class Amount(val value: BigDecimal) {

    companion object {

        fun create(amount: BigDecimal): Either<TooLowAmount, Amount> =
            if (amount <= ZERO) TooLowAmount.left()
            else Amount(amount).right()

        fun create(amount: BigDecimal, min: BigDecimal): Either<TooLowAmount, Amount> =
            if (amount < min) TooLowAmount.left()
            else Amount(amount).right()
    }
}
