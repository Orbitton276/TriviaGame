package com.trivia.multi.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.trivia.multi.BuildConfig
import com.trivia.multi.domain.model.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {

    private val Context.dataStorePreference: DataStore<Preferences> by preferencesDataStore(name = "profile_prefs")
    private val dataStorePreference = applicationContext.dataStorePreference

    // Fetch the complete profile by combining both name and image
    fun fetchProfile(): Flow<Profile> {
        return DataStoreManager.getProfileFlow(applicationContext)
    }

    suspend fun setProfile(profile: Profile) {
        DataStoreManager.setProfile(applicationContext, profile = profile)
    }

    suspend fun uploadImage(imageUri: Uri, context: Context): String? {
        val client = OkHttpClient()

        val imageFile = File(getPathFromUri(imageUri, context)) // Get file path from URI
        val base64Image =
            android.util.Base64.encodeToString(imageFile.readBytes(), android.util.Base64.DEFAULT)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", base64Image)
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload?key=${BuildConfig.IMGBB_API_KEY}")
            .post(requestBody)
            .build()

        return suspendCancellableCoroutine { continuation ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val imageUrl = json.getJSONObject("data").getString("url")
                        continuation.resumeWith(Result.success(imageUrl)) // Return the successful result
                    } else {
                        continuation.resumeWithException(IOException("Upload failed: ${response.message}")) // Signal an error if response is not successful
                    }
                }
            })
        }
    }

    private fun getPathFromUri(uri: Uri, context: Context): String {
        val fileName = getFileName(uri, context)
        val file = File(context.cacheDir, fileName)

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    private fun getFileName(uri: Uri, context: Context): String {
        var name = "temp_file"
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}

object DataStoreManager {
    private const val PREFERENCES_NAME = "user_prefs"

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

    val PROFILE_NAME_KEY = stringPreferencesKey("name")
    val PROFILE_IMAGE_KEY = stringPreferencesKey("image")
    val PROFILE_ID_KEY = stringPreferencesKey("id")

    // Save string to DataStore
    suspend fun saveString(context: Context, key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { prefs -> prefs[key] = value }
    }

    // Get string from DataStore
    fun getString(context: Context, key: Preferences.Key<String>): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[key] }
    }

    // Get the complete profile from DataStore
    fun getProfileFlow(context: Context): Flow<Profile> {
        val nameFlow = getString(context, PROFILE_NAME_KEY)
        val imageFlow = getString(context, PROFILE_IMAGE_KEY)
        val id = getString(context, PROFILE_ID_KEY)


        return combine(nameFlow, imageFlow, id) { name, image, id ->
//            val isFetched = if (id.isNullOrEmpty()) false else true
            Profile(name ?: "", image ?: "", id ?: null, true)
        }
    }

    suspend fun getProfile(context: Context): Profile {
        return context.dataStore.data
            .first { it[PROFILE_NAME_KEY] != null && it[PROFILE_IMAGE_KEY] != null }
            .let { prefs ->
                Profile(
                    name = prefs[PROFILE_NAME_KEY] ?: "",
                    profileImage = prefs[PROFILE_IMAGE_KEY] ?: "",
                    id = prefs[PROFILE_ID_KEY] ?: ""
                )
            }
    }

    suspend fun setProfile(context: Context, profile: Profile) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_NAME_KEY] = profile.name
            prefs[PROFILE_IMAGE_KEY] = profile.profileImage
            prefs[PROFILE_ID_KEY] = profile.id ?: ""
        }

    }
}
