package com.lemurs.lemurs_app.data.local

import com.lemurs.lemurs_app.data.local.activeData.Audio
import com.lemurs.lemurs_app.data.local.activeData.SurveyResponse
import com.lemurs.lemurs_app.data.local.activeData.Written
import com.lemurs.lemurs_app.data.local.passiveData.Bluetooth
import com.lemurs.lemurs_app.data.local.passiveData.Calorie
import com.lemurs.lemurs_app.data.local.passiveData.Distance
import com.lemurs.lemurs_app.data.local.passiveData.GPS
import com.lemurs.lemurs_app.data.local.passiveData.Health
import com.lemurs.lemurs_app.data.local.passiveData.Screentime
import com.lemurs.lemurs_app.data.local.passiveData.Sleep
import com.lemurs.lemurs_app.data.local.passiveData.Speed
import com.lemurs.lemurs_app.data.local.passiveData.Step
import com.lemurs.lemurs_app.data.local.passiveData.TikTok
import com.lemurs.lemurs_app.data.local.passiveData.Weight
import com.lemurs.lemurs_app.data.local.userData.User
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

object RealmDatabase {
    private val config by lazy {
        RealmConfiguration.create(schema = setOf(
            Audio::class,
            SurveyResponse::class,
            Written::class,
            Bluetooth::class,
            GPS::class,
            Step::class,
            Calorie::class,
            Distance::class,
            Health::class,
            Speed::class,
            Sleep::class,
            Weight::class,
            Screentime::class,
            TikTok::class,
            User::class
        ))
    }

    // Lazily open the realm when first requested.
    private val _realm: Realm by lazy { Realm.open(config) }

    fun getRealm(): Realm = _realm
}
