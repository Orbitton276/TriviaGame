package com.trivia.multi.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.trivia.multi.data.repository.GameRepository
import com.trivia.multi.data.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideGameRepository(
        firestore: FirebaseFirestore,
        profileRepository: ProfileRepository,
        @ApplicationContext context: Context
    ): GameRepository {
        return GameRepository(firestore, profileRepository, context)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(
        @ApplicationContext context: Context
    ): ProfileRepository {
        return ProfileRepository(context)
    }
}
