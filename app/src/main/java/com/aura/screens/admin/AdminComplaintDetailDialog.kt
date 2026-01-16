package com.aura.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.models.Complaint
import com.aura.models.Faculty
import com.aura.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintDetailDialog(
    complaint: Complaint,
    facultyList: List<Faculty>,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFaculty by remember { mutableStateOf<Faculty?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(complaint.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(complaint.description)
                
                HorizontalDivider()
                
                Text("Assign to Faculty", style = MaterialTheme.typography.titleSmall)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedFaculty?.name ?: "Select Faculty",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        facultyList.forEach { faculty ->
                            DropdownMenuItem(
                                text = { Text(faculty.name + " (" + faculty.department + ")") },
                                onClick = {
                                    selectedFaculty = faculty
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedFaculty?.let { onAssign(it.id) } },
                enabled = selectedFaculty != null
            ) {
                Text("Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
