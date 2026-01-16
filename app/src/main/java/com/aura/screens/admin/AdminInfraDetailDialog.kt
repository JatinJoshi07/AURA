package com.aura.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.models.InfrastructureIssue
import com.aura.models.Faculty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminInfraDetailDialog(
    issue: InfrastructureIssue,
    facultyList: List<Faculty>,
    onDismiss: () -> Unit,
    onAssign: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedFaculty by remember { mutableStateOf<Faculty?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Maintenance: ${issue.category.uppercase()}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(issue.description)
                Text("Location: ${issue.building}, ${issue.floor}, ${issue.room}", style = MaterialTheme.typography.bodySmall)
                
                HorizontalDivider()
                
                Text("Assign to Maintenance/Faculty", style = MaterialTheme.typography.titleSmall)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedFaculty?.name ?: "Select Staff",
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
                Text("Assign Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
