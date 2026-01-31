package com.aura.screens.faculty

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.models.Batch
import com.aura.models.User
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.FacultyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchManagementScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: FacultyViewModel = hiltViewModel()
) {
    val batches by viewModel.myBatches.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Batches") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.loadAllStudents()
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Batch")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (batches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No batches created yet. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(batches) { batch ->
                        BatchCard(
                            batch = batch,
                            studentCount = batch.studentIds.size,
                            onDelete = { viewModel.deleteBatch(batch.id) },
                            onClick = { 
                                // Navigate to batch details
                                navController.navigate("batch_details/${batch.id}")
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    if (showAddDialog) {
        CreateBatchDialog(
            allStudents = allStudents,
            onDismiss = { showAddDialog = false },
            onCreate = { name, dept, year, selectedIds ->
                viewModel.createBatch(name, dept, year, selectedIds)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun BatchCard(batch: Batch, studentCount: Int, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(batch.name, style = MaterialTheme.typography.titleLarge)
                Text("${batch.department} - ${batch.year}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$studentCount Students", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchDialog(
    allStudents: List<User>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    val selectedStudentIds = remember { mutableStateListOf<String>() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredStudents = allStudents.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.enrollmentNumber.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Batch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Batch Name (e.g. Div D)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dept, onValueChange = { dept = it }, label = { Text("Department") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.fillMaxWidth())
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Select Students", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by name/enrollment") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Default.Search, null) }
                )
                
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(filteredStudents) { student ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (selectedStudentIds.contains(student.id)) selectedStudentIds.remove(student.id)
                                else selectedStudentIds.add(student.id)
                            }
                        ) {
                            Checkbox(
                                checked = selectedStudentIds.contains(student.id),
                                onCheckedChange = { checked ->
                                    if (checked) selectedStudentIds.add(student.id)
                                    else selectedStudentIds.remove(student.id)
                                }
                            )
                            Column {
                                Text(student.name, style = MaterialTheme.typography.bodyMedium)
                                Text(student.enrollmentNumber, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, dept, year, selectedStudentIds.toList()) },
                enabled = name.isNotBlank() && selectedStudentIds.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
