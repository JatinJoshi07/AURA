package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
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
import com.aura.models.QuizAttempt
import com.aura.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizLeaderboardScreen(
    quizId: String,
    navController: NavController,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val leaderboard by viewModel.leaderboard.collectAsState()

    LaunchedEffect(quizId) {
        viewModel.loadLeaderboard(quizId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (leaderboard.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No attempts yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(leaderboard) { index, attempt ->
                    LeaderboardCard(index + 1, attempt)
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(rank: Int, attempt: QuizAttempt) {
    val cardColor = when (rank) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.1f) // Gold
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f) // Silver
        3 -> Color(0xFFCD7F32).copy(alpha = 0.1f) // Bronze
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(attempt.userName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("${attempt.timeTakenSeconds}s", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${attempt.score}/${attempt.totalPoints}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                if (rank <= 3) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = when(rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        else -> Color(0xFFCD7F32)
                    })
                }
            }
        }
    }
}
