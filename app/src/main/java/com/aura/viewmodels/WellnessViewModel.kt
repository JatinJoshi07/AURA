package com.aura.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.CampusMood
import com.aura.models.WellnessCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class WellnessViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _myWellnessChecks = MutableStateFlow<List<WellnessCheck>>(emptyList())
    val myWellnessChecks: StateFlow<List<WellnessCheck>> = _myWellnessChecks.asStateFlow()

    private val _campusMood = MutableStateFlow(CampusMood())
    val campusMood: StateFlow<CampusMood> = _campusMood.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var myChecksListener: ListenerRegistration? = null
    private var campusMoodListener: ListenerRegistration? = null

    init {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            observeMyWellnessChecks(currentUserId)
        }
        observeCampusMood()
    }

    fun observeMyWellnessChecks(userId: String) {
        myChecksListener?.remove()
        myChecksListener = firestore.collection("wellness_checks")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val checks = snapshot?.documents?.mapNotNull {
                    it.toObject(WellnessCheck::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                _myWellnessChecks.value = checks
            }
    }

    private fun observeCampusMood() {
        // Observe all checks from the last 24 hours for campus mood
        val today = com.google.firebase.Timestamp.now()
        val startOfDay = com.google.firebase.Timestamp(today.seconds - 86400, 0)

        campusMoodListener?.remove()
        campusMoodListener = firestore.collection("wellness_checks")
            .whereGreaterThan("timestamp", startOfDay)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val checks = snapshot?.documents?.mapNotNull {
                    it.toObject(WellnessCheck::class.java)
                } ?: emptyList()

                if (checks.isNotEmpty()) {
                    val total = checks.size
                    val veryPoor = checks.count { it.moodLevel == 1 }
                    val poor = checks.count { it.moodLevel == 2 }
                    val neutral = checks.count { it.moodLevel == 3 }
                    val good = checks.count { it.moodLevel == 4 }
                    val excellent = checks.count { it.moodLevel == 5 }
                    val average = checks.map { it.moodLevel }.average()
                    val positivePercentage = ((good + excellent) * 100 / total)

                    _campusMood.value = CampusMood(
                        averageMood = average,
                        totalChecks = total,
                        veryPoorCount = veryPoor,
                        poorCount = poor,
                        neutralCount = neutral,
                        goodCount = good,
                        excellentCount = excellent,
                        veryPoor = veryPoor.toFloat() / total,
                        poor = poor.toFloat() / total,
                        neutral = neutral.toFloat() / total,
                        good = good.toFloat() / total,
                        excellent = excellent.toFloat() / total,
                        positivePercentage = positivePercentage
                    )
                } else {
                    _campusMood.value = CampusMood()
                }
            }
    }

    fun submitWellnessCheck(moodLevel: Int, note: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val check = WellnessCheck(
                        userId = currentUser.uid,
                        moodLevel = moodLevel,
                        note = note,
                        isAnonymous = true
                    )
                    firestore.collection("wellness_checks").add(check).await()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Keep for manual refresh if needed
    fun loadMyWellnessChecks(userId: String) = observeMyWellnessChecks(userId)
    fun loadCampusMood() = observeCampusMood()

    override fun onCleared() {
        super.onCleared()
        myChecksListener?.remove()
        campusMoodListener?.remove()
    }
}
