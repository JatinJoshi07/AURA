package com.aura.models

data class SystemHealth(
    val serverStatus: String = "Online",
    val serverHealth: Int = 100, // 0-100
    val databaseStatus: String = "Online",
    val databaseHealth: Int = 100,
    val aiStatus: String = "Online",
    val aiHealth: Int = 100,
    val mapsStatus: String = "Online",
    val mapsHealth: Int = 100,
    val lastChecked: Long = System.currentTimeMillis()
)
