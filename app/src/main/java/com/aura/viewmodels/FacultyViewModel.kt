package com.aura.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class FacultyViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _dashboardStats = MutableStateFlow(FacultyDashboardStats())
    val dashboardStats: StateFlow<FacultyDashboardStats> = _dashboardStats.asStateFlow()

    private val _assignedEmergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val assignedEmergencies: StateFlow<List<Emergency>> = _assignedEmergencies.asStateFlow()

    private val _assignedIssues = MutableStateFlow<List<InfrastructureIssue>>(emptyList())
    val assignedIssues: StateFlow<List<InfrastructureIssue>> = _assignedIssues.asStateFlow()

    private val _allIssues = MutableStateFlow<List<InfrastructureIssue>>(emptyList())
    val allIssues: StateFlow<List<InfrastructureIssue>> = _allIssues.asStateFlow()

    private val _allComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val allComplaints: StateFlow<List<Complaint>> = _allComplaints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var assignedEmergenciesListener: ListenerRegistration? = null
    private var assignedIssuesListener: ListenerRegistration? = null
    private var allIssuesListener: ListenerRegistration? = null
    private var allComplaintsListener: ListenerRegistration? = null

    init {
        startListeningToData()
    }

    private fun startListeningToData() {
        val currentUser = auth.currentUser ?: return
        
        // 1. Assigned Emergencies
        assignedEmergenciesListener = firestore.collection("emergencies")
            .whereEqualTo("assignedTo", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Emergency::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                _assignedEmergencies.value = list
                updateDashboardStats()
            }

        // 2. Assigned Infrastructure Issues
        assignedIssuesListener = firestore.collection("infrastructure")
            .whereEqualTo("assignedTo", currentUser.uid)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(InfrastructureIssue::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                _assignedIssues.value = list
                updateDashboardStats()
            }

        // 3. All Infrastructure Issues (for management screen)
        allIssuesListener = firestore.collection("infrastructure")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(InfrastructureIssue::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                _allIssues.value = list
            }

        // 4. All Complaints (for complaints screen)
        allComplaintsListener = firestore.collection("complaints")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Complaint::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                _allComplaints.value = list
            }
    }

    private fun updateDashboardStats() {
        val activeAssigned = _assignedEmergencies.value.count { it.status == "active" }
        val pendingAssignedIssues = _assignedIssues.value.count { it.status == "reported" || it.status == "assigned" }
        
        // Resolved today would ideally be a separate listener or filtered from a larger set
        // For simplicity, we filter from current assigned list
        val today = com.google.firebase.Timestamp.now()
        val startOfToday = com.google.firebase.Timestamp(today.seconds - (today.seconds % 86400), 0)
        
        val resolvedTodayCount = _assignedEmergencies.value.count { 
            it.status == "resolved" && it.resolvedAt != null && it.resolvedAt!! >= startOfToday 
        }

        _dashboardStats.value = FacultyDashboardStats(
            assignedIncidents = activeAssigned,
            pendingIssues = pendingAssignedIssues,
            resolvedToday = resolvedTodayCount
        )
    }

    fun updateComplaintStatus(complaintId: String, status: String) {
        viewModelScope.launch {
            try {
                firestore.collection("complaints").document(complaintId)
                    .update("status", status).await()
            } catch (e: Exception) {
                // Error handled by real-time listener if it fails
            }
        }
    }

    fun resolveEmergency(emergencyId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("emergencies").document(emergencyId)
                    .update(
                        mapOf(
                            "status" to "resolved",
                            "resolvedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            } catch (e: Exception) {}
        }
    }

    fun updateIssueStatus(issueId: String, status: String) {
        viewModelScope.launch {
            try {
                val updateData = mutableMapOf<String, Any>("status" to status)
                if (status == "resolved") updateData["resolvedAt"] = FieldValue.serverTimestamp()
                firestore.collection("infrastructure").document(issueId).update(updateData).await()
            } catch (e: Exception) {}
        }
    }

    // Stub methods for backward compatibility with screens calling them
    fun loadDashboardData() {}
    fun loadAllComplaints() {}
    fun loadAssignedEmergencies() {}
    fun loadInfrastructureIssues() {}

    override fun onCleared() {
        super.onCleared()
        assignedEmergenciesListener?.remove()
        assignedIssuesListener?.remove()
        allIssuesListener?.remove()
        allComplaintsListener?.remove()
    }
}
