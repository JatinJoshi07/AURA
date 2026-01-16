package com.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aura.screens.auth.FacultyRegisterScreen
import com.aura.screens.auth.LoginScreen
import com.aura.screens.auth.RegisterScreen
import com.aura.screens.admin.*
import com.aura.screens.faculty.*
import com.aura.screens.student.*
import com.aura.screens.ProfileScreen
import com.aura.ui.theme.AURATheme
import com.aura.utils.PermissionsManager
import com.aura.viewmodels.AuthViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissionsManager.updatePermissions(permissions)
        }

        permissionsManager = PermissionsManager(this, permissionLauncher)

        setContent {
            AURATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuraApp(permissionsManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AuraApp(permissionsManager: PermissionsManager) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionsManager.requestAllPermissions()
        isInitialized = true
    }

    // Role-based navigation logic
    LaunchedEffect(isLoggedIn, currentUser, currentRoute) {
        if (isLoggedIn && currentUser != null) {
            val destination = when (currentUser?.role) {
                "admin" -> "admin_dashboard"
                "faculty" -> "faculty_dashboard"
                else -> "student_dashboard"
            }
            
            // Only redirect if we are on an auth screen
            val isAuthScreen = currentRoute == "register" || currentRoute == "login" || currentRoute == "faculty_register"
            if (isAuthScreen || currentRoute == null) {
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            }
        } else if (isInitialized && !isLoggedIn) {
            // Redirect to login if not logged in and on a protected screen
            val isAuthScreen = currentRoute == "register" || currentRoute == "login" || currentRoute == "faculty_register"
            if (!isAuthScreen && currentRoute != null) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "login"
            ) {
                // Auth Screens
                composable("login") { LoginScreen(navController, authViewModel) }
                composable("register") { RegisterScreen(navController, authViewModel) }
                composable("faculty_register") { FacultyRegisterScreen(navController, authViewModel) }

                // Dashboards
                composable("student_dashboard") { StudentDashboard(navController, authViewModel) }
                composable("faculty_dashboard") { FacultyDashboard(navController, authViewModel) }
                composable("admin_dashboard") { AdminDashboard(navController, authViewModel) }

                // Universal Screens
                composable("profile") { ProfileScreen(navController, authViewModel) }

                // Student Features
                composable("emergency") { EmergencyScreen(navController, authViewModel, permissionsManager) }
                composable("all_emergencies") { AllEmergenciesScreen(navController) }
                composable("user_emergencies") { UserEmergenciesScreen(navController) }
                composable("pink_shield") { PinkShieldScreen(navController, authViewModel, permissionsManager) }
                composable("project_hub") { ProjectHubScreen(navController, authViewModel) }
                composable("complaint") { ComplaintScreen(navController, authViewModel) }
                composable("wellness") { WellnessScreen(navController, authViewModel) }
                composable("gemini_companion") { GeminiCompanionScreen(navController) }

                // Faculty/Admin Features
                composable("incident_dashboard") { IncidentDashboard(navController, authViewModel) }
                composable("infrastructure_management") { InfrastructureManagement(navController, authViewModel) }
                composable("student_complaints") { StudentComplaintsScreen(navController, authViewModel) }
                composable("attendance_report") { AttendanceReportScreen(navController) }
                composable("manage_faculty") { ManageFaculty(navController, authViewModel) }
                composable("broadcast") { BroadcastScreen(navController, authViewModel) }
                
                composable("analytics") { AnalyticsScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
                composable("profile_requests") { ProfileRequestsScreen(navController) }
            }

            // Profile Loading Overlay
            if (isLoggedIn && currentUser == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading profile...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
