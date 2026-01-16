package com.aura.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.Project
import com.aura.models.ProjectApplication
import com.aura.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    // Collections
    private val projectsCollection = firestore.collection("projects")
    private val applicationsCollection = firestore.collection("project_applications")
    private val usersCollection = firestore.collection("users")

    // State flows
    private val _allProjects = MutableStateFlow<List<Project>>(emptyList())
    val allProjects: StateFlow<List<Project>> = _allProjects.asStateFlow()

    private val _myProjects = MutableStateFlow<List<Project>>(emptyList())
    val myProjects: StateFlow<List<Project>> = _myProjects.asStateFlow()

    private val _createdProjects = MutableStateFlow<List<Project>>(emptyList())
    val createdProjects: StateFlow<List<Project>> = _createdProjects.asStateFlow()

    private val _joinedProjects = MutableStateFlow<List<Project>>(emptyList())
    val joinedProjects: StateFlow<List<Project>> = _joinedProjects.asStateFlow()

    private val _myApplications = MutableStateFlow<List<ProjectApplication>>(emptyList())
    val myApplications: StateFlow<List<ProjectApplication>> = _myApplications.asStateFlow()

    private val _recommendedProjects = MutableStateFlow<List<Project>>(emptyList())
    val recommendedProjects: StateFlow<List<Project>> = _recommendedProjects.asStateFlow()

    private val _projectCategories = MutableStateFlow<List<String>>(emptyList())
    val projectCategories: StateFlow<List<String>> = _projectCategories.asStateFlow()

    private val _topSkills = MutableStateFlow<List<String>>(emptyList())
    val topSkills: StateFlow<List<String>> = _topSkills.asStateFlow()

    private val _selectedProjectApplications = MutableStateFlow<List<ProjectApplication>>(emptyList())
    val selectedProjectApplications: StateFlow<List<ProjectApplication>> = _selectedProjectApplications.asStateFlow()

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    // Listeners
    private var allProjectsListener: ListenerRegistration? = null
    private var myApplicationsListener: ListenerRegistration? = null
    private var createdProjectsListener: ListenerRegistration? = null
    private var joinedProjectsListener: ListenerRegistration? = null
    private var projectApplicationsListener: ListenerRegistration? = null

    init {
        setupRealTimeListeners()
        loadProjectCategories()
        loadTopSkills()
    }

    private fun setupRealTimeListeners() {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. All Public Projects
        allProjectsListener?.remove()
        allProjectsListener = projectsCollection
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Project::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                _allProjects.value = list
                viewModelScope.launch { loadRecommendedProjects(currentUserId) }
            }

        // 2. User's Applications
        myApplicationsListener?.remove()
        myApplicationsListener = applicationsCollection
            .whereEqualTo("applicantId", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(ProjectApplication::class.java)?.copy(id = it.id)
                }?.sortedByDescending { it.appliedAt } ?: emptyList()
                _myApplications.value = list
            }

        // 3. Created Projects
        createdProjectsListener?.remove()
        createdProjectsListener = projectsCollection
            .whereEqualTo("creatorId", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(Project::class.java)?.copy(id = it.id)
                } ?: emptyList()
                _createdProjects.value = list
                updateMyProjects()
            }

        // 4. Joined Projects
        joinedProjectsListener?.remove()
        joinedProjectsListener = projectsCollection
            .whereArrayContains("teamMembers", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    val p = it.toObject(Project::class.java)?.copy(id = it.id)
                    if (p?.creatorId != currentUserId) p else null
                } ?: emptyList()
                _joinedProjects.value = list
                updateMyProjects()
            }
    }

    private fun updateMyProjects() {
        _myProjects.value = _createdProjects.value + _joinedProjects.value
    }

    fun loadAllProjects() {
        setupRealTimeListeners()
    }

    fun observeProjectApplications(projectId: String) {
        projectApplicationsListener?.remove()
        projectApplicationsListener = applicationsCollection
            .whereEqualTo("projectId", projectId)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull {
                    it.toObject(ProjectApplication::class.java)?.copy(id = it.id)
                } ?: emptyList()
                _selectedProjectApplications.value = list
            }
    }

    private suspend fun loadRecommendedProjects(currentUserId: String?) {
        if (currentUserId == null) return
        try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val userSkills = userDoc.get("skills") as? List<String> ?: emptyList()
            if (userSkills.isEmpty()) return

            val filtered = _allProjects.value.filter { project ->
                val hasMatch = project.requiredSkills.any { skill ->
                    userSkills.any { it.contains(skill, true) || skill.contains(it, true) }
                }
                hasMatch && project.creatorId != currentUserId && !project.teamMembers.contains(currentUserId) && project.status == "open"
            }
            _recommendedProjects.value = filtered.take(5)
        } catch (e: Exception) {}
    }

    private fun loadProjectCategories() {
        _projectCategories.value = listOf("Web Dev", "Mobile", "AI/ML", "IoT", "UI/UX", "Robotics", "Research")
    }

    private fun loadTopSkills() {
        _topSkills.value = listOf("Kotlin", "Python", "React", "Figma", "Firebase", "Java", "Node.js")
    }

    fun createProject(title: String, description: String, department: String, requiredSkills: List<String>, maxTeamSize: Int, deadline: Date? = null, tags: List<String> = emptyList(), visibility: String = "public") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = auth.currentUser ?: return@launch
                val userDoc = usersCollection.document(user.uid).get().await()
                val project = Project(
                    title = title, description = description, creatorId = user.uid, creatorName = userDoc.getString("name") ?: "User",
                    department = userDoc.getString("department") ?: department, requiredSkills = requiredSkills, maxTeamSize = maxTeamSize,
                    deadline = deadline?.let { Timestamp(it) }, tags = tags, visibility = visibility, status = "open", teamMembers = listOf(user.uid)
                )
                projectsCollection.add(project).await()
                usersCollection.document(user.uid).update("totalProjects", FieldValue.increment(1)).await()
                _successMessage.value = "Project created!"
            } catch (e: Exception) { _errorMessage.value = e.message }
            finally { _isLoading.value = false }
        }
    }

    fun applyToProject(projectId: String, message: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = auth.currentUser ?: return@launch
                val application = ProjectApplication(
                    projectId = projectId, applicantId = user.uid, applicantName = "User", // Real name from DB ideally
                    status = "pending", message = message
                )
                applicationsCollection.add(application).await()
                _successMessage.value = "Application sent!"
            } catch (e: Exception) { _errorMessage.value = e.message }
            finally { _isLoading.value = false }
        }
    }

    fun acceptApplication(applicationId: String) {
        viewModelScope.launch {
            try {
                val appDoc = applicationsCollection.document(applicationId).get().await()
                val application = appDoc.toObject(ProjectApplication::class.java) ?: return@launch
                
                applicationsCollection.document(applicationId).update("status", "accepted").await()
                projectsCollection.document(application.projectId).update("teamMembers", FieldValue.arrayUnion(application.applicantId)).await()
            } catch (e: Exception) { _errorMessage.value = e.message }
        }
    }

    fun rejectApplication(applicationId: String) {
        viewModelScope.launch {
            try {
                applicationsCollection.document(applicationId).update("status", "rejected").await()
            } catch (e: Exception) { _errorMessage.value = e.message }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        allProjectsListener?.remove()
        myApplicationsListener?.remove()
        createdProjectsListener?.remove()
        joinedProjectsListener?.remove()
        projectApplicationsListener?.remove()
    }
}
