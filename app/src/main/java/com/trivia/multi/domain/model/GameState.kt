package com.trivia.multi.domain.model

import com.trivia.multi.utils.Constants
import com.trivia.multi.utils.Constants.GAME_TURN_DURATION

data class GameState(
    val roomId: String = "",
//    val profiles: List<Profile> = listOf((Profile("name", "https://media.istockphoto.com/id/2161765195/photo/a-smiling-man-working-over-the-laptop-at-the-office.jpg?s=2048x2048&w=is&k=20&c=jaZ6zJyU6mTRLomQAuUIjnG9lemGaOiMsHkS__EKOCc="))),
    val profiles: List<Profile> = emptyList(),
    val currentTurn: String = "",
    val turnStartTime: Long = 0L,
    val turnDuration: Long = GAME_TURN_DURATION,
    val gameOver: Boolean = false,
    val sessionStarted: Boolean = false,
    val error: String? = null,
    val question: String = "",
    val selectedAnswer: String = "",
    val answers: List<String> = emptyList(),
    val scores: Map<String, Int> = emptyMap(),
    val correctAnswer: String = "",
    val usedQuestions: List<String> = emptyList(),
    val questionPool: List<String> = emptyList(),
    val roundsPlayed: Int = 0,
    val maxRounds: Int = Constants.MAX_ROUNDS     // Max number of rounds before game over

) {
    fun checkGameOver(): Boolean {
        // Game over conditions
        return roundsPlayed + 1 >= maxRounds || profiles.size < 2
    }
}

