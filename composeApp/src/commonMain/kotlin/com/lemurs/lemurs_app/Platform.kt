package com.lemurs.lemurs_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform