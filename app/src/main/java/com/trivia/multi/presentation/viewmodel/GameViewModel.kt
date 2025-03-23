package com.trivia.multi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trivia.multi.data.repository.GameRepository
import com.trivia.multi.domain.model.GameIntent
import com.trivia.multi.domain.model.GameState
import com.trivia.multi.domain.model.GameValidationResult
import com.trivia.multi.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    val profile = gameRepository.fetchProfileFlow()
        .onEach {
            println("fetched profile ${it.toString()}")
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Profile())
//    init {
//        viewModelScope.launch {
//            profile.collectLatest {
//                println("🔄 Profile updated inside ViewModel: $it")
//            }
//        }
//    }
    private val _joinRoomValidate = MutableSharedFlow<GameValidationResult>()
    val joinRoomValidate: SharedFlow<GameValidationResult> = _joinRoomValidate.asSharedFlow()

    private var gameJob: Job? = null

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.ValidateRoom -> validateRoom(intent.roomId)
            is GameIntent.JoinRoom -> joinRoom(intent.roomId)
            is GameIntent.MakeMove -> makeMove(intent.roomId, intent.playerId, intent.action)
            is GameIntent.ObserveGameState -> observeGameState(intent.roomId)
            is GameIntent.CreateGameRoom -> createGameRoom(intent.onGameStart) // Handle creating a new game room
        }
    }

    private fun validateRoom(roomId: String) {
        viewModelScope.launch {

            when (val result = gameRepository.validateGame(roomId)) {
                GameValidationResult.AlreadyStarted -> {
                    _joinRoomValidate.emit(result)
                }

                GameValidationResult.NotFound -> {
                    _joinRoomValidate.emit(result)
                }

                GameValidationResult.Valid -> {
                    _joinRoomValidate.emit(result)
                }

                is GameValidationResult.Error -> {
                    _joinRoomValidate.emit(result)
                }
            }
            // _joinRoomValidate.emit(true)
        }
    }

    private fun observeGameState(roomId: String) {
        // Cancel any existing job to avoid multiple observers
        gameJob?.cancel()

        gameJob = viewModelScope.launch {
            gameRepository.observeGameState(roomId).collect { newState ->
                _state.value = newState // Update state with the latest game state from Firebase
            }
        }
    }

    private fun joinRoom(roomId: String) {
        _state.value = _state.value.copy(roomId = roomId)

        viewModelScope.launch {
            gameRepository.updateGameRoom(roomId, profile.value)

            gameRepository.observeGameState(roomId).collect { newState ->
                _state.value = newState
            }
        }
    }

    private fun makeMove(roomId: String, playerId: String, action: String) {
        viewModelScope.launch {
            gameRepository.makeMove(roomId, playerId, action)
        }
    }

    private fun createGameRoom(onGameStart: (String) -> Unit) {

        viewModelScope.launch {
            val roomId =
                UUID.randomUUID().toString().take(6) // Generate a short random ID
            withContext(Dispatchers.Main) {
                onGameStart(roomId)
            }

            gameRepository.createGameRoom(
                roomId,
                profile.value
            ) // Create the room in the repository
            _state.value =
                _state.value.copy(roomId = roomId) // Update the state with the new room ID

        }
    }

    fun gameStarted() {
        viewModelScope.launch {
            gameRepository.startGame(_state.value.roomId)
        }
    }

    fun onBack() {
        viewModelScope.launch {
            gameRepository.onBack(_state.value.roomId, profile.value)
        }
    }

    private fun shortenWithTinyURL(originalUrl: String, callback: (String) -> Unit) {
        val encodedUrl = URLEncoder.encode(originalUrl, "UTF-8")
        val apiUrl = "https://tinyurl.com/api-create.php?url=$encodedUrl"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = URL(apiUrl).readText()
                withContext(Dispatchers.Main) {
                    callback(response) // Return the shortened URL
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(originalUrl) // Fallback to original link if API fails
                }
            }
        }
    }

    fun shareGameLink(roomId: String, callback: (String) -> Unit) {
        val deepLink = "trivia://game/$roomId"
        shortenWithTinyURL(deepLink, callback)
    }
}


