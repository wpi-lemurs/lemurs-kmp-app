package com.lemurs.lemurs_app.data.api

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import co.touchlab.kermit.Logger
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import com.lemurs.lemurs_app.data.AndroidActivityProvider
import com.lemurs.lemurs_app.data.AndroidContextProvider
import com.lemurs.lemurs_app.data.datastore.JwtTokenResponseImpl
import com.lemurs.lemurs_app.R
import com.lemurs.lemurs_app.ui.screens.LemurScreen
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.lemurs.lemurs_app.survey.Demographic
import com.lemurs.lemurs_app.survey.fetchDemographic

actual class MicrosoftApiAuthorizationService actual constructor(
    private var webApiService: WebAPIAuthorizationService

) : KoinComponent {

    //    val context: Context = requireNotNull(org.koin.core.context.GlobalContext.get().get())
    val context: Context = requireNotNull(AndroidContextProvider.context)
    val logger = Logger.withTag("MSAuth")
    private var authClient: ISingleAccountPublicClientApplication? = null
    private var mAccount: IAccount? = null
    private var microsoftAccessToken: String = ""
    val jwtTokenResponseImpl: JwtTokenResponseImpl by inject()
//    lateinit var navController: NavHostController
    private lateinit var onNavigate: (String) -> Unit
    private val scopes : String = "api://b00e7cc0-f93d-4caf-9f9c-c97d8d6f6a0d/lemurs"

    /**
     * initializes the authentication client and loads the microsoft account
     * loading account attempts to silently acquire access token if user is still logged in
     *
     * returns true if user is already logged in
     */
    actual fun initClient(navigate: (String) -> Unit): Boolean {
        onNavigate = navigate

        var loggedIn = false
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            R.raw.auth_config_claim_auth_android,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    authClient = application
                    loggedIn = loadAccount()
                }

                override fun onError(exception: MsalException) {
                    logger.w("Failed to create auth client: $exception")
                }
            }
        )
        return loggedIn // NOTE: still returns false immediately, since async init
    }

    /**
     * checks if user is still logged in and attempts to silently acquire access token
     *
     * returns true if user is already logged in and successfully acquired token
     */
    private fun loadAccount(): Boolean {
        var loggedIn = false
        logger.w("loading account")
        if (authClient == null) {
            logger.w("client null, account not loaded")
            return loggedIn
        }
        authClient!!.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                mAccount = activeAccount
                logger.w("account loaded")
                loggedIn = silentlyAcquireToken()
                return
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                if (currentAccount == null) {
                    // Perform a cleanup task as the signed-in account changed.
                    mAccount = null
                    logger.w("Signed Out.")
                    return
                }
            }

            override fun onError(exception: MsalException) {
                logger.w(exception.toString())
                return
            }
        })
        return loggedIn
    }

    fun checkDemographicsEmpty(): Boolean {
        var demographics : List<Demographic> = emptyList()
        runBlocking {
            demographics = fetchDemographic()
        }
        logger.w("fetched demographics: " + demographics.toString() )
        return demographics.isEmpty()
    }

    /*
    returns true if acct already logged in and token already acquired
     */
    fun silentlyAcquireToken(): Boolean {
        val scopesList = scopes.lowercase().split(" ")
        if (mAccount != null) {
            logger.w("An account is already signed in, acquiring token silently")
            val silentParameters = AcquireTokenSilentParameters.Builder()
                .fromAuthority(mAccount!!.authority)
                .forAccount(mAccount)
                .withScopes(scopesList)
                .withCallback(authSilentCallback)
                .build()
            try {
                authClient!!.acquireTokenSilentAsync(silentParameters)
            } catch (e: Exception) {
                logger.w("failed to silently acquire token: " + e.toString())
            }
            return true
        }
        return false
    }

    /**
     * interactively acquires token by creating pop up for user to log in
     * called by log in button
     */
    actual fun acquireToken() {
        val scopesList = scopes.lowercase().split(" ")

        if (authClient == null) {
            logger.w("authClient is null, cannot acquire token. Make sure initClient has completed initialization.")
            return
        }

        if (mAccount != null) {
            silentlyAcquireToken()
            return
        }

        val activity = AndroidActivityProvider.activity
        if (activity == null) {
            logger.w("Activity is null, cannot acquire token interactively")
            return
        }

        logger.w("acquiring token interactively")
        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .forAccount(mAccount)
            .withScopes(scopesList)
            .withCallback(getAuthInteractiveCallback())
            .build()

        try {
            authClient!!.acquireToken(parameters)
        } catch (e: Exception) {
            logger.w("failed to interactively acquire token: " + e.toString())
        }
        return
    }

    /**
     * call back once user interactively logs in through pop-up
     */
    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        logger.w("authentication callback...")
        return object : AuthenticationCallback {

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, use it to call a protected resource - Web API */
                logger.w("Successfully interactively authenticated")

                mAccount = authenticationResult.account

                /* Reload account asynchronously to get the up-to-date list. */
                microsoftAccessToken = authenticationResult.accessToken
                logger.w("Microsoft Access token: " + microsoftAccessToken)
                runBlocking {
                    val jwtTokenResponseJson =
                        webApiService.accessWebApi(microsoftAccessToken)
                    jwtTokenResponseImpl.updateLemursAccessToken(
                        jwtTokenResponseJson.jsonObject.get(
                            "accessToken"
                        )!!.jsonPrimitive.content
                    )
                    jwtTokenResponseImpl.updateRefreshToken(
                        jwtTokenResponseJson.jsonObject.get(
                            "refreshToken"
                        )!!.jsonPrimitive.content
                    )
                }
                logger.w("navigating out of login")
                if (!checkDemographicsEmpty()) {
                    logger.w("navigating to home")
                    onNavigate(LemurScreen.Main.name)
                } else {
                    logger.w("navigating to info page")
                    onNavigate(LemurScreen.Information.name)
                }
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                logger.w("Authentication failed: $exception")
                microsoftAccessToken = ""
                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            override fun onCancel() {
                /* User canceled the authentication */
                logger.w("User cancelled login.")
            }
        }
    }

    /**
     * Callback used for silent acquireToken calls
     */
    private val authSilentCallback: SilentAuthenticationCallback
        private get() = object : SilentAuthenticationCallback {

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                logger.w("Successfully silently authenticated")

                microsoftAccessToken = authenticationResult.accessToken
                logger.w("Access token: " + microsoftAccessToken)
                runBlocking {
                    val jwtTokenResponseJson =
                        webApiService.accessWebApi(microsoftAccessToken)
                    jwtTokenResponseImpl.updateLemursAccessToken(
                        jwtTokenResponseJson.jsonObject.get(
                            "accessToken"
                        )!!.jsonPrimitive.content
                    )
                    jwtTokenResponseImpl.updateRefreshToken(
                        jwtTokenResponseJson.jsonObject.get(
                            "refreshToken"
                        )!!.jsonPrimitive.content
                    )
                }
                logger.w("navigating out of login")
                if (!checkDemographicsEmpty()) {
                    logger.w("navigating to home")
                    onNavigate(LemurScreen.Main.name)
                } else {
                    logger.w("navigating to info page")
                    onNavigate(LemurScreen.Information.name)
                }
            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                logger.w("Authentication failed: $exception")
                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception is MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                }
            }
        }

    /**
     * for logging user out
     * called by log out button
     */
    actual fun removeAccount() {
        authClient!!.signOut(signOutCallback())
    }

    /**
     * call back to log user out
     */
    private fun signOutCallback(): ISingleAccountPublicClientApplication.SignOutCallback {
        return object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                mAccount = null
                loadAccount()
                runBlocking {
                    jwtTokenResponseImpl.updateLemursAccessToken("")
                    jwtTokenResponseImpl.updateRefreshToken("")
                }
                onNavigate(LemurScreen.Login.name)
            }

            override fun onError(exception: MsalException) {
                logger.w("Exception: " + exception)
            }
        }
    }
}