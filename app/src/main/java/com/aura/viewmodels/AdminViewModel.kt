package com.aura.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val tag = "AdminViewModel"
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()

    private val _recentEmergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val recentEmergencies: StateFlow<List<Emergency>> = _recentEmergencies.asStateFlow()

    private val _recentComplaints = MutableStateFlow<List<Complaint>>(emptyList())
    val recentComplaints: StateFlow<List<Complaint>> = _recentComplaints.asStateFlow()

    private val _infrastructureIssues = MutableStateFlow<List<InfrastructureIssue>>(emptyList())
    val infrastructureIssues: StateFlow<List<InfrastructureIssue>> = _infrastructureIssues.asStateFlow()

    private val _facultyList = MutableStateFlow<List<Faculty>>(emptyList())
    val facultyList: StateFlow<List<Faculty>> = _facultyList.asStateFlow()

    private val _userList = MutableStateFlow<List<User>>(emptyList())
    val userList: StateFlow<List<User>> = _userList.asStateFlow()

    private val _broadcasts = MutableStateFlow<List<Broadcast>>(emptyList())
    val broadcasts: StateFlow<List<Broadcast>> = _broadcasts.asStateFlow()

    private val _settings = MutableStateFlow(mapOf<String, Any>())
    val settings: StateFlow<Map<String, Any>> = _settings.asStateFlow()

    private val _profileRequests = MutableStateFlow<List<ProfileUpdateRequest>>(emptyList())
    val profileRequests: StateFlow<List<ProfileUpdateRequest>> = _profileRequests.asStateFlow()

    private val _systemHealth = MutableStateFlow(SystemHealth())
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()

    private val _adminLocationAddress = MutableStateFlow<String?>(null)
    val adminLocationAddress: StateFlow<String?> = _adminLocationAddress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var emergenciesListener: ListenerRegistration? = null
    private var complaintsListener: ListenerRegistration? = null
    private var facultyListener: ListenerRegistration? = null
    private var studentsListener: ListenerRegistration? = null
    private var broadcastsListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var infraListener: ListenerRegistration? = null

    init {
        initializeDatabaseSchema()
        startListeningToData()
        loadSettings()
        monitorSystemHealth()
        fetchAdminLocationAddress()
    }

    @SuppressLint("MissingPermission")
    private fun fetchAdminLocationAddress() {
        viewModelScope.launch {
            try {
                val result = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
                
                result?.let { location ->
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addressString = addresses[0].getAddressLine(0) ?: "Address not found"
                        _adminLocationAddress.value = addressString
                    } else {
                        _adminLocationAddress.value = "${location.latitude}, ${location.longitude}"
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error fetching admin address: ${e.message}")
                _adminLocationAddress.value = "Location services unavailable"
            }
        }
    }

    private fun monitorSystemHealth() {
        viewModelScope.launch {
            while (true) {
                try {
                    val dbStart = System.currentTimeMillis()
                    firestore.collection("system_settings").document("metadata").get().await()
                    val dbLatency = System.currentTimeMillis() - dbStart
                    
                    val dbHealth = when {
                        dbLatency < 300 -> 100
                        dbLatency < 800 -> 90
                        dbLatency < 1500 -> 75
                        else -> 50
                    }

                    _systemHealth.value = SystemHealth(
                        serverStatus = "Online",
                        serverHealth = 99,
                        databaseStatus = if (dbLatency < 3000) "Online" else "Slow",
                        databaseHealth = dbHealth,
                        aiStatus = "Online",
                        aiHealth = if (_dashboardStats.value.activeEmergencies > 5) 88 else 99,
                        mapsStatus = "Online",
                        mapsHealth = 100,
                        lastChecked = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    _systemHealth.value = _systemHealth.value.copy(
                        databaseStatus = "Offline",
                        databaseHealth = 0
                    )
                }
                delay(30000)
            }
        }
    }

    private fun initializeDatabaseSchema() {
        viewModelScope.launch {
            try {
                val schema = mapOf(
                    "system_settings" to mapOf("emergencyRadius" to 500, "autoResolveHours" to 24),
                    "users" to mapOf("description" to "User registry"),
                    "emergencies" to mapOf("description" to "SOS alerts"),
                    "complaints" to mapOf("description" to "Student reports"),
                    "infrastructure" to mapOf("description" to "Maintenance tasks"),
                    "broadcasts" to mapOf("description" to "Announcements"),
                    "profile_update_requests" to mapOf("description" to "Identity verification"),
                    "mail" to mapOf("description" to "Email triggers"),
                    "projects" to mapOf("description" to "Collaboration hub"),
                    "wellness_checks" to mapOf("description" to "Sentiment tracking")
                )

                for ((collection, data) in schema) {
                    val docRef = firestore.collection(collection).document("metadata")
                    if (!docRef.get().await().exists()) {
                        docRef.set(data + mapOf("initializedAt" to FieldValue.serverTimestamp())).await()
                    }
                }
                
                // Ensure global configuration exists
                val configRef = firestore.collection("system_settings").document("global_config")
                if (!configRef.get().await().exists()) {
                    configRef.set(mapOf(
                        "emergencyRadius" to 500,
                        "autoResolveEnabled" to true,
                        "notificationPriority" to true,
                        "aiSensitivity" to 0.7,
                        "voiceTriggerEnabled" to true,
                        "analyzeComplaintsWithAI" to true,
                        "maintenanceAutoAssign" to false,
                        "minBudgetForApproval" to 5000,
                        "allowAnonymousComplaints" to true,
                        "forceUserVerification" to false,
                        "dataRetentionMonths" to 6
                    )).await()
                }
            } catch (e: Exception) {
                Log.e(tag, "Self-repair schema error: ${e.message}")
            }
        }
    }

    private fun startListeningToData() {
        // Emergencies
        emergenciesListener = firestore.collection("emergencies")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->
                _recentEmergencies.value = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id == "metadata") null
                    else doc.toObject(Emergency::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                updateDashboardCounts()
            }

        // Students
        studentsListener = firestore.collection("users")
            .whereEqualTo("role", "student")
            .addSnapshotListener { snapshot, _ ->
                _userList.value = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.name } ?: emptyList()
                updateDashboardCounts()
            }

        // Complaints
        complaintsListener = firestore.collection("complaints")
            .addSnapshotListener { snapshot, _ ->
                _recentComplaints.value = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id == "metadata") null
                    else doc.toObject(Complaint::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                updateDashboardCounts()
            }

        // Infra
        infraListener = firestore.collection("infrastructure")
            .addSnapshotListener { snapshot, _ ->
                _infrastructureIssues.value = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id == "metadata") null
                    else doc.toObject(InfrastructureIssue::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                updateDashboardCounts()
            }

        // Faculty
        facultyListener = firestore.collection("users")
            .whereEqualTo("role", "faculty")
            .addSnapshotListener { snapshot, _ ->
                _facultyList.value = snapshot?.documents?.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    Faculty(id = doc.id, name = user?.name ?: "", email = user?.email ?: "", 
                            mobileNumber = user?.mobileNumber ?: "", department = user?.department ?: "", isActive = user?.isActive ?: true)
                } ?: emptyList()
            }

        // Requests
        requestsListener = firestore.collection("profile_update_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                _profileRequests.value = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id == "metadata") null
                    else doc.toObject(ProfileUpdateRequest::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
            }

        // Broadcasts
        broadcastsListener = firestore.collection("broadcasts")
            .addSnapshotListener { snapshot, _ ->
                _broadcasts.value = snapshot?.documents?.mapNotNull { doc ->
                    if (doc.id == "metadata" || doc.id == "init_marker") null
                    else doc.toObject(Broadcast::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
            }
    }

    private fun updateDashboardCounts() {
        _dashboardStats.value = DashboardStats(
            totalUsers = _userList.value.size,
            activeEmergencies = _recentEmergencies.value.count { it.status == "active" },
            pendingComplaints = _recentComplaints.value.count { it.status == "pending" },
            infrastructureIssues = _infrastructureIssues.value.count { it.status == "reported" }
        )
    }

    fun loadDashboardData() = updateDashboardCounts()
    fun loadAllStudents() {}
    fun loadAllEmergencies() {}
    fun loadAllComplaints() {}
    fun loadAllInfrastructureIssues() {}

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try { firestore.collection("users").document(studentId).delete().await() } 
            catch (e: Exception) { Log.e(tag, "Delete error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun loadBroadcasts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("broadcasts").get().await()
                _broadcasts.value = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == "metadata" || doc.id == "init_marker") null
                    else doc.toObject(Broadcast::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
            } catch (e: Exception) { Log.e(tag, "Broadcast error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun sendBroadcast(title: String, message: String, type: String, priority: String, audience: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
                    val userName = userDoc.getString("name") ?: "Admin"
                    val broadcast = Broadcast(title = title, message = message, type = type, priority = priority, 
                                              sentBy = currentUser.uid, sentByName = userName, audience = audience)
                    firestore.collection("broadcasts").add(broadcast).await()
                }
            } catch (e: Exception) { Log.e(tag, "Send error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun loadFacultyList() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("users").whereEqualTo("role", "faculty").get().await()
                _facultyList.value = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    Faculty(id = doc.id, name = user?.name ?: "", email = user?.email ?: "", 
                            mobileNumber = user?.mobileNumber ?: "", department = user?.department ?: "", isActive = user?.isActive ?: true)
                }
            } catch (e: Exception) { Log.e(tag, "Faculty error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun addFaculty(name: String, email: String, mobile: String, department: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid ?: throw Exception("Auth failed")
                val user = User(id = userId, name = name, email = email, mobileNumber = mobile, department = department, role = "faculty")
                firestore.collection("users").document(userId).set(user).await()
                sendEmail(to = email, subject = "AURA Faculty Account", text = "Welcome $name. Your account is ready. Email: $email, Pwd: $password")
            } catch (e: Exception) { Log.e(tag, "Add faculty error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun deleteFaculty(facultyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try { firestore.collection("users").document(facultyId).delete().await() } 
            catch (e: Exception) { Log.e(tag, "Delete error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    private fun sendEmail(to: String, subject: String, text: String) {
        if (to.isBlank()) return
        firestore.collection("mail").add(mapOf("to" to to, "message" to mapOf("subject" to subject, "text" to text)))
    }

    fun approveProfileUpdate(request: ProfileUpdateRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("users").document(request.userId).update(request.requestedChanges).await()
                firestore.collection("profile_update_requests").document(request.id).update(mapOf("status" to "approved", "processedAt" to FieldValue.serverTimestamp())).await()
            } catch (e: Exception) { Log.e(tag, "Approve error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun rejectProfileUpdate(request: ProfileUpdateRequest, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try { firestore.collection("profile_update_requests").document(request.id).update(mapOf("status" to "rejected", "adminNotes" to reason, "processedAt" to FieldValue.serverTimestamp())).await() } 
            catch (e: Exception) { Log.e(tag, "Reject error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    private fun loadSettings() {
        firestore.collection("system_settings").document("global_config")
            .addSnapshotListener { snapshot, _ -> if (snapshot != null && snapshot.exists()) _settings.value = snapshot.data ?: emptyMap() }
    }

    fun saveSettings(newSettings: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.value = true
            try { firestore.collection("system_settings").document("global_config").set(newSettings + mapOf("lastUpdated" to FieldValue.serverTimestamp())).await() } 
            catch (e: Exception) { Log.e(tag, "Save error: ${e.message}") } 
            finally { _isLoading.value = false }
        }
    }

    fun resolveEmergency(emergencyId: String) {
        viewModelScope.launch {
            try { firestore.collection("emergencies").document(emergencyId).update(mapOf("status" to "resolved", "resolvedAt" to FieldValue.serverTimestamp())).await() } 
            catch (e: Exception) { Log.e(tag, "Resolve error: ${e.message}") }
        }
    }

    fun assignComplaint(complaintId: String, facultyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("complaints").document(complaintId)
                    .update(mapOf(
                        "assignedTo" to facultyId,
                        "status" to "in_review"
                    )).await()
            } catch (e: Exception) {
                Log.e(tag, "Assign complaint error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun assignInfrastructureIssue(issueId: String, facultyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("infrastructure").document(issueId)
                    .update(mapOf(
                        "assignedTo" to facultyId,
                        "status" to "assigned"
                    )).await()
            } catch (e: Exception) {
                Log.e(tag, "Assign infra issue error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        emergenciesListener?.remove()
        complaintsListener?.remove()
        facultyListener?.remove()
        studentsListener?.remove()
        broadcastsListener?.remove()
        requestsListener?.remove()
        infraListener?.remove()
    }
}
