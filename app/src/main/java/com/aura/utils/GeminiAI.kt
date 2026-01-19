package com.aura.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAI(private val context: Context) {

    private val tag = "GeminiAI"
    
    // Optimized safety settings for institutional safety guidance
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH)
    )

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyAykyRrE-4LmkbroqfsIIZlCdp7MrxKo4o",
            generationConfig = generationConfig {
                temperature = 0.75f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 2048
            },
            safetySettings = safetySettings
        )
    }

    suspend fun generateResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (text.isNullOrBlank()) {
                    Log.e(tag, "AI returned empty text. Finreason: ${response.candidates.firstOrNull()?.finishReason}")
                    "I'm listening, but I couldn't generate a specific answer for that. Could you provide more details?"
                } else {
                    text
                }
            } catch (e: Exception) {
                Log.e(tag, "AI Generation Error: ${e.message}")
                "I'm temporarily unavailable. Please check your network and try again."
            }
        }
    }

    suspend fun analyzeImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Identify infrastructure or safety issues in this campus photo. Provide object, danger level (1-5), and suggested action."
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text ?: "Image analysis unavailable."
            } catch (e: Exception) {
                "Error analyzing image: ${e.message}"
            }
        }
    }

    suspend fun analyzeComplaint(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Summarize this student complaint concisely with sentiment and urgency (1-5): $text"
                val response = generativeModel.generateContent(prompt)
                response.text ?: "Complaint summary unavailable."
            } catch (e: Exception) {
                "Error processing complaint."
            }
        }
    }
}
