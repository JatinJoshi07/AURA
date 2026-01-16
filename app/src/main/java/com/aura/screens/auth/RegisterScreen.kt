package com.aura.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aura.components.GradientButton
import com.aura.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var enrollment by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var batchYear by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loginResult by authViewModel.loginResult.collectAsState()

    // Handle registration result
    LaunchedEffect(loginResult) {
        loginResult?.let { result ->
            isLoading = false
            if (result.isSuccess) {
                // Navigation is handled automatically in MainActivity based on auth state
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
            }
        }
    }

    val departments = listOf(
        "Computer Science", "Electronics", "Mechanical", "Civil",
        "Electrical", "Chemical", "Biotechnology", "MBA", "MCA"
    )

    val genders = listOf("Male", "Female", "Other")
    val batchYears = (2018..2025).map { it.toString() }

    var deptExpanded by remember { mutableStateOf(false) }
    var batchExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Create Account",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Join the AURA community for a smarter and safer campus experience.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Personal Info Section
                RegistrationSection(title = "Personal Details") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = enrollment,
                            onValueChange = { enrollment = it },
                            label = { Text("Enrollment") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    }
                }

                // Academic Info Section
                RegistrationSection(title = "Academic Details") {
                    ExposedDropdownMenuBox(
                        expanded = deptExpanded,
                        onExpandedChange = { deptExpanded = !deptExpanded }
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            label = { Text("Department") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptExpanded) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = deptExpanded,
                            onDismissRequest = { deptExpanded = false }
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept) },
                                    onClick = {
                                        department = dept
                                        deptExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Batch Dropdown
                        ExposedDropdownMenuBox(
                            expanded = batchExpanded,
                            onExpandedChange = { batchExpanded = !batchExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = batchYear,
                                onValueChange = {},
                                label = { Text("Batch") },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = batchExpanded) },
                                shape = RoundedCornerShape(16.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = batchExpanded,
                                onDismissRequest = { batchExpanded = false }
                            ) {
                                batchYears.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year) },
                                        onClick = {
                                            batchYear = year
                                            batchExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Gender Dropdown
                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = !genderExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                label = { Text("Gender") },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                shape = RoundedCornerShape(16.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false }
                            ) {
                                genders.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = {
                                            gender = g
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Security Section
                RegistrationSection(title = "Security") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }

                GradientButton(
                    text = if (isLoading) "Creating Account..." else "Create Account",
                    onClick = {
                        errorMessage = validateRegistrationDetailed(
                            name, email, enrollment, mobile, department, batchYear, gender, password, confirmPassword
                        )
                        if (errorMessage == null) {
                            isLoading = true
                            authViewModel.registerStudent(
                                name, email, enrollment, mobile, department, batchYear.toInt(), gender, password
                            )
                        }
                    },
                    enabled = !isLoading
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account?", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)))
                    TextButton(onClick = { navController.navigate("login") }) {
                        Text("Sign In", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        )
        content()
    }
}

private fun validateRegistrationDetailed(
    name: String,
    email: String,
    enrollment: String,
    mobile: String,
    department: String,
    batchYear: String,
    gender: String,
    password: String,
    confirmPassword: String
): String? {
    if (name.isBlank()) return "Please enter your full name"
    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter a valid email address"
    if (enrollment.isBlank()) return "Please enter your enrollment number"
    if (mobile.length != 10) return "Mobile number must be exactly 10 digits"
    if (department.isBlank()) return "Please select your department"
    if (batchYear.isBlank()) return "Please select your batch year"
    if (gender.isBlank()) return "Please select your gender"
    if (password.length < 6) return "Password must be at least 6 characters"
    if (password != confirmPassword) return "Passwords do not match"
    return null
}
