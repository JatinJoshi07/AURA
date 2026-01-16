package com.aura.models

data class FacultyDashboardStats(
    val assignedIncidents: Int = 0,
    val pendingIssues: Int = 0,
    val resolvedToday: Int = 0,
    val avgResponseTime: Int = 0,
    val responseRate: Int = 0,
    val resolutionRate: Int = 0,
    val avgRating: Double = 0.0
)
