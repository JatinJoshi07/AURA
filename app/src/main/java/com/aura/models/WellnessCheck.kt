package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class WellnessCheck(
    @DocumentId val id: String = "",
    val userId: String = "",
    val moodLevel: Int = 3, // 1-5 scale
    val note: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isAnonymous: Boolean = true
)

data class CampusMood(
    val averageMood: Double = 0.0,
    val totalChecks: Int = 0,
    val veryPoorCount: Int = 0,
    val poorCount: Int = 0,
    val neutralCount: Int = 0,
    val goodCount: Int = 0,
    val excellentCount: Int = 0,
    val veryPoor: Float = 0f,
    val poor: Float = 0f,
    val neutral: Float = 0f,
    val good: Float = 0f,
    val excellent: Float = 0f,
    val positivePercentage: Int = 0
)
