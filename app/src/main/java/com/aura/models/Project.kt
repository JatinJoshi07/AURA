package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Project(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val department: String = "",
    val requiredSkills: List<String> = emptyList(),
    val teamMembers: List<String> = emptyList(), // user IDs
    val status: String = "", // open, in_progress, completed, cancelled
    val createdAt: Timestamp = Timestamp.now(),
    val deadline: Timestamp? = null,
    val maxTeamSize: Int = 5,
    val tags: List<String> = emptyList(),
    val visibility: String = "public" // public, department_only, private
)

data class ProjectApplication(
    val id: String = "",
    val projectId: String = "",
    val applicantId: String = "",
    val applicantName: String = "",
    val skills: List<String> = emptyList(),
    val message: String = "",
    val status: String = "", // pending, accepted, rejected
    val appliedAt: Timestamp = Timestamp.now()
)