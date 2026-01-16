package com.aura.viewmodels

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.models.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val tag = "AuthViewModel"
    private val adminEmail = "jatinjoshi9527@gmail.com"

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginResult = MutableStateFlow<Result<FirebaseUser>?>(null)
    val loginResult: StateFlow<Result<FirebaseUser>?> = _loginResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var userListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d(tag, "Auth state changed: user=${user?.uid}, email=${user?.email}")
            _isLoggedIn.value = user != null

            user?.uid?.let { uid ->
                observeUserData(uid)
            } ?: run {
                userListener?.remove()
                _currentUser.value = null
            }
        }
    }

    private fun observeUserData(userId: String) {
        userListener?.remove()
        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to user data: ${error.message}")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)?.copy(id = document.id)

                    val authUser = auth.currentUser
                    val isSpecifiedAdmin = authUser?.email?.trim()?.equals(adminEmail, ignoreCase = true) ?: false

                    if (user != null && isSpecifiedAdmin && user.role != "admin") {
                        // Auto-upgrade specified admin email in DB
                        firestore.collection("users").document(userId).update("role", "admin")
                    }

                    _currentUser.value = user
                    Log.d(tag, "User data updated in real-time: ${user?.name}")
                } else if (document != null && !document.exists()) {
                    // Create default profile if missing
                    createDefaultProfile(userId)
                }
            }
    }

    private fun createDefaultProfile(userId: String) {
        viewModelScope.launch {
            try {
                val authUser = auth.currentUser
                val isSpecifiedAdmin = authUser?.email?.trim()?.equals(adminEmail, ignoreCase = true) ?: false

                val defaultUser = if (isSpecifiedAdmin) {
                    User(id = userId, role = "admin", name = "AURA Administrator", email = adminEmail)
                } else {
                    User(id = userId, role = "student", name = authUser?.displayName ?: "AURA User", email = authUser?.email ?: "")
                }
                firestore.collection("users").document(userId).set(defaultUser).await()
                Log.d(tag, "Created missing profile for $userId")
            } catch (e: Exception) {
                Log.e(tag, "Error creating profile: ${e.message}")
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                Log.d(tag, "Attempting login for: $email")
                _errorMessage.value = null
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                _loginResult.value = Result.success(result.user!!)
            } catch (e: Exception) {
                Log.e(tag, "Login failed: ${e.message}")
                _errorMessage.value = e.message ?: "Authentication failed"
                _loginResult.value = Result.failure(e)
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                _successMessage.value = "Password reset email sent to $email"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send reset email"
            }
        }
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                handleGoogleSignIn(result)
            } catch (e: Exception) {
                Log.e(tag, "Google Sign-In error: ${e.message}")
                _errorMessage.value = "Sign-in failed: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun handleGoogleSignIn(result: GetCredentialResponse) {
        try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            _loginResult.value = Result.success(authResult.user!!)
        } catch (e: Exception) {
            _errorMessage.value = "Auth processing error: ${e.localizedMessage}"
        }
    }

    fun registerStudent(name: String, email: String, enrollment: String, mobile: String, department: String, batchYear: Int, gender: String, password: String) {
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val user = authResult.user ?: throw Exception("Failed to create user")
                val isSpecifiedAdmin = email.trim().equals(adminEmail, ignoreCase = true)

                val userData = User(
                    id = user.uid,
                    name = name,
                    email = email.trim(),
                    enrollmentNumber = enrollment,
                    mobileNumber = mobile,
                    department = department,
                    batchYear = batchYear,
                    gender = gender,
                    role = if (isSpecifiedAdmin) "admin" else "student"
                )

                firestore.collection("users").document(user.uid).set(userData).await()
                _loginResult.value = Result.success(user)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Registration failed"
                _loginResult.value = Result.failure(e)
            }
        }
    }

    fun logout() {
        auth.signOut()
        userListener?.remove()
        _currentUser.value = null
        _isLoggedIn.value = false
        _loginResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}


