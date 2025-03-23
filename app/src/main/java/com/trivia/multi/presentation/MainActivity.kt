package com.trivia.multi.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.trivia.multi.presentation.screens.GameScreen
import com.trivia.multi.presentation.screens.LobbyScreen
import com.trivia.multi.presentation.screens.OnBoarding
import com.trivia.multi.presentation.screens.OnBoardingProfile
import com.trivia.multi.presentation.screens.Screen
import com.trivia.multi.presentation.viewmodel.GameViewModel
import com.trivia.multi.presentation.viewmodel.OnBoardingViewModel
import com.trivia.multi.ui.theme.TriviaMultiTheme
import com.trivia.multi.utils.Common.Brush
import com.trivia.multi.utils.Common.GameLoadingAnimation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG: String = "mainA"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: ")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TriviaMultiTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                GameNavHost(intent/*, Modifier.padding(innerPadding)*/)
//                }
            }
        }

    }


}

@Composable
private fun handleDeepLink(
    intent: Intent?,
    viewModel: GameViewModel,
    navController: NavHostController
) {
    intent?.data?.let { uri ->
        // Extract the roomId from the URI
        val roomId = uri.lastPathSegment
        if (roomId != null) {
            // Use the roomId to navigate to the correct screen
            navController.navigate(Screen.Game.createRoute(roomId))
        }
    }
}


@Composable
fun GameNavHost(
    intent: Intent?,
    navController: NavHostController = rememberNavController()
) {
    val viewModel: GameViewModel = hiltViewModel()
    val onBoardingViewModel = hiltViewModel<OnBoardingViewModel>()

    var startDest by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val profile = viewModel.profile.first {
            it.isFetched
        } // Waits for first valid profile
        startDest = if (profile.id.isNullOrEmpty()) Screen.OnBoarding.route else Screen.Lobby.route
    }

    if (startDest == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush()), contentAlignment = Alignment.Center
        ) {
            GameLoadingAnimation()
        }
    } else {
        NavHost(
            navController = navController,
            startDestination = startDest!!
        ) {

            navigation(
                route = Screen.OnBoarding.route,
                startDestination = Screen.OnBoardingStart.route
            ) {
                composable(Screen.OnBoardingStart.route) {
                    OnBoarding(navController)
                }
                composable(Screen.OnBoardingProfile.route) {
                    OnBoardingProfile(
                        navController,
                        onBoardingViewModel
                    )
                }
            }


            composable(Screen.Lobby.route) {
                LobbyScreen(viewModel, navController, { roomId ->
                    // Navigate to game screen
                    navController.navigate(Screen.Game.createRoute(roomId))
                })
            }

            composable(
                Screen.Game.route,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink {
                    uriPattern = "com.trivia.multi://game/{roomId}"
                }) // Deep link setup
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                GameScreen(roomId, viewModel, navController)
            }
        }

        intent?.let {
            handleDeepLink(intent, viewModel, navController)
        }
    }

}