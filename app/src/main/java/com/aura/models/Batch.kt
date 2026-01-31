package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Batch(
    @DocumentId val id: String = "",
    val name: String = "", // e.g., "D division of Computer Engineering"
    val department: String = "",
    val year: String = "", // e.g., "2024" or "3rd Year"
    val createdBy: String = "", // Faculty ID
    val studentIds: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)