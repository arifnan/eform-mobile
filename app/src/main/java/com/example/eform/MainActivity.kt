package com.example.eform

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.eform.navigation.AppNavHost
import com.example.eform.navigation.Screen
import com.example.eform.ui.theme.EformTheme
import com.example.eform.utils.Constants
import com.example.eform.utils.NotificationHelper

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var loggedInUserIdentifier: String? = null // Store logged-in user identifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(applicationContext) // Create notification channel

        setContent {
            EformTheme {
                navController = rememberNavController()

                AppNavHost(
                    navController = navController,
                    onLoginSuccess = { identifier -> // Callback to store identifier after login
                        loggedInUserIdentifier = identifier
                    }
                )
            }
        }
        // Handle intents that might open the app (deep link or notification)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the current intent of the activity
        // Handle new intent if the app is already running
        handleIntent(intent)
    }

    /**
     * Central function to handle all types of intents (deep link & notification).
     * This is called in onCreate and onNewIntent.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent == null || !::navController.isInitialized) {
            return
        }

        // Prioritize deep link handling. If the intent is a deep link,
        // handleDeepLink will return true and we don't need to continue.
        val isDeepLinkHandled = handleDeepLink(intent)

        if (!isDeepLinkHandled) {
            // If it's not a deep link, try to handle it as a notification intent.
            handleNotificationIntent(intent)
        }
    }

    /**
     * Revises the logic to only parse the deep link and navigate
     * to the unified login screen, carrying the 'formCode'. The subsequent logic
     * (after successful login) will be handled by LoginScreen.
     *
     * @return True if the intent was successfully handled as a deep link, false otherwise.
     */
    private fun handleDeepLink(intent: Intent): Boolean {
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            // Check if URI matches the app's deep link scheme
            if (uri != null && uri.scheme == Constants.APP_DEEP_LINK_SCHEME && uri.host == Constants.APP_DEEP_LINK_HOST) {
                // Get the last segment of the path, which is the form code (e.g., "BRZNXUCO")
                val formCode = uri.lastPathSegment
                if (!formCode.isNullOrEmpty()) {
                    // Directly navigate to the unified Login screen and include the formCode as an argument.
                    navController.navigate(Screen.Login.createRoute(formCode)) {
                        // Clear all previous backstack so the user cannot return to the initial screen
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                    // Clear the intent after handling so it's not processed again when the activity is recreated (e.g., screen rotation)
                    setIntent(Intent())
                    return true // Indicates deep link has been handled
                }
            }
        }
        return false // Not a valid deep link
    }

    /**
     * Logic to handle intent from notification. This logic is kept
     * as before without changes.
     */
    private fun handleNotificationIntent(intent: Intent) {
        if (intent.hasExtra("target_screen")) {
            val targetScreen = intent.getStringExtra("target_screen")
            // Check if the target is the notification page
            if (targetScreen == Screen.Notification.route) { // This is "notification/{userIdentifier}"
                if (loggedInUserIdentifier != null) {
                    // If user is logged in, open notification page
                    navController.navigate(
                        Screen.Notification.route.replace("{userIdentifier}", loggedInUserIdentifier!!)
                    ) {
                        launchSingleTop = true
                    }
                } else {
                    // If no user is logged in, navigate to the unified login page
                    navController.navigate(Screen.Login.createRoute()) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                    Toast.makeText(this, "Please log in to see notifications", Toast.LENGTH_SHORT).show()
                }
                // Remove extra from intent so it's not processed repeatedly
                intent.removeExtra("target_screen")
            }
        }
    }
}