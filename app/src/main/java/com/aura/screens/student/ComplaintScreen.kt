package com.aura.screens.student

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aura.components.*
import com.aura.models.Complaint
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.ComplaintViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val myComplaints by complaintViewModel.myComplaints.collectAsState()
    val isLoading by complaintViewModel.isLoading.collectAsState()
    val errorMsg by complaintViewModel.errorMessage.collectAsState()
    val successMsg by complaintViewModel.successMessage.collectAsState()

    var showNewComplaint by remember { mutableStateOf(false) }
    var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }
    var filterStatus by remember { mutableStateOf("all") }
    val context = LocalContext.current

    LaunchedEffect(currentUser) {
        currentUser?.id?.let { complaintViewModel.loadMyComplaints(it) }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            complaintViewModel.clearMessages()
        }
    }

    LaunchedEffect(successMsg) {
        successMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            complaintViewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Report an Issue",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && myComplaints.isEmpty()) {
            LoadingAnimation()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Stats Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Anonymous Reporting",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${myComplaints.size} Reports Filed",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Your identity is protected by end-to-end encryption.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Filter Section
                Text(
                    "My Reports",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                ScrollableTabRow(
                    selectedTabIndex = listOf("all", "pending", "in_review", "resolved").indexOf(filterStatus).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    listOf("all", "pending", "in_review", "resolved").forEach { status ->
                        Tab(
                            selected = filterStatus == status,
                            onClick = { filterStatus = status },
                            text = {
                                Text(
                                    status.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        )
                    }
                }

                // Complaints list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    val filteredComplaints = if (filterStatus == "all") myComplaints 
                                            else myComplaints.filter { it.status == filterStatus }

                    if (filteredComplaints.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Description,
                                message = "No reports found for this category."
                            )
                        }
                    } else {
                        items(filteredComplaints) { complaint ->
                            ComplaintCard(
                                complaint = complaint,
                                onClick = { selectedComplaint = complaint }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // New Complaint FAB
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
            ExtendedFloatingActionButton(
                onClick = { showNewComplaint = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("File Report") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    // New Complaint Dialog
    if (showNewComplaint) {
        NewComplaintDialog(
            onDismiss = { showNewComplaint = false },
            onSubmit = { title, description, type, category, isAnonymous, evidence ->
                complaintViewModel.submitComplaint(
                    title, description, type, category, isAnonymous, evidence
                )
                showNewComplaint = false
            }
        )
    }

    // Complaint Detail Dialog
    selectedComplaint?.let { complaint ->
        ComplaintDetailDialog(
            complaint = complaint,
            onDismiss = { selectedComplaint = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintCard(
    complaint: Complaint,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (complaint.priority == "high") Color(0x77302141) else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    complaint.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (complaint.status) {
                        "pending" -> MaterialTheme.colorScheme.errorContainer
                        "in_review" -> MaterialTheme.colorScheme.secondaryContainer
                        "resolved" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        complaint.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                complaint.description.take(60) + if (complaint.description.length > 60) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (complaint.isAnonymous) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = "Anonymous",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "Anonymous",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Named",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            complaint.userName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(complaint.timestamp.toDate()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewComplaintDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, Boolean, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("complaint") }
    var selectedCategory by remember { mutableStateOf("safety") }
    var isAnonymous by remember { mutableStateOf(true) }
    var evidence by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { evidence = evidence + it }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { evidence = evidence + it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize(),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("New Issue Report", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                        },
                        actions = {
                            TextButton(
                                onClick = {
                                    if (title.isNotBlank() && description.isNotBlank()) {
                                        onSubmit(title, description, selectedType, selectedCategory, isAnonymous, evidence.map { it.toString() })
                                    }
                                },
                                enabled = title.isNotBlank() && description.isNotBlank()
                            ) {
                                Text("Submit", fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Type & Category
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Safety", "Facilities", "Academic").forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory.equals(cat, ignoreCase = true),
                                        onClick = { selectedCategory = cat.lowercase() },
                                        label = { Text(cat) }
                                    )
                                }
                            }
                        }

                        // Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Issue Title") },
                            placeholder = { Text("Brief summary of the issue") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Detailed Description") },
                            placeholder = { Text("What happened? When? Where?") },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Attachments
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Evidence & Photos", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        if (cameraPermissionState.status.isGranted) {
                                            val uri = getTempUri(context)
                                            tempPhotoUri = uri
                                            cameraLauncher.launch(uri)
                                        } else {
                                            cameraPermissionState.launchPermissionRequest()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Camera")
                                }

                                Button(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Gallery")
                                }
                            }

                            if (evidence.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(evidence) { uri ->
                                        Box(modifier = Modifier.size(100.dp)) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            IconButton(
                                                onClick = { evidence = evidence.filter { it != uri } },
                                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Anonymity
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Stay Anonymous", fontWeight = FontWeight.Bold)
                                    Text("Your identity will not be shared with anyone.", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = isAnonymous, onCheckedChange = { isAnonymous = it })
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailDialog(
    complaint: Complaint,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Complaint Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Title
            Text(
                complaint.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            // Description
            Text(
                complaint.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Evidence Images if any
            if (complaint.evidenceUrls.isNotEmpty()) {
                Text("Attachments", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(complaint.evidenceUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Timestamp
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Submitted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "Submitted",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(complaint.timestamp.toDate()),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun getTempUri(context: Context): Uri {
    val tempFile = File.createTempFile("aura_report_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}
