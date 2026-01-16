package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ProfileUpdateRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "",
    val requestedChanges: Map<String, Any> = emptyMap(),
    val status: String = "pending", // pending, approved, rejected
    val adminNotes: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val processedAt: Timestamp? = null
)
