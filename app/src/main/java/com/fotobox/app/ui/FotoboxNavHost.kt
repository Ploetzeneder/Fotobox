package com.fotobox.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fotobox.app.ui.screens.AudioGuestbookScreen
import com.fotobox.app.ui.screens.CameraScreen
import com.fotobox.app.ui.screens.ConnectScreen
import com.fotobox.app.ui.screens.GalleryScreen
import com.fotobox.app.ui.screens.HomeScreen
import com.fotobox.app.ui.screens.PhotoReviewScreen
import com.fotobox.app.ui.screens.SettingsScreen
import com.fotobox.app.ui.screens.SlideShowScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object PhotoReview : Screen("review/{sessionId}") {
        fun createRoute(sessionId: Long) = "review/$sessionId"
    }
    data object Gallery : Screen("gallery")
    data object Settings : Screen("settings")
    data object SlideShow : Screen("slideshow")
    data object AudioGuestbook : Screen("audio_guestbook")
    data object Connect : Screen("connect")
}

@Composable
fun FotoboxNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartCamera = { navController.navigate(Screen.Camera.route) },
                onOpenGallery = { navController.navigate(Screen.Gallery.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onStartSlideShow = { navController.navigate(Screen.SlideShow.route) },
                onOpenAudioGuestbook = { navController.navigate(Screen.AudioGuestbook.route) }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotosTaken = { sessionId ->
                    navController.navigate(Screen.PhotoReview.createRoute(sessionId)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PhotoReview.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
            PhotoReviewScreen(
                sessionId = sessionId,
                onRetake = {
                    navController.navigate(Screen.Camera.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Gallery.route) {
            GalleryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenConnect = { navController.navigate(Screen.Connect.route) }
            )
        }

        composable(Screen.SlideShow.route) {
            SlideShowScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AudioGuestbook.route) {
            AudioGuestbookScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Connect.route) {
            ConnectScreen(onBack = { navController.popBackStack() })
        }
    }
}
