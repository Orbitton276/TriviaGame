package com.trivia.multi.domain.model

sealed class GameIntent {
    data class ValidateRoom(val roomId: String) : GameIntent()
    data class JoinRoom(val roomId: String) : GameIntent()
    data class MakeMove(val roomId: String, val playerId: String, val action: String) : GameIntent()
    data class ObserveGameState(val roomId: String) : GameIntent() // Observe game state for a room
    data class CreateGameRoom(val onGameStart: (String) -> Unit) : GameIntent() // Intent to create a game room
}


