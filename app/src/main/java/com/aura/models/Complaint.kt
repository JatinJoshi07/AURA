package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Complaint(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "", // harassment, bullying, infrastructure, other
    val title: String = "",
    val description: String = "",
    val isAnonymous: Boolean = true,
    val status: String = "", // pending, in_review, resolved, rejected
    val priority: String = "medium",
    val sentimentScore: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val resolvedAt: Timestamp? = null,
    val assignedTo: String? = null,
    val evidenceUrls: List<String> = emptyList(),
    val category: String = "" // academic, personal, safety, infrastructure
)
