package com.aura.screens.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.models.*
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.QuizViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizTakingScreen(
    quizId: String,
    navController: NavController,
    authViewModel: AuthViewModel,
    quizViewModel: QuizViewModel = hiltViewModel()
) {
    val quizzes by quizViewModel.availableQuizzes.collectAsState()
    val quiz = quizzes.find { it.id == quizId }
    val currentUser by authViewModel.currentUser.collectAsState()

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    var timeLeftSeconds by remember { mutableIntStateOf(0) }
    var isQuizStarted by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    // Start timer logic
    LaunchedEffect(quiz) {
        if (quiz != null && !isQuizStarted) {
            timeLeftSeconds = quiz.durationInMinutes * 60
            isQuizStarted = true
        }
    }

    LaunchedEffect(isQuizStarted) {
        if (isQuizStarted) {
            while (timeLeftSeconds > 0 && !isSubmitted) {
                delay(1000)
                timeLeftSeconds--
            }
            if (timeLeftSeconds <= 0 && !isSubmitted) {
                // Auto-submit
                submitQuiz(quiz, currentUser, answers, (quiz?.durationInMinutes ?: 0) * 60L, quizViewModel, navController)
                isSubmitted = true
            }
        }
    }

    // Prevent accidental back navigation
    BackHandler(enabled = !isSubmitted) {
        // Show dialog or do nothing
    }

    if (quiz == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(quiz.title) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = if (timeLeftSeconds < 60) Color.Red else Color.Unspecified)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatTime(timeLeftSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (timeLeftSeconds < 60) Color.Red else Color.Unspecified,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1).toFloat() / quiz.questions.size },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )

            val question = quiz.questions[currentQuestionIndex]
            
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    text = "Question ${currentQuestionIndex + 1} of ${quiz.questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = question.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(24.dp))

                question.options.forEach { option ->
                    val isSelected = answers[question.id] == option
                    OutlinedCard(
                        onClick = { answers[question.id] = option },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ),
                        border = CardDefaults.outlinedCardBorder(isSelected)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { answers[question.id] = option })
                            Text(text = option, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                    enabled = currentQuestionIndex > 0
                ) {
                    Text("Previous")
                }

                if (currentQuestionIndex == quiz.questions.size - 1) {
                    Button(
                        onClick = {
                            submitQuiz(quiz, currentUser, answers, (quiz.durationInMinutes * 60L - timeLeftSeconds), quizViewModel, navController)
                            isSubmitted = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Submit Quiz")
                    }
                } else {
                    Button(onClick = { currentQuestionIndex++ }) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

private fun submitQuiz(
    quiz: Quiz?,
    user: User?,
    answers: Map<String, String>,
    timeTakenSeconds: Long,
    viewModel: QuizViewModel,
    navController: NavController
) {
    if (quiz == null || user == null) return

    var score = 0
    quiz.questions.forEach { q ->
        if (answers[q.id] == q.correctAnswer) {
            score += q.points
        }
    }

    val attempt = QuizAttempt(
        quizId = quiz.id,
        quizTitle = quiz.title,
        userId = user.id,
        userName = user.name,
        score = score,
        totalPoints = quiz.questions.sumOf { it.points },
        timeTakenSeconds = timeTakenSeconds,
        submittedAt = Timestamp.now(),
        answers = answers
    )

    viewModel.submitAttempt(attempt)
    navController.navigate("quiz_result/${quiz.id}/${score}") {
        popUpTo("quiz_taking/${quiz.id}") { inclusive = true }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
