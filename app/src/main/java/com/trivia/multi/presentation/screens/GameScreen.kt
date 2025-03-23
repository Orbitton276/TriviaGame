package com.trivia.multi.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.trivia.multi.R
import com.trivia.multi.domain.model.GameIntent
import com.trivia.multi.domain.model.GameState
import com.trivia.multi.presentation.viewmodel.GameViewModel
import com.trivia.multi.utils.Common.Brush
import com.trivia.multi.utils.Common.GameButton
import com.trivia.multi.utils.Common.GameListText
import com.trivia.multi.utils.Common.GameLoadingAnimation
import com.trivia.multi.utils.Common.GameQuestionButton
import com.trivia.multi.utils.Common.GameText
import com.trivia.multi.utils.Common.GameTitle
import com.trivia.multi.utils.Constants
import com.trivia.multi.utils.Constants.UX_DELAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun GameScreen(roomId: String, viewModel: GameViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsState()
    BackHandler {
        viewModel.onBack()
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(GameIntent.JoinRoom(roomId))
        // Observe Firestore updates when entering the screen
        viewModel.onIntent(GameIntent.ObserveGameState(roomId))
    }
    Scaffold(
        topBar = { BuildTopBar() },
        modifier = Modifier.fillMaxSize()
    ) { contentPadding ->
        Box( // Ensures background is applied properly
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush())
                .padding(contentPadding) // Respect Scaffold's content padding
        ) {
            if (state.sessionStarted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Room info
                    GameText("Room: $roomId", fontSize = 22.sp)

                    val gameStarted = state.currentTurn.isNotEmpty()

                    if (!gameStarted) {
                        // Start button (if game hasn't started)
                        GameLobby(state, viewModel)
                    } else if (state.gameOver) {
                        GameOver(state, viewModel, navController)
                    } else {
                        GameBoard(state, viewModel, roomId)
                    }
                }
            } else {
                WaitingRoom(roomId, navController, viewModel)
            }
        }
    }


}


@Composable
fun GameLobby(gameState: GameState = GameState(), viewModel: GameViewModel) {
    Column(
        Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameTitle("Game lobby")
            FinalScoreTable(gameState)
        }
        if (gameState.profiles.size > 1) {
            GameButton(
                "start",
                onClick = { viewModel.gameStarted() })
        }
    }

}

@Composable
fun FinalScoreTable(gameState: GameState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {

        gameState.profiles.forEach { profile ->
//        items(gameState.profiles) { profile ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = profile.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop,
                )
                GameListText(
                    text = profile.name,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                GameListText(
                    text = (gameState.scores[profile.id] ?: 0).toString(),
                    fontSize = 16.sp
                )
            }

        }
    }
}

