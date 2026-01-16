package com.aura.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.utils.GeminiAI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeminiViewModel @Inject constructor(
    private val geminiAI: GeminiAI
) : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Hello! I'm your AURA AI Companion. I can help students with safety tips and guide faculties with incident management. How can I help you today?", isUser = false)
    ))
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String, role: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text, isUser = true)
        _chatHistory.value = _chatHistory.value + userMessage
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val contextPrompt = when (role) {
                    "faculty" -> "Act as an expert university safety officer assisting faculty. Answer clearly and concisely. Faculty question: $text"
                    "admin" -> "Act as a campus systems analyst assisting administrators. Focus on infrastructure and security management. Admin question: $text"
                    else -> "Act as a helpful campus safety companion for students. Provide practical safety tips and guidance. Student question: $text"
                }
                
                // Use the generic generateResponse method for better reliability
                val response = geminiAI.generateResponse(contextPrompt)
                _chatHistory.value = _chatHistory.value + ChatMessage(response, isUser = false)
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + ChatMessage("I encountered an issue. Please try rephrasing your question.", isUser = false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
