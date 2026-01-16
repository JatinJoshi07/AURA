package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

data class Emergency(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "", // manual, medical, security, fire, harassment
    val status: String = "active", // active, resolved, cancelled
    val location: GeoPoint? = null,
    val address: String = "",
    val description: String = "",
    val audioClipUrl: String? = null,
    val imageUrl: String? = null,
    val dangerLevel: Int = 1, // 1-5 scale
    @ServerTimestamp val timestamp: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val assignedTo: String? = null,
    val priority: String = "medium" // low, medium, high, critical
)

data class SafeRoute(
    val id: String = "",
    val fromLocation: GeoPoint,
    val toLocation: GeoPoint,
    val pathCoordinates: List<GeoPoint> = emptyList(),
    val safetyScore: Int = 0,
    val lightingLevel: String = "", // good, moderate, poor
    val footTraffic: String = "", // high, medium, low
    val estimatedTime: Int = 0, // in minutes
    val lastUpdated: Timestamp = Timestamp.now()
)
