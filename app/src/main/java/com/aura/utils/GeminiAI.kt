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
    
    // Safety settings to ensure campus safety queries are not blocked incorrectly
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
    )

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyAykyRrE-4LmkbroqfsIIZlCdp7MrxKo4o",
            generationConfig = generationConfig {
                temperature = 0.7f
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
                // Ensure the prompt is clear and doesn't trigger filters unnecessarily
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (text.isNullOrBlank()) {
                    Log.e(tag, "AI returned empty response. Finish reason: ${response.candidates.firstOrNull()?.finishReason}")
                    "I'm sorry, I couldn't generate a helpful response for that. Could you try rephrasing?"
                } else {
                    text
                }
            } catch (e: Exception) {
                Log.e(tag, "Generation error: ${e.message}")
                "I encountered a technical issue while processing your request. Please check your internet connection or try again later."
            }
        }
    }

    suspend fun analyzeImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze this image and identify any infrastructure issues, safety hazards, 
                    or maintenance problems in a college campus setting. 
                    Provide:
                    1. Object identified
                    2. Danger level (1-5)
                    3. Suggested action
                    4. Estimated severity (low, medium, high, critical)
                    
                    Be concise and accurate.
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text ?: "Unable to analyze image"
            } catch (e: Exception) {
                Log.e(tag, "Image analysis error: ${e.message}")
                "Error analyzing image: ${e.message}"
            }
        }
    }

    suspend fun analyzeComplaint(text: String): String {
        return withContext(Dispatchers.IO) {
            if (text.isBlank()) return@withContext "Empty complaint text"
            
            try {
                val prompt = """
                    Analyze this complaint text from a college student:
                    "$text"
                    
                    Provide a concise summary including:
                    1. Sentiment (positive, negative, neutral)
                    2. Urgency level (1-5)
                    3. Suggested category (academic, personal, safety, infrastructure)
                    4. Key issues identified
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                response.text ?: "No analysis available"
            } catch (e: Exception) {
                Log.e(tag, "Complaint analysis error: ${e.message}")
                "Analysis currently unavailable."
            }
        }
    }

    suspend fun generateSafetyTips(location: String, time: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Generate safety tips for a student walking in $location at $time.
                    Consider lighting, common hazards, and emergency procedures.
                    Provide 5 concise bullet points.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                response.text ?: "Stay aware of your surroundings and keep your phone charged."
            } catch (e: Exception) {
                Log.e(tag, "Safety tips error: ${e.message}")
                "Stay aware of your surroundings."
            }
        }
    }
}
