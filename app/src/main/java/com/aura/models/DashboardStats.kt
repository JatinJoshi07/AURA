package com.aura.models

data class DashboardStats(
    val totalUsers: Int = 0,
    val activeEmergencies: Int = 0,
    val pendingComplaints: Int = 0,
    val infrastructureIssues: Int = 0,
    val resolvedToday: Int = 0
)
