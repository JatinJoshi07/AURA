package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Unpublished
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
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    quizViewModel: QuizViewModel = hiltViewModel()
) {
    val quizzes by quizViewModel.quizzes.collectAsState()
    val isLoading by quizViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        quizViewModel.loadFacultyQuizzes()
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create_quiz") }) {
                Icon(Icons.Default.Add, contentDescription = "Create Quiz")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (quizzes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No quizzes created yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quizzes) { quiz ->
                    QuizFacultyCard(
                        quiz = quiz,
                        onLeaderboardClick = { navController.navigate("quiz_leaderboard/${quiz.id}") },
                        onToggleActive = { quizViewModel.toggleQuizActive(quiz.id, !quiz.isActive) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuizFacultyCard(
    quiz: Quiz, 
    onLeaderboardClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quiz.title, 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (quiz.isActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (quiz.isActive) "ACTIVE" else "DRAFT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (quiz.isActive) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Text(quiz.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Duration: ${quiz.durationInMinutes} mins", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onToggleActive) {
                    Icon(
                        imageVector = if (quiz.isActive) Icons.Default.Unpublished else Icons.Default.Publish,
                        contentDescription = if (quiz.isActive) "Unpublish" else "Publish",
                        tint = if (quiz.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onLeaderboardClick) {
                    Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard")
                }
            }
        }
    }
}
