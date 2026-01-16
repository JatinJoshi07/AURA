package com.aura.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.Complaint
import com.aura.models.User
import com.aura.utils.GeminiAI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ComplaintViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val geminiAI: GeminiAI
) : ViewModel() {

    private val _myComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val myComplaints: StateFlow<List<Complaint>> = _myComplaints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var complaintsListener: ListenerRegistration? = null

    fun loadMyComplaints(userId: String) {
        complaintsListener?.remove()
        _isLoading.value = true
        
        complaintsListener = firestore.collection("complaints")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    android.util.Log.e("ComplaintViewModel", "Listener error: ${error.message}")
                    _errorMessage.value = "Access Denied: Check Firebase Rules"
                    return@addSnapshotListener
                }

                val complaints = snapshot?.documents?.mapNotNull {
                    it.toObject(Complaint::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                
                _myComplaints.value = complaints
                _errorMessage.value = null
            }
    }

    fun submitComplaint(
        title: String,
        description: String,
        type: String,
        category: String,
        isAnonymous: Boolean,
        evidenceUris: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            try {
                val firebaseUser = auth.currentUser ?: throw Exception("Not authenticated")
                
                val evidenceUrls = mutableListOf<String>()
                evidenceUris.forEach { uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        val fileName = "complaints/${firebaseUser.uid}/${UUID.randomUUID()}.jpg"
                        val ref = storage.reference.child(fileName)
                        ref.putFile(uri).await()
                        val downloadUrl = ref.downloadUrl.await().toString()
                        evidenceUrls.add(downloadUrl)
                    } catch (e: Exception) {
                        android.util.Log.e("ComplaintViewModel", "Image upload failed: ${e.message}")
                    }
                }

                val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
                val user = userDoc.toObject(User::class.java)
                val userName = if (isAnonymous) "Anonymous" else user?.name ?: "User"

                val analysis = try {
                    geminiAI.analyzeComplaint(description)
                } catch (e: Exception) {
                    "Analysis unavailable"
                }

                val complaint = Complaint(
                    userId = firebaseUser.uid,
                    userName = userName,
                    type = type,
                    title = title,
                    description = description,
                    isAnonymous = isAnonymous,
                    status = "pending",
                    category = category,
                    evidenceUrls = evidenceUrls,
                    priority = if (category == "safety" || type == "harassment") "high" else "medium"
                )

                firestore.collection("complaints").add(complaint).await()
                _successMessage.value = "Report filed successfully."
                
            } catch (e: Exception) {
                android.util.Log.e("ComplaintViewModel", "Submit error: ${e.message}")
                _errorMessage.value = "Submission Failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        complaintsListener?.remove()
    }
}
