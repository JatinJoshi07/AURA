package com.aura.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class VoiceRecognition(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _panicKeywordDetected = MutableStateFlow(false)
    val panicKeywordDetected: StateFlow<Boolean> = _panicKeywordDetected

    private val _isReadyToSpeak = MutableStateFlow(false)
    val isReadyToSpeak: StateFlow<Boolean> = _isReadyToSpeak

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentPanicKeyword: String = ""
    private var isContinuous: Boolean = false

    fun startListening(panicKeyword: String, continuous: Boolean = false) {
        currentPanicKeyword = panicKeyword
        isContinuous = continuous
        
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                val msg = "Speech recognition not available"
                Log.e("VoiceRecognition", msg)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                return@post
            }

            try {
                // Ensure we don't have multiple instances
                stopListeningInternal()

                _isListening.value = true
                _panicKeywordDetected.value = false
                _recognizedText.value = "Initializing..."
                _isReadyToSpeak.value = false

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("VoiceRecognition", "onReadyForSpeech")
                        _isReadyToSpeak.value = true
                        _recognizedText.value = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("VoiceRecognition", "onBeginningOfSpeech")
                        _recognizedText.value = "Sensing voice..."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d("VoiceRecognition", "onEndOfSpeech")
                        _isReadyToSpeak.value = false
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission missing"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                            else -> "Error: $error"
                        }
                        Log.e("VoiceRecognition", "onError: $message ($error)")
                        _recognizedText.value = "Status: $message"
                        
                        // Restart immediately if continuous and not a fatal error
                        if (isContinuous && (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || 
                            error == SpeechRecognizer.ERROR_NO_MATCH || 
                            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
                            
                            mainHandler.postDelayed({
                                if (_isListening.value) {
                                    startListeningInternal()
                                }
                            }, 500)
                        } else {
                            _isListening.value = false
                            _isReadyToSpeak.value = false
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _recognizedText.value = text
                        Log.d("VoiceRecognition", "onResults: $text")

                        if (text.contains(currentPanicKeyword, ignoreCase = true)) {
                            _panicKeywordDetected.value = true
                            Log.d("VoiceRecognition", "Keyword MATCHED!")
                        }
                        
                        if (isContinuous) {
                            mainHandler.postDelayed({
                                if (_isListening.value) {
                                    startListeningInternal()
                                }
                            }, 300)
                        } else {
                            _isListening.value = false
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _recognizedText.value = text
                            if (text.contains(currentPanicKeyword, ignoreCase = true)) {
                                _panicKeywordDetected.value = true
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                startListeningInternal()
            } catch (e: Exception) {
                Log.e("VoiceRecognition", "Crash in startListening: ${e.message}")
                _isListening.value = false
                _recognizedText.value = "Service error"
            }
        }
    }

    private fun startListeningInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceRecognition", "Failed to startListeningInternal: ${e.message}")
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceRecognition", "Error during stop: ${e.message}")
        } finally {
            speechRecognizer = null
        }
    }

    fun stopListening() {
        mainHandler.post {
            isContinuous = false
            stopListeningInternal()
            _isListening.value = false
            _isReadyToSpeak.value = false
        }
    }

    fun cleanup() {
        stopListening()
    }
    
    fun resetPanicDetection() {
        _panicKeywordDetected.value = false
    }
}
