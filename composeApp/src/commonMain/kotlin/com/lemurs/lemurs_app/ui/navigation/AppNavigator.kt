package com.lemurs.lemurs_app.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.State

// This file will be used as the abstract navigation logic
// for commonMain, making it KMM compatible. Reason being that commonMain has had
// many files using navController which comes from the
// androidx.navigation import that is an android specific library.

class AppNavigator( initialDestination: String) {
    private val navStack = mutableStateListOf(initialDestination)
    val currentDestination: String get() = navStack.last()

    fun navigateTo(destination: String) {
        println("AppNavigator: navigating to $destination")
        navStack.add(destination)
        println("AppNavigator: current destination is now ${navStack.last()}")
    }

    fun goBack() {
        if (navStack.size > 1) {
            navStack.removeAt(navStack.lastIndex)
            println("AppNavigator: went back, current destination is now ${navStack.last()}")
        }
    }

    fun canGoBack(): Boolean = navStack.size > 1
}