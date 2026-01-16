package com.aura.models


import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val name: String = "",
    val email: String = "",
    val enrollmentNumber: String = "",
    val mobileNumber: String = "",
    val department: String = "",
    val batchYear: Int = 0,
    val gender: String = "",
    val role: String = "student", // student, faculty, admin
    val skills: List<String> = emptyList(),
    val rating: Double = 0.0,
    val totalProjects: Int = 0,
    val panicKeyword: String? = null,
    val emergencyContacts: List<String> = emptyList(), // Phone numbers
    val trustedFriends: List<String> = emptyList(), // List of User IDs
    val fcmToken: String? = null,
    val profileImage: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val lastLogin: Timestamp = Timestamp.now()
)

data class Faculty(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val department: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true
)