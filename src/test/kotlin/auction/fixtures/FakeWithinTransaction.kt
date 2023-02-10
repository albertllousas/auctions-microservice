package auction.fixtures

import auction.domain.model.WithinTransaction

object FakeWithinTransaction : WithinTransaction {
    override fun <T> invoke(transactionalBlock: () -> T): T = transactionalBlock()

}