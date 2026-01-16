package com.aura.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.Broadcast
import com.aura.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _broadcasts = MutableStateFlow<List<Broadcast>>(emptyList())
    val broadcasts: StateFlow<List<Broadcast>> = _broadcasts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        setupRealTimeListener()
    }

    private fun setupRealTimeListener() {
        firestore.collection("broadcasts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BroadcastViewModel", "Error listening to broadcasts: ${error.message}")
                    return@addSnapshotListener
                }
                
                val newList = snapshot?.documents?.mapNotNull { 
                    it.toObject(Broadcast::class.java)?.copy(id = it.id)
                } ?: emptyList()

                // Filter out system markers and internal metadata
                val filteredList = newList.filter { it.id != "metadata" && it.id != "init_marker" }

                // Check for new items to send local notification
                // Only send if the new list is different from the current one and not empty
                if (_broadcasts.value.isNotEmpty() && filteredList.isNotEmpty()) {
                    val latestNew = filteredList.first()
                    val currentFirstId = _broadcasts.value.firstOrNull()?.id
                    
                    if (latestNew.id != currentFirstId) {
                        NotificationHelper.showBroadcastNotification(
                            context,
                            "AURA: ${latestNew.title}",
                            latestNew.message
                        )
                    }
                }
                
                _broadcasts.value = filteredList
            }
    }

    fun loadBroadcasts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("broadcasts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { 
                    if (it.id == "metadata" || it.id == "init_marker") null
                    else it.toObject(Broadcast::class.java)?.copy(id = it.id)
                }
                _broadcasts.value = list
            } catch (e: Exception) {
                Log.e("BroadcastViewModel", "Error loading broadcasts: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
