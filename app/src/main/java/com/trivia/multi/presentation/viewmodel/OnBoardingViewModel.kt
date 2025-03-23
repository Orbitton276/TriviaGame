package com.trivia.multi.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.MutableState
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trivia.multi.data.repository.ProfileRepository
import com.trivia.multi.domain.model.Profile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {


    private val _saveEvent = MutableSharedFlow<Unit>()
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    val profile = profileRepository.fetchProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Profile()
        )

    @OptIn(ExperimentalUuidApi::class)
    fun saveProfile(name: String, profileImage: Uri?, isImagePicked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val imageProfile = profileImage?.let { uri ->
                if (isImagePicked) {
                    // we need to use permanent uri
                    try {
                        profileImage.let { profileRepository.uploadImage(it, context) }
                    } catch (e:Exception) {
                        saveImageToInternalStorage(context, uri)
                    }
                } else {
                    profileImage
                }
            }
            profileRepository.setProfile(Profile(name, imageProfile.toString(),
                if (profile.value.id.isNullOrBlank()) Uuid.random().toString() else profile.value.id))
            _saveEvent.emit(Unit)
        }
    }

    fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null

        val fileName = "profile_image_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)

        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)

        inputStream.close()
        outputStream.close()

        return Uri.fromFile(file) // This URI is now permanent
    }

    fun createImageFile(context: Context): Uri {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "IMG_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match `android:authorities` in Manifest
            file
        )
    }

    fun takePicture(
        cameraPermissionGranted: Boolean,
        cameraImageUri: MutableState<Uri?>,
        takePictureLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
        cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>
    ) {
        if (cameraPermissionGranted) {
            val uri = createImageFile(context) // Create the image file and get the URI
            cameraImageUri.value = uri // Store the URI
            takePictureLauncher.launch(uri) // Launch the camera with the URI
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

}