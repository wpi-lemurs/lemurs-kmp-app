package com.lemurs.lemurs_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lemurs.lemurs_app.data.api.MicrosoftApiAuthorizationService
import com.lemurs.lemurs_app.data.api.WebAPIAuthorizationService
import com.lemurs.lemurs_app.ui.screens.AudioScreen
import com.lemurs.lemurs_app.ui.screens.DailyInformationScreen
import com.lemurs.lemurs_app.ui.screens.DailyQuestionsScreen
import com.lemurs.lemurs_app.ui.screens.DemographicsScreen
import com.lemurs.lemurs_app.ui.screens.HealthScreen
import com.lemurs.lemurs_app.ui.screens.LemurScreen
import com.lemurs.lemurs_app.ui.screens.LemursTopBar
import com.lemurs.lemurs_app.ui.screens.LoginScreen
import com.lemurs.lemurs_app.ui.screens.MainScreen
import com.lemurs.lemurs_app.ui.screens.ProfileScreen
import com.lemurs.lemurs_app.ui.screens.ProgressHistoryScreen
import com.lemurs.lemurs_app.ui.screens.ProgressScreen
import com.lemurs.lemurs_app.ui.screens.ResourcesScreen
import com.lemurs.lemurs_app.ui.screens.SubmissionScreen
import com.lemurs.lemurs_app.ui.screens.SurveyInformationScreen
import com.lemurs.lemurs_app.ui.screens.SurveyScreen
import com.lemurs.lemurs_app.ui.screens.WeeklyInformationScreen
import com.lemurs.lemurs_app.ui.screens.WeeklyQuestionsScreen
import com.lemurs.lemurs_app.ui.screens.WritingScreen
import com.lemurs.lemurs_app.ui.navigation.AppNavigator
import com.lemurs.lemurs_app.ui.theme.LemursAppTheme
import com.lemurs.lemurs_app.ui.viewmodel.DemographicsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.lemurs.lemurs_app.util.UriOpener

val web = WebAPIAuthorizationService()
val microsoftService = MicrosoftApiAuthorizationService(web)

/*
* This is the main entry point for the multiplatform application.
*  Both platforms will call this function to start the app
*/
@Composable
fun App() {
    val navigator = remember { AppNavigator(LemurScreen.Login.name) }
    App(navigator)
}
@Composable
fun App(navigator: AppNavigator) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LemursAppTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = navigator.currentDestination == LemurScreen.Main.name,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(280.dp)
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LEMURS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resources item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                navigator.navigateTo(LemurScreen.Resources.name)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Resources",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Resources",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    HorizontalDivider()

                    // Logout item
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { drawerState.close() }
                                microsoftService.removeAccount()
                                navigator.navigateTo(LemurScreen.Login.name)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        ) {
            // Main content
            Scaffold(
                topBar = {
                    // Determine the current screen based on the navigator's current destination
                    val currentScreen = LemurScreen.entries.find { it.name == navigator.currentDestination }
                        ?: error("Unknown route: ${navigator.currentDestination}")

                    LemursTopBar(
                        currentScreen = currentScreen,
                        canNavigateBack = navigator.canGoBack(),
                        onNavigateBack = navigator::goBack,
                        onNavigateTo = navigator::navigateTo,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    NavigationComponent(
                        navigator = navigator,
                        currentRoute = navigator.currentDestination,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun NavigationComponent(
    navigator: AppNavigator,
    currentRoute: String,
    modifier: Modifier = Modifier
) {
    val viewModel = koinInject<DemographicsViewModel>()
    val uriOpener = koinInject<UriOpener>()

    // Debug logging
    println("NavigationComponent: currentRoute = $currentRoute")

    when (currentRoute) {
        LemurScreen.Main.name -> MainScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Login.name -> LoginScreen(onNavigateTo = navigator::navigateTo, microsoftService = microsoftService, uriOpener = uriOpener)
        LemurScreen.Survey.name -> SurveyScreen(onNavigateTo = navigator::navigateTo, navigator = navigator)
        LemurScreen.Profile.name -> ProfileScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Progress.name -> ProgressScreen(onNavigateTo = navigator::navigateTo, totalEarnings = "$32.50", dailyProgress = 7, weeklyProgress = 2)
        LemurScreen.History.name -> ProgressHistoryScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Information.name -> SurveyInformationScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Demographics.name -> DemographicsScreen(viewModel, onNavigateTo = navigator::navigateTo)
        LemurScreen.Daily.name -> DailyQuestionsScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Weekly.name -> WeeklyQuestionsScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Writing.name -> WritingScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Audio.name -> AudioScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.Submission.name -> SubmissionScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.HealthConnect.name -> HealthScreen(onNavigateTo = navigator::navigateTo)
        LemurScreen.DailyInformation.name -> DailyInformationScreen(onNavigateTo = navigator::navigateTo, onBack = navigator::goBack, uriOpener = uriOpener)
        LemurScreen.WeeklyInformation.name -> WeeklyInformationScreen(onNavigateTo = navigator::navigateTo, onBack = navigator::goBack, uriOpener = uriOpener)
        LemurScreen.Resources.name -> ResourcesScreen(onNavigateTo = navigator::navigateTo, uriOpener)

        else -> LoginScreen(onNavigateTo = navigator::navigateTo, microsoftService = microsoftService, uriOpener = uriOpener)
    }
}
