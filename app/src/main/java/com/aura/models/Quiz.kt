package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Quiz(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val batchId: String = "",
    val facultyId: String = "",
    val durationInMinutes: Int = 0,
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),
    val questions: List<Question> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true
)

data class Question(
    val id: String = "",
    val text: String = "",
    val type: String = "MCQ", // MCQ, TF, SHORT
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val points: Int = 1
)

data class QuizAttempt(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val userId: String = "",
    val userName: String = "",
    val score: Int = 0,
    val totalPoints: Int = 0,
    val timeTakenSeconds: Long = 0,
    val submittedAt: Timestamp = Timestamp.now(),
    val answers: Map<String, String> = emptyMap() // questionId to studentAnswer
)