@Composable
fun GameBoard(state: GameState, viewModel: GameViewModel, roomId: String) {
    val profile by viewModel.profile.collectAsState()
//    var currentProfilePlaying by remember { mutableStateOf<Profile?>(null) }
    val currentProfilePlaying = state.profiles.find { it.id == state.currentTurn }

    val isMyTurn = profile.id == state.currentTurn
    var remainingTime by remember { mutableLongStateOf(0L) }
    var timeIsUp by remember { mutableStateOf(false) } // Prevents overlapping execution


    println("current turn: ${state.currentTurn} profileId = ${profile.id}")
    LaunchedEffect(state.currentTurn, state.turnStartTime, state.profiles) {

        // Get the time difference between the local device's current time and the turn start time.
        val timeDifference = System.currentTimeMillis() - state.turnStartTime
        timeIsUp = false
        while (true) {
            // Calculate remaining time considering time difference
            val newTime = max(
                0,
                (state.turnStartTime + state.turnDuration) - (System.currentTimeMillis() - timeDifference)
            )
            remainingTime = newTime
            if (newTime == 0L) {
                if (isMyTurn) {
                    timeIsUp = true // Ensure timeout action runs only once
                    println("⏳ Timeout reached! No answer selected.")
                    viewModel.onIntent(
                        GameIntent.MakeMove(
                            roomId,
                            profile.id ?: "",
                            ""
                        )
                    )
                    delay(UX_DELAY)

                } else {
                    if (state.profiles.size < 2) {
                        // we should make a move to result in game over.
                        viewModel.onIntent(
                            GameIntent.MakeMove(roomId, profile.id ?: "", "")
                        )
                    }
                }
                break
            }
            delay(1000)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameText(
            "${remainingTime / 1000} sec",
            fontSize = 32.sp,
        )

        GameText(state.question)

        // Display answers and handle selection
        state.answers.forEach { answer ->
            GameQuestionButton(
                answer,
                isMyTurn,
                state.selectedAnswer == answer,
                answer == state.correctAnswer
            ) {
                CoroutineScope(Dispatchers.Main).launch {
                    viewModel.onIntent(
                        GameIntent.MakeMove(
                            roomId,
                            profile.id ?: "",
                            answer
                        )
                    )
                    delay(UX_DELAY)
                }
            }

        }

        if (state.selectedAnswer.isNotBlank()) {
            Text(
                text = if (state.selectedAnswer == state.correctAnswer) "Correct!" else "Incorrect!",
                color = if (state.selectedAnswer == state.correctAnswer) Color.Green else Color.Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

        }

        if (!isMyTurn) {
            // Show message for non-active players
            GameText(
                "Waiting for ${currentProfilePlaying?.name ?: "Unknown"} to play...",
                fontSize = 12.sp
            )
        } else {
            // check if time is up
            if (timeIsUp) {
                GameText(stringResource(R.string.time_is_up))
            }
        }
        Scores(state)
    }


}

@Composable
fun Scores(gameState: GameState) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items(gameState.profiles) { profile ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameText(
                    text = profile.name,
                    fontSize = 24.sp
                )
                Spacer(Modifier.height(2.dp))
                AsyncImage(
                    model = profile.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(shape = CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(2.dp))
                GameText(
                    text = (gameState.scores[profile.id] ?: 0).toString(),
                    fontSize = 24.sp
                )
            }
        }
    }
}

@Composable
fun GameOver(
    state: GameState = GameState(),
    viewModel: GameViewModel,
    navController: NavHostController
) {
    Column(
        Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameTitle(stringResource(R.string.game_over))
            FinalScoreTable(state)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.profiles.size > 1) {
                GameButton(stringResource(R.string.re_match), onClick = { viewModel.gameStarted() })
            }
            GameButton(text = stringResource(R.string.done), onClick = {
                viewModel.onBack()
                navController.popBackStack()
            })
        }
    }


}

@Composable
fun WaitingRoom(roomId: String = "", navController: NavHostController, viewModel: GameViewModel) {
    // ClipboardManager for copying the deep link
    val clipboardManager = LocalClipboardManager.current

    BackHandler {
        viewModel.onBack()
        navController.popBackStack()
    }
    var linkToShare by remember { mutableStateOf("") }
    var timeout: Int by remember { mutableStateOf(Constants.WAITING_TIMEOUT) }
    var job by remember { mutableStateOf<Job?>(null) }  // Hold the job reference for controlling the timeout
    val coroutineScope = rememberCoroutineScope()

    // Show loading indicator until session is started
    LaunchedEffect(Unit) {
        viewModel.shareGameLink(roomId) { link ->
            linkToShare = link
        }
    }

    LaunchedEffect(Unit) {
        job = coroutineScope.launch {
            do {
                delay(1000)
                timeout -= 1;
            } while (timeout > 0)
            viewModel.onBack()
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(50.dp))
        GameTitle(stringResource(R.string.waiting_room))
        GameText("($roomId)")
        GameText(timeout.toString())
        GameLoadingAnimation(Modifier.size(96.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Text and button for copying the deep link
        Column(horizontalAlignment = Alignment.CenterHorizontally) {


            GameText(text = stringResource(R.string.share_this_link), fontSize = 16.sp)

            GameButton(
                text = stringResource(R.string.copy_link),
                onClick = {
                    // Copy the deep link to clipboard
                    clipboardManager.setText(AnnotatedString(linkToShare))
                }
            )

            GameButton(
                text = stringResource(R.string.cancel),
                onClick = {
                    navController.popBackStack()
                }
            )
        }
    }

}

