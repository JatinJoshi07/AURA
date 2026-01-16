package com.aura.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.Emergency
import com.aura.models.User
import com.aura.utils.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val emergenciesCollection = firestore.collection("emergencies")
    private val usersCollection = firestore.collection("users")
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _emergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val emergencies: StateFlow<List<Emergency>> = _emergencies.asStateFlow()

    private val _userEmergencies = MutableStateFlow<List<Emergency>>(emptyList())
    val userEmergencies: StateFlow<List<Emergency>> = _userEmergencies.asStateFlow()

    private val _activeEmergency = MutableStateFlow<Emergency?>(null)
    val activeEmergency: StateFlow<Emergency?> = _activeEmergency.asStateFlow()

    private var emergenciesListener: ListenerRegistration? = null
    private var userEmergenciesListener: ListenerRegistration? = null

    init {
        startListeningToEmergencies()
        auth.addAuthStateListener { 
            startListeningToUserEmergencies()
        }
        if (auth.currentUser != null) {
            startListeningToUserEmergencies()
        }
    }

    private fun startListeningToEmergencies() {
        emergenciesListener?.remove()
        emergenciesListener = emergenciesCollection
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Emergency::class.java)?.copy(id = doc.id)
                }?.sortedWith(
                    compareByDescending<Emergency> { it.timestamp?.seconds ?: Long.MAX_VALUE }
                        .thenByDescending { it.id }
                ) ?: emptyList()

                _emergencies.value = list
            }
    }

    private fun startListeningToUserEmergencies() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _userEmergencies.value = emptyList()
            _activeEmergency.value = null
            userEmergenciesListener?.remove()
            return
        }
        
        userEmergenciesListener?.remove()
        userEmergenciesListener = emergenciesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Emergency::class.java)?.copy(id = doc.id)
                }?.sortedWith(
                    compareByDescending<Emergency> { it.timestamp?.seconds ?: Long.MAX_VALUE }
                        .thenByDescending { it.id }
                ) ?: emptyList()

                _userEmergencies.value = list
                _activeEmergency.value = list.firstOrNull { it.status == "active" }
            }
    }

    @SuppressLint("MissingPermission")
    fun createEmergency(type: String, description: String) {
        viewModelScope.launch {
            try {
                val firebaseUser = auth.currentUser ?: return@launch
                
                // Get location with timeout
                val location = withTimeoutOrNull(3000) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                } ?: fusedLocationClient.lastLocation.await() ?: LocationService.currentLocation.value

                val geoPoint = location?.let { GeoPoint(it.latitude, it.longitude) }
                
                // Fetch user data
                val user = try {
                    usersCollection.document(firebaseUser.uid).get().await().toObject(User::class.java)
                } catch (e: Exception) { null }

                val emergency = Emergency(
                    userId = firebaseUser.uid,
                    userName = user?.name ?: firebaseUser.displayName ?: "AURA User",
                    type = type,
                    status = "active",
                    location = geoPoint,
                    description = description,
                    priority = when(type.lowercase()) {
                        "medical", "fire" -> "critical"
                        "security", "harassment" -> "high"
                        else -> "medium"
                    }
                )

                emergenciesCollection.add(emergency).await()
                sendEmergencyNotifications(user, geoPoint, description)
            } catch (e: Exception) {
                Log.e("EmergencyViewModel", "Error: ${e.message}")
            }
        }
    }

    private fun sendEmergencyNotifications(user: User?, location: GeoPoint?, message: String) {
        if (user == null) return
        viewModelScope.launch {
            user.trustedFriends.forEach { friendId ->
                try {
                    val friendDoc = firestore.collection("users").document(friendId).get().await()
                    val friendEmail = friendDoc.getString("email")
                    if (friendEmail != null) {
                        firestore.collection("mail").add(mapOf(
                            "to" to friendEmail,
                            "message" to mapOf(
                                "subject" to "AURA SOS ALERT!",
                                "text" to "${user.name} needs help! Location: https://maps.google.com/?q=${location?.latitude},${location?.longitude}"
                            )
                        ))
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun updateEmergencyStatus(id: String, status: String) {
        viewModelScope.launch {
            emergenciesCollection.document(id).update(mapOf("status" to status, "resolvedAt" to FieldValue.serverTimestamp()))
        }
    }
}
