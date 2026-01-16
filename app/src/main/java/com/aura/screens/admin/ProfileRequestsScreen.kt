package com.aura.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aura.components.LoadingAnimation
import com.aura.models.ProfileUpdateRequest
import com.aura.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRequestsScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val requests by adminViewModel.profileRequests.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile Verifications", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingAnimation()
        } else if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No pending verifications", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(requests) { request ->
                    RequestCard(
                        request = request,
                        onApprove = { adminViewModel.approveProfileUpdate(request) },
                        onReject = { adminViewModel.rejectProfileUpdate(request, "Request denied by administrator.") }
                    )
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: ProfileUpdateRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(request.userName, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(request.userRole.uppercase(), style = MaterialTheme.typography.labelSmall)
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            Text("Verification Details:", style = MaterialTheme.typography.labelMedium)
            
            request.requestedChanges.forEach { (key, value) ->
                if (key == "profileImage") {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("New Profile Picture:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = value.toString(),
                            contentDescription = "New Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("${key.replaceFirstChar { it.uppercase() }}: ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        Text(value.toString(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}
