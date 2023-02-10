package auction.infrastructure.out.db

import auction.domain.model.WithinTransaction
import com.mongodb.TransactionOptions
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient

interface MongoSessionHolder {
    fun get(): ClientSession
    fun clear()
}

class ThreadLocalMongoSessionHolder(private val mongoClient: MongoClient) : MongoSessionHolder {

    companion object {
        private val context = ThreadLocal<ClientSession?>()
    }

    override fun get(): ClientSession = context.get() ?: (mongoClient.startSession().also { context.set(it) })

    override fun clear() = context.remove()
}

class WithinMongoTransaction(private val session: MongoSessionHolder) : WithinTransaction {

    override fun <T> invoke(transactionalBlock: () -> T): T = try {
        session.get().let { client ->
            if (!client.hasActiveTransaction())
                client.use { client.withTransaction { transactionalBlock() }.also { session.clear() } }
             else transactionalBlock()
        }
    } catch (e: Exception) {
        session.clear()
        throw e
    }
}

class IsAnActiveTransaction(private val session: MongoSessionHolder) {

    operator fun invoke(): Boolean = session.get().hasActiveTransaction()
}
