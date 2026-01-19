package com.aura.viewmodels

import androidx.lifecycle.ViewModel
import com.aura.utils.VoiceRecognition
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PinkShieldViewModel @Inject constructor(
    private val voiceRecognition: VoiceRecognition
) : ViewModel() {

    val isListening: StateFlow<Boolean> = voiceRecognition.isListening
    val recognizedText: StateFlow<String> = voiceRecognition.recognizedText
    val panicDetected: StateFlow<Boolean> = voiceRecognition.panicKeywordDetected
    val isReady: StateFlow<Boolean> = voiceRecognition.isReadyToSpeak

    fun resetPanic() {
        voiceRecognition.resetPanicDetection()
    }
}
