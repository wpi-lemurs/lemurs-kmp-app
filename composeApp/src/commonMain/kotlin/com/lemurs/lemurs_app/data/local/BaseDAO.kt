package com.lemurs.lemurs_app.data.local

import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.notifications.ResultsChange
import io.realm.kotlin.types.RealmObject
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

/**
 * A generic DAO interface providing basic CRUD operations.
 * @param T The RealmObject type.
 */
interface BaseDAO<T : RealmObject> {
    suspend fun insert(item: T)
    suspend fun getAll(): List<T>
    fun getAllAsFlow(): Flow<ResultsChange<T>>
    suspend fun delete(item: T): Boolean
    suspend fun deleteAll()
}

/**
 * A generic implementation of the BaseDAO interface for Realm.
 * @param clazz The KClass of the RealmObject.
 */
abstract class BaseDAOImpl<T : RealmObject>(private val clazz: KClass<T>) : BaseDAO<T> {
    protected fun realm(): Realm = RealmDatabase.getRealm()

    override suspend fun insert(item: T) {
        realm().write {
            copyToRealm(item, updatePolicy = UpdatePolicy.ALL)
        }
    }

    override suspend fun getAll(): List<T> {
        return realm().query(clazz).find()
    }

    override fun getAllAsFlow(): Flow<ResultsChange<T>> {
        return realm().query(clazz).asFlow()
    }

    override suspend fun delete(item: T): Boolean {
        return try {
            realm().write {
                findLatest(item)?.also { delete(it) }
            }
            true
        } catch (e: Exception) {
            // Log the exception to avoid unused parameter warning
            println("BaseDAOImpl.delete error: ${e.message}")
            false
        }
    }

    override suspend fun deleteAll() {
        realm().write {
            delete(this.query(clazz).find())
        }
    }
}
