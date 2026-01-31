package com.aura.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val quizzes: StateFlow<List<Quiz>> = _quizzes.asStateFlow()

    private val _availableQuizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val availableQuizzes: StateFlow<List<Quiz>> = _availableQuizzes.asStateFlow()

    private val _attempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val attempts: StateFlow<List<QuizAttempt>> = _attempts.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val leaderboard: StateFlow<List<QuizAttempt>> = _leaderboard.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _batchStats = MutableStateFlow<Map<String, BatchQuizStats>>(emptyMap())
    val batchStats: StateFlow<Map<String, BatchQuizStats>> = _batchStats.asStateFlow()

    private var quizListener: ListenerRegistration? = null
    private var availableQuizListener: ListenerRegistration? = null
    private var studentBatchListener: ListenerRegistration? = null

    fun createQuiz(quiz: Quiz) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("quizzes").add(quiz).await()
            } catch (e: Exception) {
                Log.e("QuizVM", "Error creating quiz", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleQuizActive(quizId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                firestore.collection("quizzes").document(quizId)
                    .update("isActive", isActive)
                    .await()
            } catch (e: Exception) {
                Log.e("QuizVM", "Error toggling quiz", e)
            }
        }
    }

    fun loadFacultyQuizzes() {
        val uid = auth.currentUser?.uid ?: return
        quizListener?.remove()
        quizListener = firestore.collection("quizzes")
            .whereEqualTo("facultyId", uid)
            .addSnapshotListener { snapshot, _ ->
                _quizzes.value = snapshot?.documents?.mapNotNull { it.toObject(Quiz::class.java)?.copy(id = it.id) } ?: emptyList()
            }
    }

    fun loadQuizzesForStudent() {
        val uid = auth.currentUser?.uid ?: return
        studentBatchListener?.remove()
        availableQuizListener?.remove()
        
        // Listen for batch changes in real-time
        studentBatchListener = firestore.collection("batches")
            .whereArrayContains("studentIds", uid)
            .addSnapshotListener { batchSnapshot, batchError ->
                if (batchError != null) {
                    Log.e("QuizVM", "Error listening to batches", batchError)
                    return@addSnapshotListener
                }
                
                val batchIds = batchSnapshot?.documents?.map { it.id } ?: emptyList()
                
                if (batchIds.isNotEmpty()) {
                    // Start listening for quizzes in those batches
                    availableQuizListener?.remove()
                    availableQuizListener = firestore.collection("quizzes")
                        .whereIn("batchId", batchIds)
                        .whereEqualTo("isActive", true)
                        .addSnapshotListener { quizSnapshot, quizError ->
                            if (quizError != null) {
                                Log.e("QuizVM", "Error listening to quizzes", quizError)
                                return@addSnapshotListener
                            }
                            
                            val now = com.google.firebase.Timestamp.now()
                            val list = quizSnapshot?.documents?.mapNotNull { it.toObject(Quiz::class.java)?.copy(id = it.id) } ?: emptyList()
                            
                            // Filter for active time window
                            // We allow a 5-minute buffer for clock sync issues
                            val bufferMillis = 5 * 60 * 1000L
                            val nowMillis = now.toDate().time
                            
                            _availableQuizzes.value = list.filter { quiz ->
                                val startTime = quiz.startTime.toDate().time
                                val endTime = quiz.endTime.toDate().time
                                nowMillis in (startTime - bufferMillis)..endTime
                            }
                        }
                } else {
                    _availableQuizzes.value = emptyList()
                }
            }
    }

    fun submitAttempt(attempt: QuizAttempt) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("quiz_attempts").add(attempt).await()
            } catch (e: Exception) {
                Log.e("QuizVM", "Error submitting attempt", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLeaderboard(quizId: String) {
        viewModelScope.launch {
            firestore.collection("quiz_attempts")
                .whereEqualTo("quizId", quizId)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("timeTakenSeconds", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("QuizVM", "Leaderboard error: ${e.message}")
                        return@addSnapshotListener
                    }
                    _leaderboard.value = snapshot?.documents?.mapNotNull { it.toObject(QuizAttempt::class.java)?.copy(id = it.id) } ?: emptyList()
                }
        }
    }

    fun loadBatchComparisonData(batchIds: List<String>) {
        if (batchIds.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val statsMap = mutableMapOf<String, BatchQuizStats>()
                
                for (batchId in batchIds) {
                    val quizzesSnapshot = firestore.collection("quizzes")
                        .whereEqualTo("batchId", batchId)
                        .get()
                        .await()
                    
                    val quizIds = quizzesSnapshot.documents.map { it.id }
                    if (quizIds.isEmpty()) {
                        statsMap[batchId] = BatchQuizStats(0.0, 0, 0)
                        continue
                    }

                    var totalScore = 0.0
                    var totalAttempts = 0
                    
                    quizIds.chunked(10).forEach { chunk ->
                        val attemptsSnapshot = firestore.collection("quiz_attempts")
                            .whereIn("quizId", chunk)
                            .get()
                            .await()
                        
                        val attemptsList = attemptsSnapshot.documents.mapNotNull { it.toObject(QuizAttempt::class.java) }
                        totalAttempts += attemptsList.size
                        totalScore += attemptsList.sumOf { (it.score.toDouble() / it.totalPoints) * 100 }
                    }

                    val avgPercentage = if (totalAttempts > 0) totalScore / totalAttempts else 0.0
                    statsMap[batchId] = BatchQuizStats(avgPercentage, totalAttempts, quizIds.size)
                }
                
                _batchStats.value = statsMap
            } catch (e: Exception) {
                Log.e("QuizVM", "Error loading comparison data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStudentAttempts() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firestore.collection("quiz_attempts")
                .whereEqualTo("userId", uid)
                .addSnapshotListener { snapshot, _ ->
                    _attempts.value = snapshot?.documents?.mapNotNull { it.toObject(QuizAttempt::class.java)?.copy(id = it.id) } ?: emptyList()
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        quizListener?.remove()
        availableQuizListener?.remove()
        studentBatchListener?.remove()
    }
}

data class BatchQuizStats(
    val averagePercentage: Double = 0.0,
    val totalAttempts: Int = 0,
    val totalQuizzes: Int = 0
)
