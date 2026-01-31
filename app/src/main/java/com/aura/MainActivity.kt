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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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


    LaunchedEffect(isLoggedIn, currentUser, currentRoute) {
        if (isLoggedIn && currentUser != null) {
            val destination = when (currentUser?.role) {
                "admin" -> "admin_dashboard"
                "faculty" -> "faculty_dashboard"
                else -> "student_dashboard"
            }
            
            val isAuthScreen = currentRoute == "register" || currentRoute == "login" || currentRoute == "faculty_register"
            if (isAuthScreen || currentRoute == null) {
                navController.navigate(destination) {
                    popUpTo("login") { inclusive = true }
                }
            }
        } else if (isInitialized && !isLoggedIn) {
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
                composable("student_quiz") { StudentQuizScreen(navController, authViewModel) }

                // Faculty/Admin Features
                composable("incident_dashboard") { IncidentDashboard(navController, authViewModel) }
                composable("infrastructure_management") { InfrastructureManagement(navController, authViewModel) }
                composable("student_complaints") { StudentComplaintsScreen(navController, authViewModel) }
                composable("attendance_report") { AttendanceReportScreen(navController) }
                composable("manage_faculty") { ManageFaculty(navController, authViewModel) }
                composable("broadcast") { BroadcastScreen(navController, authViewModel) }
                
                // Batch Management
                composable("manage_batches") { BatchManagementScreen(navController, authViewModel) }
                composable(
                    "batch_details/{batchId}",
                    arguments = listOf(navArgument("batchId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
                    BatchDetailScreen(batchId, navController)
                }

                // Quiz System
                composable("quiz_list") { QuizListScreen(navController, authViewModel) }
                composable("create_quiz") { QuizCreationScreen(navController, authViewModel) }
                composable(
                    "quiz_taking/{quizId}",
                    arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                    QuizTakingScreen(quizId, navController, authViewModel)
                }
                composable(
                    "quiz_leaderboard/{quizId}",
                    arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                    QuizLeaderboardScreen(quizId, navController)
                }
                composable(
                    "quiz_result/{quizId}/{score}",
                    arguments = listOf(
                        navArgument("quizId") { type = NavType.StringType },
                        navArgument("score") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                    val score = backStackEntry.arguments?.getInt("score") ?: 0
                    QuizResultScreen(quizId, score, navController)
                }
                
                composable("batch_comparison") { BatchComparisonScreen(navController) }
                composable("take_attendance") { AttendanceManagementScreen(navController) }
                
                composable("analytics") { AnalyticsScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
                composable("profile_requests") { ProfileRequestsScreen(navController) }
                composable("wellness_report") { WellnessReportScreen(navController) }
            }

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
