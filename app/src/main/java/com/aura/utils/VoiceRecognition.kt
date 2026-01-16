package com.aura.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException

class VoiceRecognition(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _panicKeywordDetected = MutableStateFlow(false)
    val panicKeywordDetected: StateFlow<Boolean> = _panicKeywordDetected

    @RequiresApi(Build.VERSION_CODES.S)
    fun startListening(panicKeyword: String) {
        _isListening.value = true
        _panicKeywordDetected.value = false

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    // Handle error
                }

                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _recognizedText.value = text

                    // Check for panic keyword
                    if (text.contains(panicKeyword, ignoreCase = true)) {
                        _panicKeywordDetected.value = true
                        triggerEmergency()
                    }
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {}

                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
        }

        speechRecognizer?.startListening(
            android.speech.RecognizerIntent.getVoiceDetailsIntent(context)
        )

        // Start recording audio for emergency clip
        startRecording()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startRecording() {
        try {
            val audioFile = File.createTempFile("emergency_audio", ".3gp", context.cacheDir)

            mediaRecorder = MediaRecorder(context).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)

                prepare()
                start()
            }

            // Record for 10 seconds max
            android.os.Handler().postDelayed({
                stopRecordingAndUpload(audioFile)
            }, 10000)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecordingAndUpload(audioFile: File) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            // Upload audio file to Firebase Storage
            // This would be implemented with Firebase Storage
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerEmergency() {
        // Trigger emergency response
        // Send location, audio clip, and alert to security
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null

        _isListening.value = false
    }

    fun cleanup() {
        stopListening()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}