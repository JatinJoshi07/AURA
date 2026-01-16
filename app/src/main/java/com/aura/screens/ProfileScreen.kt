package com.aura.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aura.models.User
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.ProfileViewModel
import com.aura.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoading by profileViewModel.isLoading.collectAsState()
    val message by profileViewModel.message.collectAsState()
    val userList by adminViewModel.userList.collectAsState()
    val context = LocalContext.current

    // Base Fields
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    
    // Student/Faculty Specific
    var enrollment by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var batchYear by remember { mutableStateOf("") }
    
    // Additional Info
    var skillInput by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var emergencyContacts by remember { mutableStateOf<List<String>>(emptyList()) }
    var contactInput by remember { mutableStateOf("") }
    
    // Image Upload
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            currentUser?.let { user ->
                profileViewModel.uploadProfilePicture(user, it)
            }
        }
    }

    // Friend Search
    var friendSearchQuery by remember { mutableStateOf("") }
    var showFriendSearch by remember { mutableStateOf(false) }
    val filteredStudents = userList.filter { 
        it.role == "student" && 
        it.id != currentUser?.id &&
        (it.name.contains(friendSearchQuery, ignoreCase = true) || it.email.contains(friendSearchQuery, ignoreCase = true))
    }

    LaunchedEffect(currentUser) {
        currentUser?.let {
            name = it.name
            mobile = it.mobileNumber
            enrollment = it.enrollmentNumber
            department = it.department
            batchYear = it.batchYear.toString()
            skills = it.skills
            emergencyContacts = it.emergencyContacts
        }
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            profileViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Profile", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Common Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = currentUser?.profileImage ?: "https://ui-avatars.com/api/?name=${currentUser?.name ?: "User"}",
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        currentUser?.role?.uppercase() ?: "USER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }

            // 2. Identity Section (Dynamic based on Role)
            item {
                SectionTitle("Identity Details")
                IdentityCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        IdentityField(label = "Full Name", value = name, onValueChange = { name = it })
                        IdentityField(label = "Mobile Number", value = mobile, onValueChange = { mobile = it })
                        
                        if (currentUser?.role != "admin") {
                            IdentityField(label = "Department", value = department, onValueChange = { department = it })
                            
                            if (currentUser?.role == "student") {
                                IdentityField(label = "Enrollment Number", value = enrollment, onValueChange = { enrollment = it })
                                IdentityField(label = "Batch Year", value = batchYear, onValueChange = { batchYear = it })
                            }
                            
                            Text(
                                "Note: Identity changes require Admin verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text("Administrator privileged account", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            // 3. Trusted Friends & Emergency Contacts (Student Only)
            if (currentUser?.role == "student") {
                item {
                    SectionTitle("Trusted Friends (AURA Users)")
                    IdentityCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Friends added here will receive app notifications during your SOS.", style = MaterialTheme.typography.bodySmall)
                            
                            Button(
                                onClick = { 
                                    adminViewModel.loadAllStudents()
                                    showFriendSearch = true 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PersonAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Search & Add Friends")
                            }

                            // List of current trusted friends (Placeholder UI)
                            currentUser?.trustedFriends?.forEach { friendId ->
                                // Ideally fetch names from a user map
                                Text("Friend ID: $friendId", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 4. Skills & Phone Contacts
            if (currentUser?.role == "student" || currentUser?.role == "faculty") {
                item {
                    SectionTitle("Emergency Contacts (Phone Numbers)")
                    IdentityCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Emergency Contacts (Safety)
                            Column {
                                Text("Manual Contacts", style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = contactInput,
                                        onValueChange = { contactInput = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Add number...") }
                                    )
                                    IconButton(onClick = { if(contactInput.isNotBlank()) { emergencyContacts = emergencyContacts + contactInput; contactInput = "" } }) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    emergencyContacts.forEach { contact ->
                                        ListItem(
                                            headlineContent = { Text(contact) },
                                            trailingContent = {
                                                IconButton(onClick = { emergencyContacts = emergencyContacts.filter { it != contact } }) {
                                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Logout
            item {
                OutlinedButton(
                    onClick = { authViewModel.logout() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout Account")
                }
            }

            // 6. Save Button
            item {
                Button(
                    onClick = {
                        currentUser?.let { user ->
                            val requestedChanges = mutableMapOf<String, Any>()
                            if (name.trim() != user.name) requestedChanges["name"] = name.trim()
                            if (mobile.trim() != user.mobileNumber) requestedChanges["mobileNumber"] = mobile.trim()
                            if (department.trim() != user.department) requestedChanges["department"] = department.trim()
                            if (enrollment.trim() != user.enrollmentNumber) requestedChanges["enrollmentNumber"] = enrollment.trim()
                            if (batchYear.toIntOrNull() != user.batchYear) requestedChanges["batchYear"] = batchYear.toIntOrNull() ?: user.batchYear

                            val directUpdates = mutableMapOf<String, Any>()
                            if (skills != user.skills) directUpdates["skills"] = skills
                            if (emergencyContacts != user.emergencyContacts) directUpdates["emergencyContacts"] = emergencyContacts

                            if (user.role == "admin") {
                                val allUpdates = directUpdates + requestedChanges
                                if (allUpdates.isNotEmpty()) profileViewModel.updateProfileDirectly(user.id, allUpdates)
                            } else {
                                if (directUpdates.isNotEmpty()) profileViewModel.updateProfileDirectly(user.id, directUpdates)
                                if (requestedChanges.isNotEmpty()) profileViewModel.requestProfileUpdate(user, requestedChanges)
                                if (directUpdates.isEmpty() && requestedChanges.isEmpty()) {
                                    Toast.makeText(context, "No changes detected", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Update Profile")
                }
            }
        }
    }

    // Friend Search Dialog
    if (showFriendSearch) {
        AlertDialog(
            onDismissRequest = { showFriendSearch = false },
            title = { Text("Search Students") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = friendSearchQuery,
                        onValueChange = { friendSearchQuery = it },
                        placeholder = { Text("Search name or email...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Box(modifier = Modifier.heightIn(max = 300.dp)) {
                        LazyColumn {
                            items(filteredStudents) { friend ->
                                ListItem(
                                    headlineContent = { Text(friend.name) },
                                    supportingContent = { Text(friend.email) },
                                    trailingContent = {
                                        val isAdded = currentUser?.trustedFriends?.contains(friend.id) == true
                                        IconButton(onClick = {
                                            val updatedFriends = if (isAdded) {
                                                currentUser?.trustedFriends?.filter { it != friend.id } ?: emptyList()
                                            } else {
                                                (currentUser?.trustedFriends ?: emptyList()) + friend.id
                                            }
                                            profileViewModel.updateProfileDirectly(currentUser?.id ?: "", mapOf("trustedFriends" to updatedFriends))
                                        }) {
                                            Icon(
                                                if (isAdded) Icons.Default.CheckCircle else Icons.Default.Add,
                                                null,
                                                tint = if (isAdded) Color.Green else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFriendSearch = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun AdminOptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun IdentityCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun IdentityField(label: String, value: String, onValueChange: (String) -> Unit = {}, enabled: Boolean = true) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}
