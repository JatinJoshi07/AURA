package com.aura.di

import android.content.Context
import com.aura.utils.GeminiAI
import com.aura.utils.VoiceRecognition
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {

    @Provides
    @Singleton
    fun provideGeminiAI(@ApplicationContext context: Context): GeminiAI {
        return GeminiAI(context)
    }

    @Provides
    @Singleton
    fun provideVoiceRecognition(@ApplicationContext context: Context): VoiceRecognition {
        return VoiceRecognition(context)
    }
}