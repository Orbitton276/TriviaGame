package com.trivia.multi.presentation.screens

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.trivia.multi.presentation.viewmodel.OnBoardingViewModel
import com.trivia.multi.utils.Common.Brush
import com.trivia.multi.utils.Common.GameButton
import com.trivia.multi.utils.Common.GameTextField
import com.trivia.multi.utils.Common.GameTitle
import kotlinx.coroutines.delay


@Composable
fun OnBoarding(
    navController: NavHostController = rememberNavController(),
) {
    val MARGIN = 100.dp
    Scaffold(Modifier.fillMaxSize()) { padd ->

        Box(
            modifier = Modifier
                .padding(padd)
                .fillMaxSize()
                .background(brush = Brush()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column {
                    GameTitle("quiz game", modifier = Modifier.padding(top = MARGIN))
                }

                GameButton("start", modifier = Modifier.padding(bottom = MARGIN)) {
                    navController.navigate(Screen.OnBoardingProfile.route)
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun OnBoardingProfile(
    navController: NavHostController = rememberNavController(),
    onBoardingViewModel: OnBoardingViewModel = hiltViewModel()
) {
    val profile by onBoardingViewModel.profile.collectAsState()
    val MARGIN = 100.dp
    var name by remember { mutableStateOf(profile.name) }
    var imageUri by remember { mutableStateOf<Uri?>(profile.profileImage.toUri()) }
    var isImagePicked by remember { mutableStateOf(false) }
    var showSaveLoading by remember { mutableStateOf(false) }
    var saveDone by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        name = profile.name
        imageUri = profile.profileImage.toUri() ?: null
    }
    val context = LocalContext.current

    // Camera Permission
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var runCameraAfterPermission by remember { mutableStateOf(false) }
    // Camera Capture (Creates a file first)
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri.value?.let {
                    imageUri = it
                    isImagePicked = true
                }
            }
        }

    LaunchedEffect(Unit) {
        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        println(cameraPermissionGranted)
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!cameraPermissionGranted && isGranted) {
            runCameraAfterPermission = true
        }
        cameraPermissionGranted = isGranted
    }


    // Gallery Picker
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                isImagePicked = true
            }
        }

    if (runCameraAfterPermission) {
        runCameraAfterPermission = false
//        takePicture()
        onBoardingViewModel.takePicture(
            cameraPermissionGranted,
            cameraImageUri,
            takePictureLauncher,
            cameraPermissionLauncher
        )
    }

    fun showImageSourceDialog(context: Context, pickImage: () -> Unit) {
        val options = arrayOf("Pick from Gallery", "Take a Photo")
        AlertDialog.Builder(context)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImage() // Open Gallery
                    1 -> onBoardingViewModel.takePicture(
                        cameraPermissionGranted,
                        cameraImageUri,
                        takePictureLauncher,
                        cameraPermissionLauncher
                    )

                }
            }
            .show()
    }
    LaunchedEffect(Unit) {
        onBoardingViewModel.saveEvent.collect { emitted ->
            // navigate only after save
            showSaveLoading = false
            saveDone = true
            delay(200)
            navController.navigate(Screen.Lobby.route) {
                popUpTo(Screen.OnBoarding.route) {
                    inclusive = true
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current

    Scaffold(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
    ) { padd ->
        Box(
            modifier = Modifier
                .padding(padd)
                .fillMaxSize()
                .background(Brush()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var isValid by remember { mutableStateOf(true) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GameTitle("profile")
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .padding(4.dp)
                            .clickable {
                                showImageSourceDialog(
                                    context,
                                    { pickImageLauncher.launch("image/*") })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = imageUri,
                                placeholder = rememberVectorPainter(Icons.Filled.Person),
                                error = rememberVectorPainter(Icons.Filled.Person)
                            ),
                            contentDescription = "Contact Photo",
                            modifier = Modifier
                                .size(256.dp)
                                .background(Color.Gray)
                                .border(2.dp, Color.White, CircleShape)
                                .clip(CircleShape)
                                .then(
                                    if (imageUri.toString()
                                            .isEmpty()
                                    ) Modifier.padding(12.dp) else Modifier
                                ),

                            contentScale = ContentScale.Crop
                        )
                    }
                    // Button for Selecting Image
                    Button(
                        onClick = {
                            showImageSourceDialog(
                                context,
                                { pickImageLauncher.launch("image/*") })
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color(0xFF542DD6),
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Select Image",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center
                        )
                    }
                    GameTextField(name, isValid, onNameChange = {
                        isValid = true
                        name = it
                    })
                }

                // Save/Next button

                GameButton(
                    text = "done",
                    modifier = Modifier.padding(bottom = MARGIN),
                    showSaveLoading
                ) {
                    isValid = name.isNotBlank()
                    if (isValid) {
                        showSaveLoading = true
                        onBoardingViewModel.saveProfile(name, imageUri, isImagePicked)
                    }
                }

            }
        }
    }

}

