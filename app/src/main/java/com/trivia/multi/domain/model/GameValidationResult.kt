package com.trivia.multi.domain.model

sealed class GameValidationResult {
    object Valid : GameValidationResult()  // Room is valid, user can join
    object AlreadyStarted : GameValidationResult()  // Game has already started
    object NotFound : GameValidationResult()  // Room does not exist
    data class Error(val error: String) : GameValidationResult()  // Generic error
}