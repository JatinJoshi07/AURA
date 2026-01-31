package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class AttendanceRecord(
    @DocumentId val id: String = "",
    val batchId: String = "",
    val facultyId: String = "",
    val date: Timestamp = Timestamp.now(),
    val studentAttendance: Map<String, Boolean> = emptyMap(), // studentId to isPresent
    val totalStudents: Int = 0,
    val presentCount: Int = 0
)
