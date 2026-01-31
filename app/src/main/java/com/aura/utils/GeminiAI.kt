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
    
    // Safety settings for campus usage
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH)
    )

    private val generativeModel by lazy {
        GenerativeModel(
            // Corrected model string format for SDK version 0.9.0
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyAfaROPktnIljltwht-DOFuHnUIHC8fky0",
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
                response.text ?: "I'm sorry, I couldn't generate a response."
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: ""
                Log.e(tag, "Gemini Error: $errorMsg")
                
                // Detailed handling for common API errors
                when {
                    errorMsg.contains("blocked", ignoreCase = true) -> 
                        "API Blocked: Please enable 'Generative Language API' in Google Cloud Console."
                    errorMsg.contains("404", ignoreCase = true) -> 
                        "Model Not Found: The model name or version might be restricted for your region/key."
                    errorMsg.contains("API_KEY_INVALID", ignoreCase = true) -> 
                        "Invalid API Key. Please verify your credentials."
                    else -> "AI Service Error: ${e.localizedMessage}"
                }
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
                "Error analyzing image: ${e.localizedMessage}"
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
                "Error processing complaint: ${e.localizedMessage}"
            }
        }
    }
}
