package com.aura.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Batch(
    @DocumentId val id: String = "",
    val name: String = "",
    val department: String = "",
    val year: String = "",
    val createdBy: String = "",
    val studentIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)
