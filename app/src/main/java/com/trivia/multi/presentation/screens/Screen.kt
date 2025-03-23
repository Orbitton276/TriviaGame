package com.trivia.multi.presentation.screens


sealed class Screen(val route: String) {
    object OnBoarding : Screen("onboarding")
    object OnBoardingStart : Screen("onboarding/start")
    object OnBoardingProfile : Screen("onboarding/profile")
    object Lobby : Screen("lobby")
    object Game : Screen("game/{roomId}") {
        fun createRoute(roomId: String): String {
            return "game/$roomId"
        }
    }
}


