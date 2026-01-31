package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.models.Batch
import com.aura.models.Question
import com.aura.models.Quiz
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.FacultyViewModel
import com.aura.viewmodels.QuizViewModel
import com.google.firebase.Timestamp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    facultyViewModel: FacultyViewModel = hiltViewModel(),
    quizViewModel: QuizViewModel = hiltViewModel()
) {
    val batches by facultyViewModel.myBatches.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var selectedBatchId by remember { mutableStateOf("") }
    val questions = remember { mutableStateListOf<Question>() }

    var showAddQuestionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && selectedBatchId.isNotBlank() && questions.isNotEmpty()) {
                                val quiz = Quiz(
                                    title = title,
                                    description = description,
                                    batchId = selectedBatchId,
                                    facultyId = currentUser?.id ?: "",
                                    durationInMinutes = duration.toIntOrNull() ?: 30,
                                    questions = questions.toList(),
                                    startTime = Timestamp.now(), // Simplified for now
                                    endTime = Timestamp(Date(System.currentTimeMillis() + 86400000)) // 24h later
                                )
                                quizViewModel.createQuiz(quiz)
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddQuestionDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Question")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Quiz Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (minutes)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Target Batch", style = MaterialTheme.typography.titleSmall)
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = batches.find { it.id == selectedBatchId }?.name ?: "Select Batch",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    batches.forEach { batch ->
                        DropdownMenuItem(
                            text = { Text(batch.name) },
                            onClick = {
                                selectedBatchId = batch.id
                                expanded = false
                            }
                        )
                    }
                }
            }

            Text("Questions (${questions.size})", style = MaterialTheme.typography.titleLarge)
            questions.forEachIndexed { index, question ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Q${index + 1}: ${question.text}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { questions.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                        Text("Type: ${question.type}", style = MaterialTheme.typography.bodySmall)
                        if (question.type == "MCQ") {
                            question.options.forEach { option ->
                                Text("• $option", color = if (option == question.correctAnswer) Color.Green else Color.Unspecified)
                            }
                        } else {
                            Text("Correct Answer: ${question.correctAnswer}", color = Color.Green)
                        }
                    }
                }
            }
        }
    }

    if (showAddQuestionDialog) {
        AddQuestionDialog(
            onDismiss = { showAddQuestionDialog = false },
            onAdd = { question ->
                questions.add(question)
                showAddQuestionDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestionDialog(onDismiss: () -> Unit, onAdd: (Question) -> Unit) {
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("MCQ") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }
    var option4 by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Question") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Question Text") })
                
                Text("Type")
                Row {
                    FilterChip(selected = type == "MCQ", onClick = { type = "MCQ" }, label = { Text("MCQ") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == "TF", onClick = { type = "TF" }, label = { Text("True/False") })
                }

                if (type == "MCQ") {
                    OutlinedTextField(value = option1, onValueChange = { option1 = it }, label = { Text("Option 1") })
                    OutlinedTextField(value = option2, onValueChange = { option2 = it }, label = { Text("Option 2") })
                    OutlinedTextField(value = option3, onValueChange = { option3 = it }, label = { Text("Option 3") })
                    OutlinedTextField(value = option4, onValueChange = { option4 = it }, label = { Text("Option 4") })
                    Text("Select Correct Option")
                    var expandedOptions by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedOptions, onExpandedChange = { expandedOptions = !expandedOptions }) {
                        OutlinedTextField(value = correctAnswer, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOptions) }, modifier = Modifier.menuAnchor())
                        ExposedDropdownMenu(expanded = expandedOptions, onDismissRequest = { expandedOptions = false }) {
                            listOf(option1, option2, option3, option4).filter { it.isNotBlank() }.forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { correctAnswer = opt; expandedOptions = false })
                            }
                        }
                    }
                } else if (type == "TF") {
                    var expandedTF by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedTF, onExpandedChange = { expandedTF = !expandedTF }) {
                        OutlinedTextField(value = correctAnswer, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTF) }, modifier = Modifier.menuAnchor())
                        ExposedDropdownMenu(expanded = expandedTF, onDismissRequest = { expandedTF = false }) {
                            listOf("True", "False").forEach { opt ->
                                DropdownMenuItem(text = { Text(opt) }, onClick = { correctAnswer = opt; expandedTF = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = Question(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    type = type,
                    options = if (type == "MCQ") listOf(option1, option2, option3, option4).filter { it.isNotBlank() } else listOf("True", "False"),
                    correctAnswer = correctAnswer
                )
                onAdd(q)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
