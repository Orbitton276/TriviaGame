package com.trivia.multi.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.trivia.multi.domain.model.GameState
import com.trivia.multi.domain.model.GameValidationResult
import com.trivia.multi.domain.model.Profile
import com.trivia.multi.utils.Constants
import com.trivia.multi.utils.Constants.UX_DELAY
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val applicationContext: Context
) {

    // This function creates a new game room in Firestore and returns the room ID
    suspend fun createGameRoom(roomId: String, profile: Profile) {

        // Define the game state
        val gameState = GameState(
            roomId = roomId,
            profiles = listOf(profile),
            currentTurn = "",
            turnStartTime = 0L,
            turnDuration = Constants.GAME_TURN_DURATION,
            gameOver = false,
            question = "",
            answers = emptyList(),
            sessionStarted = false,
        )

        // Store in Firestore
        val roomRef = firestore.collection("rooms").document(roomId)
        roomRef.set(gameState).await()

    }

    suspend fun updateGameRoom(roomId: String, profile: Profile) {
        val roomRef = firestore.collection("rooms").document(roomId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val gameState = snapshot.toObject(GameState::class.java) ?: return@runTransaction

            val updatedProfiles = gameState.profiles.toMutableList().apply {
                if (none { p ->
                        p.id == profile.id
                    }) add(profile)
            }

            val updatedState = gameState.copy(
                profiles = updatedProfiles,
//                sessionStarted = Constants.NUM_PLAYERS == updatedPlayers.size
                sessionStarted = updatedProfiles.size > 1
            )
            Log.e("bitton", "updateGameRoom " + updatedState.toString())
            transaction.set(roomRef, updatedState)
        }
    }

    fun observeGameState(roomId: String): Flow<GameState> {
        return callbackFlow {
            val roomRef = firestore.collection("rooms").document(roomId)

            val listener = roomRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(GameState(error = e.message)) // Send error state
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // Map the Firestore document to a GameState
                    val gameState = snapshot.toObject(GameState::class.java)
                    gameState?.let { trySend(it) }  // Send updated game state to the Flow
                }
            }

            // Clean up the listener when the flow is no longer collected
            awaitClose { listener.remove() }
        }
    }

    suspend fun makeMove(roomId: String, playerId: String, selectedAnswer: String) {
        val roomRef = firestore.collection("rooms").document(roomId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val gameState = snapshot.toObject(GameState::class.java) ?: return@runTransaction
            val correctAnswer = gameState.correctAnswer
            val isCorrect = selectedAnswer == correctAnswer
            val currentScores = gameState.scores.toMutableMap()
            val updatedScore = currentScores.getOrDefault(playerId, 0) + (if (isCorrect) 1 else 0)
            currentScores[playerId] = updatedScore

            val updatedState = gameState.copy(
                scores = currentScores,
                selectedAnswer = selectedAnswer,
            )
            transaction.set(roomRef, updatedState)

        }
        delay(UX_DELAY)
        val question = fetchQuestion(roomId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val gameState = snapshot.toObject(GameState::class.java) ?: return@runTransaction


            val gameOverr = gameState.checkGameOver()
            // Validate the answer
            val rounds = gameState.roundsPlayed

            // Move to the next player (assuming two players)
            val currentTurnIndex = gameState.profiles.indexOfFirst {
                it.id == gameState.currentTurn
            }
            val nextPlayer = if (currentTurnIndex == gameState.profiles.size - 1) {
                gameState.profiles.first().id
            } else {
                gameState.profiles[currentTurnIndex + 1].id
            }

            // Update game state
            val updatedState = gameState.copy(
                currentTurn = nextPlayer ?: "",
                turnStartTime = System.currentTimeMillis(),
                question = question.question,
                answers = question.answers,
                correctAnswer = question.correctAnswer,
                selectedAnswer = "",
                gameOver = gameOverr,
                roundsPlayed = rounds + 1
            )

            // Save the updated game state
            transaction.set(roomRef, updatedState)
        }
    }

    suspend fun createQuestionPool(roomRef: DocumentReference): List<String> {
        val questionsRef = firestore.collection("questions")

        // Fetch all question IDs
        val allQuestions = try {
            questionsRef.get().await().documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }

        // Pick a random subset of NUM questions
        val questionPool =
            allQuestions.shuffled().take(Constants.MAX_ROUNDS).shuffled().toMutableSet()

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val gameState = snapshot.toObject(GameState::class.java) ?: return@runTransaction
            val updatedState = gameState.copy(
                questionPool = questionPool.toList(), usedQuestions = emptyList()
            )
            transaction.set(roomRef, updatedState)
        }.await()

        return questionPool.toList() // Return the updated question pool
    }

    suspend fun startGame(roomId: String) {
        val roomRef = firestore.collection("rooms").document(roomId)
        val questionPool = createQuestionPool(roomRef)
        val firstQuestion = fetchQuestion(roomId, questionPool)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val gameState = snapshot.toObject(GameState::class.java) ?: return@runTransaction

            if (gameState.profiles.isEmpty()) return@runTransaction
            // Reference to questions collection

            val firstPlayer = gameState.profiles.first().id ?: "" // Set first turn

            val updatedState = gameState.copy(
                currentTurn = firstPlayer,
                turnStartTime = System.currentTimeMillis(),
                sessionStarted = true, // Game officially started
                question = firstQuestion.question,
                answers = firstQuestion.answers,
                correctAnswer = firstQuestion.correctAnswer,
                gameOver = false,
                scores = emptyMap(),
                roundsPlayed = 0,
            )

            transaction.set(roomRef, updatedState)
        }
    }

    private suspend fun fetchQuestion(roomId: String, pool: List<String>? = null): GameState {

        val roomSnapshot = firestore.collection("rooms").document(roomId).get().await()

        val usedQuestions = roomSnapshot.get("usedQuestions") as? List<String> ?: emptyList()

        val questionPool = pool ?: roomSnapshot.get("questionPool") as? List<String> ?: emptyList()
        val questionID = questionPool.firstOrNull()

        questionID?.let {
            var query = firestore.collection("questions").document(questionID)

            // Fetch the question (excluding used ones if applicable)
            val questionDocument = query.get().await()

//        val questionDocument = questionQuerySnapshot.documents.firstOrNull()

            if (questionDocument != null) {
                val questionText = questionDocument.getString("question") ?: "Default Question"
                val options = questionDocument.get("options") as? List<String> ?: emptyList()
                val correctAnswer = questionDocument.getString("correctAnswer") ?: ""
                val questionId = questionDocument.id

                // Step 3: Append the new questionId to the usedQuestions list
                val updatedUsedQuestions = usedQuestions + questionId  // Append the new questionId
                val updatePool = questionPool - questionId
                // Step 4: Update Firestore with the new usedQuestions list
                val usedQuestionsRef = firestore.collection("rooms").document(roomId)

                usedQuestionsRef.update("usedQuestions", updatedUsedQuestions).await()
                usedQuestionsRef.update("questionPool", updatePool).await()

                // Step 5: Return a new GameState with the fetched question and updated usedQuestions
                return GameState(
                    question = questionText,
                    answers = options,
                    correctAnswer = correctAnswer,
                    usedQuestions = updatedUsedQuestions,
                    questionPool = updatePool
                )
            }

        }

        // In case no question was found, return a default GameState
        return GameState(
            question = "Default Question",
            answers = emptyList(),
            correctAnswer = "",
//            usedQuestions = usedQuestions
        )
    }

    fun fetchProfileFlow(): Flow<Profile> {
        return profileRepository.fetchProfile()
    }

    suspend fun saveProfile(profile: Profile) {
        return profileRepository.setProfile(profile)
    }

    suspend fun onBack(roomId: String, profile: Profile) {
        val roomRef = firestore.collection("rooms").document(roomId)


        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val gameState = snapshot.toObject(GameState::class.java) ?: return@runTransaction
            val updatedProfiles = gameState.profiles.toMutableList().apply {
                val existingProfile = find { it.id == profile.id }
                if (existingProfile != null) remove(existingProfile)
            }

            val updatedState = gameState.copy(
                profiles = updatedProfiles
            )
            transaction.set(roomRef, updatedState)

        }
    }

    suspend fun validateGame(roomId: String) : GameValidationResult {
       return checkRoomStatus(roomId) // Call your backend
    }

    private suspend fun checkRoomStatus(roomId: String): GameValidationResult {
        val roomRef = firestore.collection("rooms").document(roomId)

        // check if exists
        return try {
            val documentSnapshot = roomRef.get().await()
            val exist = documentSnapshot.exists()
            if (exist) {
                return documentSnapshot.toObject(GameState::class.java)?.let { gameState ->
                    // make sure game hasnt already started
                    if (gameState.sessionStarted) {
                        GameValidationResult.AlreadyStarted
                    } else {
                        GameValidationResult.Valid
                    }
                } ?: GameValidationResult.NotFound


            } else {
                GameValidationResult.NotFound
            }
        } catch (e: Exception) {
            println("checkRoomStatus exception ${e.message}")
            GameValidationResult.Error("Error: ${e.message ?: "Something went wrong"}")
        }
    }
}