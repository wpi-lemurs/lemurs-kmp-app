package com.lemurs.lemurs_app.data.local.passiveData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query

interface ScreentimeDAO : BaseDAO<Screentime> {
    suspend fun insertScreentimeListData(screentimeData: List<Screentime>)
    suspend fun getScreentimeData(id: String): Screentime?
    suspend fun getScreentimeID(date: String): Screentime?
    suspend fun checkDuplicate(appName: String, totalTime: Long, lastTimeUsed: String): Int
    suspend fun removeDuplicates()

    // Added by Jonathan Buwembo on 2024-06-17 to support batch deletion of screentime entries after successful API submission
    suspend fun deleteBatch(entries: List<Screentime>)
}

class ScreentimeDAOImpl : BaseDAOImpl<Screentime>(Screentime::class), ScreentimeDAO {
    override suspend fun insertScreentimeListData(screentimeData: List<Screentime>) {
        realm().write {
            for (item in screentimeData) {
                copyToRealm(item)
            }
        }
    }

    override suspend fun getScreentimeData(id: String): Screentime? {
        return realm().query<Screentime>("ID == $0", id).first().find()
    }

    override suspend fun getScreentimeID(date: String): Screentime? {
        return realm().query<Screentime>("date == $0", date).first().find()
    }

    override suspend fun checkDuplicate(appName: String, totalTime: Long, lastTimeUsed: String): Int {
        return realm().query<Screentime>(
            "appName == $0 AND totalTime == $1 AND lastTimeUsed == $2",
            appName, totalTime, lastTimeUsed
        ).count().find().toInt()
    }

    override suspend fun removeDuplicates() {
        realm().write {
            // This is not a direct translation from the Room query, as Realm does not support subqueries in this way.
            // This is a simplified approach that will remove all but the first instance of a duplicate.
            val allScreentime = query<Screentime>().find()
            val seen = mutableSetOf<String>()
            for (item in allScreentime) {
                val key = "${item.appName}-${item.totalTime}-${item.lastTimeUsed}"
                if (seen.contains(key)) {
                    findLatest(item)?.also { delete(it) }
                } else {
                    seen.add(key)
                }
            }
        }
    }

    // This method deletes a batch of Screentime entries based on their IDs.
    // It first collects the ObjectIds of the entries to delete,
    // then queries for those entries and deletes them in a single write transaction.
     override suspend fun deleteBatch(entries: List<Screentime>) {
       val idsToDelete = entries.map { it.getObjectID() }
        realm().write {
            val objectsToDelete = query<Screentime>("ID IN $0", idsToDelete).find()
            delete(objectsToDelete)
        }
     }
}
