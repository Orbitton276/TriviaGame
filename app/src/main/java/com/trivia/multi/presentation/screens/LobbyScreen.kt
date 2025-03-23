package com.trivia.multi.presentation.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.trivia.multi.domain.model.GameIntent
import com.trivia.multi.domain.model.GameValidationResult
import com.trivia.multi.presentation.viewmodel.GameViewModel
import com.trivia.multi.utils.Common.Brush
import com.trivia.multi.utils.Common.GameButton
import com.trivia.multi.utils.Common.GameTextField

@Composable
fun LobbyScreen(
    viewModel: GameViewModel = hiltViewModel<GameViewModel>(),
    navController: NavController,
    onGameStart: (String) -> Unit = {}
) {
    var roomId by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var showJoinExtension by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val validating by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.joinRoomValidate.collect { result ->
                when (result) {
                    GameValidationResult.AlreadyStarted -> Toast.makeText(
                        context,
                        "Game Already Started!",
                        Toast.LENGTH_SHORT
                    ).show()

                    is GameValidationResult.Error -> Toast.makeText(
                        context,
                        result.error,
                        Toast.LENGTH_SHORT
                    ).show()

                    GameValidationResult.NotFound -> Toast.makeText(
                        context,
                        "Room not found!",
                        Toast.LENGTH_SHORT
                    ).show()

                    GameValidationResult.Valid -> onGameStart(roomId)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        topBar = { BuildTopBar(navController) },

        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(brush = Brush()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (!showJoinExtension) {
                    GameButton("Create Game",Modifier, validating) {
                        viewModel.onIntent(
                            GameIntent.CreateGameRoom(
                                onGameStart
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier.clickable {
                        // Trigger the animation
                        showJoinExtension = !showJoinExtension
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GameButton("Join Existing") {
                        showJoinExtension = !showJoinExtension
                    }

                    if (showJoinExtension) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Arrow",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Animate the appearance of the EXTRA text using AnimatedVisibility
                AnimatedVisibility(
                    visible = showJoinExtension,
                    enter = slideInVertically(initialOffsetY = { it }),
                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Enter Room Id",
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.fillMaxWidth()
                        )

                        GameTextField(roomId) {
                            roomId = it
                        }

                        AnimatedVisibility(
                            visible = roomId.isNotBlank(),
                            enter = slideInVertically(initialOffsetY = { -it }),
                        ) {
                            GameButton("Ready") {

                                viewModel.onIntent(GameIntent.ValidateRoom(roomId))
//                                onGameStart(roomId)
                            }
                        }

                    }
                }

                Spacer(Modifier.fillMaxHeight(fraction = 0.2f))
            }
        }

    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildTopBar(
    navController: NavController? = null,
    viewModel: GameViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    TopAppBar(
        modifier = Modifier.background(brush = Brush()),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent, // Make the container color transparent
        ),
        title = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(brush = Brush())
                    .padding(horizontal = 10.dp)
                    .clickable {
                        navController?.navigate(Screen.OnBoardingProfile.route)
                    },
                verticalAlignment = Alignment.CenterVertically

            ) {
                Text(
                    text = profile.name,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                TopBarProfileImage(viewModel)
            }
        },
    )
}

@Composable
fun TopBarProfileImage(viewModel: GameViewModel) {
    val profileState by viewModel.profile.collectAsState()
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = profileState.profileImage
            ),
            contentDescription = "Top Bar Profile",
            modifier = Modifier.fillMaxSize() // Ensures the image takes up the full size
        )
    }
}
