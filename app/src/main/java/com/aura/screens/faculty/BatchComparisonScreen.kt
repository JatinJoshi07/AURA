package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.viewmodels.BatchQuizStats
import com.aura.viewmodels.FacultyViewModel
import com.aura.viewmodels.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchComparisonScreen(
    navController: NavController,
    facultyViewModel: FacultyViewModel = hiltViewModel(),
    quizViewModel: QuizViewModel = hiltViewModel()
) {
    val batches by facultyViewModel.myBatches.collectAsState()
    val batchStats by quizViewModel.batchStats.collectAsState()
    val isLoading by quizViewModel.isLoading.collectAsState()

    LaunchedEffect(batches) {
        if (batches.isNotEmpty()) {
            quizViewModel.loadBatchComparisonData(batches.map { it.id })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(\"Batch Comparison\") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = \"Back\")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (batches.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(\"No batches found.\")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(batches) { batch ->
                    val stats = batchStats[batch.id] ?: BatchQuizStats()
                    ComparisonCard(batch.name, stats)
                }
            }
        }
    }
}

@Composable
fun ComparisonCard(batchName: String, stats: BatchQuizStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(batchName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (stats.averagePercentage / 100).toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(\"Avg: ${String.format(\"%.1f\", stats.averagePercentage)}%\", style = MaterialTheme.typography.bodySmall)
                Text(\"Attempts: ${stats.totalAttempts}\", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
