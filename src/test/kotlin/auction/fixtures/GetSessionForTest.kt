package auction.fixtures

import auction.infrastructure.out.db.MongoSessionHolder
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient

class MongoSessionHolderForTest(private val mongoClient: MongoClient) : MongoSessionHolder {
    override fun get(): ClientSession = mongoClient.startSession()

    override fun clear() = Unit
}
