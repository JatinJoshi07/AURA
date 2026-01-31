package com.aura.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
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

    private val _allComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val allComplaints: StateFlow<List<Complaint>> = _allComplaints.asStateFlow()

    private val _myBatches = MutableStateFlow<List<Batch>>(emptyList())
    val myBatches: StateFlow<List<Batch>> = _myBatches.asStateFlow()

    private val _allStudents = MutableStateFlow<List<User>>(emptyList())
    val allStudents: StateFlow<List<User>> = _allStudents.asStateFlow()

    private val _batchStudents = MutableStateFlow<List<User>>(emptyList())
    val batchStudents: StateFlow<List<User>> = _batchStudents.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _assignedIssues = MutableStateFlow<List<InfrastructureIssue>>(emptyList())
    val assignedIssues: StateFlow<List<InfrastructureIssue>> = _assignedIssues.asStateFlow()

    private val _allIssues = MutableStateFlow<List<InfrastructureIssue>>(emptyList())
    val allIssues: StateFlow<List<InfrastructureIssue>> = _allIssues.asStateFlow()

    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var batchesListener: ListenerRegistration? = null
    private var complaintsListener: ListenerRegistration? = null
    private var emergenciesListener: ListenerRegistration? = null
    private var issuesListener: ListenerRegistration? = null

    init {
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("FacultyVM", "User authenticated: ${user.uid}. Starting listeners...")
                startListening(user.uid)
                loadAllStudents()
            } else {
                stopListening()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun startListening(uid: String) {
        stopListening()

        batchesListener = firestore.collection("batches")
            .whereEqualTo("createdBy", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Batch::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _myBatches.value = list
            }

        complaintsListener = firestore.collection("complaints")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(Complaint::class.java)?.copy(id = it.id) } ?: emptyList()
                _allComplaints.value = list
            }

        emergenciesListener = firestore.collection("emergencies")
            .whereEqualTo("assignedTo", uid)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(Emergency::class.java)?.copy(id = it.id) } ?: emptyList()
                _assignedEmergencies.value = list
                updateDashboardStats()
            }
            
        issuesListener = firestore.collection("infrastructure")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject(InfrastructureIssue::class.java)?.copy(id = it.id) } ?: emptyList()
                _allIssues.value = list
                _assignedIssues.value = list.filter { it.assignedTo == uid }
                updateDashboardStats()
            }
    }

    fun loadAllStudents() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("role", "student")
                    .get()
                    .await()
                val students = snapshot.documents.mapNotNull { it.toObject(User::class.java)?.copy(id = it.id) }
                _allStudents.value = students
            } catch (e: Exception) {
                Log.e("FacultyVM", "Error loading students", e)
            }
        }
    }

    fun loadStudentsInBatch(studentIds: List<String>) {
        if (studentIds.isEmpty()) {
            _batchStudents.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val studentsList = mutableListOf<User>()
                studentIds.chunked(10).forEach { chunk ->
                    val snapshot = firestore.collection("users")
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    studentsList.addAll(snapshot.documents.mapNotNull {
                        it.toObject(User::class.java)?.copy(id = it.id)
                    })
                }
                _batchStudents.value = studentsList
            } catch (e: Exception) {
                Log.e("FacultyVM", "Error loading batch students", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveAttendance(batchId: String, studentAttendance: Map<String, Boolean>) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                val startOfDay = com.google.firebase.Timestamp(today.time)
                
                // Check if attendance already taken today for this batch
                val existing = firestore.collection("attendance")
                    .whereEqualTo("batchId", batchId)
                    .whereGreaterThanOrEqualTo("date", startOfDay)
                    .get()
                    .await()
                
                if (!existing.isEmpty) {
                    Log.w("FacultyVM", "Attendance already taken for today")
                    return@launch
                }

                val presentCount = studentAttendance.values.count { it }
                val attendanceRecord = AttendanceRecord(
                    batchId = batchId,
                    facultyId = uid,
                    date = com.google.firebase.Timestamp.now(),
                    studentAttendance = studentAttendance,
                    totalStudents = studentAttendance.size,
                    presentCount = presentCount
                )
                
                // Transaction to update attendance record AND student stats
                firestore.runTransaction { transaction ->
                    // 1. Create the attendance record
                    val attendanceRef = firestore.collection("attendance").document()
                    transaction.set(attendanceRef, attendanceRecord)
                    
                    // 2. Update each student's total and present counts
                    studentAttendance.forEach { (studentId, isPresent) ->
                        val studentRef = firestore.collection("users").document(studentId)
                        transaction.update(studentRef, "totalClasses", FieldValue.increment(1))
                        if (isPresent) {
                            transaction.update(studentRef, "classesPresent", FieldValue.increment(1))
                        }
                    }
                }.await()
                
                Log.d("FacultyVM", "Attendance saved and student stats updated successfully")
            } catch (e: Exception) {
                Log.e("FacultyVM", "Error saving attendance", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createBatch(name: String, department: String, year: String, studentIds: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val batchDoc = firestore.collection("batches").document()
                val batchData = hashMapOf(
                    "name" to name,
                    "department" to department,
                    "year" to year,
                    "createdBy" to uid,
                    "studentIds" to studentIds,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batchDoc.set(batchData).await()
                loadAllStudents()
            } catch (e: Exception) {
                Log.e("FacultyVM", "FAILED to create batch", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("batches").document(batchId).delete().await()
            } catch (e: Exception) {
                Log.e("FacultyVM", "Delete failed", e)
            }
        }
    }

    fun resolveEmergency(emergencyId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("emergencies").document(emergencyId)
                    .update(mapOf("status" to "resolved", "resolvedAt" to FieldValue.serverTimestamp()))
                    .await()
            } catch (e: Exception) {}
        }
    }

    fun updateComplaintStatus(id: String, status: String) {
        viewModelScope.launch {
            firestore.collection("complaints").document(id).update("status", status).await()
        }
    }

    fun updateIssueStatus(id: String, status: String) {
        viewModelScope.launch {
            val data = mutableMapOf<String, Any>("status" to status)
            if (status == "resolved") data["resolvedAt"] = FieldValue.serverTimestamp()
            firestore.collection("infrastructure").document(id).update(data).await()
        }
    }

    private fun updateDashboardStats() {
        val active = _assignedEmergencies.value.count { it.status == "active" }
        val pending = _assignedIssues.value.count { it.status != "resolved" }
        _dashboardStats.value = FacultyDashboardStats(
            assignedIncidents = active,
            pendingIssues = pending
        )
    }

    private fun stopListening() {
        batchesListener?.remove()
        complaintsListener?.remove()
        emergenciesListener?.remove()
        issuesListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) }
        stopListening()
    }

    fun loadDashboardData() { /* Handled by AuthListener */ }
    fun loadInfrastructureIssues() { /* Handled by AuthListener */ }
}
