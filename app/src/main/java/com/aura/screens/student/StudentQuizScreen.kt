package com.aura.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.models.Quiz
import com.aura.models.QuizAttempt
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.QuizViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuizScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    quizViewModel: QuizViewModel = hiltViewModel()
) {
    val availableQuizzes by quizViewModel.availableQuizzes.collectAsState()
    val myAttempts by quizViewModel.attempts.collectAsState()
    val isLoading by quizViewModel.isLoading.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        quizViewModel.loadQuizzesForStudent()
        quizViewModel.loadStudentAttempts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Quizzes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("To-Do") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Completed") })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> ActiveQuizzesList(availableQuizzes, navController)
                    1 -> CompletedQuizzesList(myAttempts)
                }
            }
        }
    }
}

@Composable
fun ActiveQuizzesList(quizzes: List<Quiz>, navController: NavController) {
    if (quizzes.isEmpty()) {
        EmptyState("No active quizzes for your batch.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(quizzes) { quiz ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(quiz.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(quiz.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${quiz.durationInMinutes} mins", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { navController.navigate("quiz_taking/${quiz.id}") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start Quiz")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedQuizzesList(attempts: List<QuizAttempt>) {
    if (attempts.isEmpty()) {
        EmptyState("You haven't completed any quizzes yet.")
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(attempts) { attempt ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(attempt.quizTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            Text("Submitted: ${sdf.format(attempt.submittedAt.toDate())}", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${attempt.score}/${attempt.totalPoints}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Text("Score", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text(message, color = Color.Gray)
        }
    }
}
