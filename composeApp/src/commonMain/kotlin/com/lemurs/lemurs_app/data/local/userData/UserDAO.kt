package com.lemurs.lemurs_app.data.local.userData

import com.lemurs.lemurs_app.data.local.BaseDAO
import com.lemurs.lemurs_app.data.local.BaseDAOImpl
import io.realm.kotlin.ext.query
interface UserDAO : BaseDAO<User> {
    suspend fun getUserData(id: String): User?
    suspend fun getUserID(date: String): User?
}

class UserDAOImpl : BaseDAOImpl<User>(User::class), UserDAO {
    override suspend fun getUserData(id: String): User? {
        return realm().query<User>("ID == $0", id).first().find()
    }

    override suspend fun getUserID(date: String): User? {
        return realm().query<User>("date == $0", date).first().find()
    }
}
