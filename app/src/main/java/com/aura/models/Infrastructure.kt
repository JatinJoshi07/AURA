package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class InfrastructureIssue(
    @DocumentId val id: String = "",
    val reportedBy: String = "",
    val reportedByName: String = "",
    val category: String = "", // electrical, plumbing, furniture, safety, other
    val description: String = "",
    val location: GeoPoint? = null,
    val building: String = "",
    val floor: String = "",
    val room: String = "",
    val imageUrl: String = "",
    val aiAnalysis: AiAnalysis? = null,
    val status: String = "", // reported, assigned, in_progress, resolved
    val priority: String = "", // low, medium, high, critical
    val assignedTo: String? = null,
    val estimatedCost: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val resolvedAt: Timestamp? = null
)

data class AiAnalysis(
    val objectIdentified: String = "",
    val dangerLevel: Int = 1,
    val confidence: Double = 0.0,
    val suggestedAction: String = "",
    val estimatedSeverity: String = ""
)

data class Broadcast(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // emergency, exam, event, general
    val priority: String = "normal",
    val sentBy: String = "",
    val sentByName: String = "",
    val audience: List<String> = emptyList(), // all, students, faculty, specific_department
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null
)
