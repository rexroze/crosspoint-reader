package com.crosspoint.reader.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crosspoint.reader.ui.home.HomeScreen
import com.crosspoint.reader.ui.library.LibraryScreen
import com.crosspoint.reader.ui.opds.OpdsScreen
import com.crosspoint.reader.ui.settings.KOReaderSyncScreen
import com.crosspoint.reader.ui.settings.SettingsScreen
import java.util.Base64

@Composable
fun NavGraph(openUri: Uri? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateToOpds = { navController.navigate(Screen.Opds.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                openUri = openUri
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Opds.route) {
            OpdsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCatalog = { url ->
                    val encoded = Base64.getUrlEncoder().encodeToString(url.toByteArray())
                    navController.navigate("opds_catalog/$encoded")
                }
            )
        }

        composable(
            route = Screen.OpdsCatalog.ROUTE,
            arguments = listOf(navArgument(Screen.OpdsCatalog.ARG_URL) { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString(Screen.OpdsCatalog.ARG_URL) ?: ""
            val url = String(Base64.getUrlDecoder().decode(encodedUrl))
            OpdsScreen(
                rootUrl = url,
                onBack = { navController.popBackStack() },
                onNavigateToCatalog = { feedUrl ->
                    val encoded = Base64.getUrlEncoder().encodeToString(feedUrl.toByteArray())
                    navController.navigate("opds_catalog/$encoded")
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToKOReaderSync = { navController.navigate(Screen.KOReaderSync.route) }
            )
        }

        composable(Screen.KOReaderSync.route) {
            KOReaderSyncScreen(onBack = { navController.popBackStack() })
        }
    }
}
