package com.aura.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.ProfileUpdateRequest
import com.aura.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _pendingRequest = MutableStateFlow<ProfileUpdateRequest?>(null)
    val pendingRequest: StateFlow<ProfileUpdateRequest?> = _pendingRequest.asStateFlow()

    init {
        fetchPendingRequest()
    }

    private fun fetchPendingRequest() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("profile_update_requests")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "pending")
                    .limit(1)
                    .get()
                    .await()
                
                _pendingRequest.value = snapshot.documents.firstOrNull()?.toObject(ProfileUpdateRequest::class.java)
                    ?.copy(id = snapshot.documents.first().id)
            } catch (e: Exception) {
                // Silently fail or log
            }
        }
    }

    fun uploadProfilePicture(user: User, imageUri: Uri) {
        viewModelScope.launch {
            _message.value = "Image selection received, but upload is disabled due to database issues."
        }
    }

    fun updateProfileDirectly(userId: String, updates: Map<String, Any>) {
        if (updates.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("users").document(userId).update(updates).await()
                _message.value = "Profile updated successfully"
            } catch (e: Exception) {
                _message.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestProfileUpdate(user: User, requestedChanges: Map<String, Any>) {
        if (requestedChanges.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if a request already exists
                val existing = firestore.collection("profile_update_requests")
                    .whereEqualTo("userId", user.id)
                    .whereEqualTo("status", "pending")
                    .get().await()

                if (!existing.isEmpty) {
                    // Update existing request or block
                    _message.value = "You already have a pending update request"
                    return@launch
                }

                val request = ProfileUpdateRequest(
                    userId = user.id,
                    userName = user.name,
                    userRole = user.role,
                    requestedChanges = requestedChanges,
                    status = "pending"
                )
                firestore.collection("profile_update_requests").add(request).await()
                _message.value = "Identity update request sent to admin"
                fetchPendingRequest()
            } catch (e: Exception) {
                _message.value = "Failed to send request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
