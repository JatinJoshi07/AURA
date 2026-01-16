package com.aura.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.*
import com.aura.models.Project
import com.aura.models.ProjectApplication
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.ProjectViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectHubScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val projects by projectViewModel.allProjects.collectAsState()
    val myProjects by projectViewModel.myProjects.collectAsState()
    val applications by projectViewModel.myApplications.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateProject by remember { mutableStateOf(false) }
    var showProjectDetail by remember { mutableStateOf<Project?>(null) }
    var showApplications by remember { mutableStateOf<Project?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        projectViewModel.loadAllProjects()
    }

    val tabs = listOf("All Projects", "My Projects", "Applications")
    val filteredProjects = when (selectedTab) {
        0 -> projects.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
        1 -> myProjects
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Project Hub",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateProject = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Project")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateProject = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Project") },
                text = { Text("Create Project") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    projectViewModel.loadAllProjects()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (isLoading && !isRefreshing) {
                LoadingAnimation(type = LoadingType.Wave)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Search bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search projects...") },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // Content based on selected tab
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        }, label = "TabContentAnimation"
                    ) { tab ->
                        when (tab) {
                            0 -> AllProjectsList(
                                projects = filteredProjects,
                                onProjectClick = { showProjectDetail = it },
                                onCreateProject = { showCreateProject = true }
                            )
                            1 -> MyProjectsList(
                                projects = filteredProjects,
                                currentUserId = currentUser?.id ?: "",
                                onProjectClick = { showProjectDetail = it },
                                onViewApplications = { 
                                    projectViewModel.observeProjectApplications(it.id)
                                    showApplications = it 
                                }
                            )
                            else -> ApplicationsList(
                                applications = applications,
                                onProjectClick = { /* Handle navigation if needed */ }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Project Dialog
    if (showCreateProject) {
        CreateProjectDialog(
            onDismiss = { showCreateProject = false },
            onCreateProject = { title, description, skills, size, deadlineDate ->
                projectViewModel.createProject(
                    title = title,
                    description = description,
                    department = currentUser?.department ?: "",
                    requiredSkills = skills,
                    maxTeamSize = size,
                    deadline = deadlineDate
                )
                showCreateProject = false
            }
        )
    }

    // Project Detail Dialog
    showProjectDetail?.let { project ->
        ProjectDetailDialog(
            project = project,
            currentUserId = currentUser?.id ?: "",
            onDismiss = { showProjectDetail = null },
            onApply = { message ->
                projectViewModel.applyToProject(project.id, message)
                showProjectDetail = null
            }
        )
    }

    // Applications Dialog
    showApplications?.let { project ->
        val projectApplications by projectViewModel.selectedProjectApplications.collectAsState()
        ProjectApplicationsDialog(
            project = project,
            applications = projectApplications,
            onDismiss = { showApplications = null },
            onAccept = { projectViewModel.acceptApplication(it) },
            onReject = { projectViewModel.rejectApplication(it) }
        )
    }
}

@Composable
fun AllProjectsList(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onCreateProject: () -> Unit
) {
    if (projects.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = "No Projects",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Projects Found",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
            Text(
                "Be the first to create a project!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCreateProject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create First Project")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(projects, key = { it.id }) { project ->
                ProjectCompactCard(
                    project = project,
                    onClick = { onProjectClick(project) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun MyProjectsList(
    projects: List<Project>,
    currentUserId: String,
    onProjectClick: (Project) -> Unit,
    onViewApplications: (Project) -> Unit
) {
    if (projects.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "No Projects",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Projects Created",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
            Text(
                "Create your first project to start collaborating",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(projects, key = { it.id }) { project ->
                MyProjectCard(
                    project = project,
                    currentUserId = currentUserId,
                    onClick = { onProjectClick(project) },
                    onViewApplications = { onViewApplications(project) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun MyProjectCard(
    project: Project,
    currentUserId: String,
    onClick: () -> Unit,
    onViewApplications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                    project.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                ProjectStatusChip(status = project.status)
            }

            Text(
                project.description.take(80) + if (project.description.length > 80) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "${project.teamMembers.size}/${project.maxTeamSize} members",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (project.creatorId == currentUserId) {
                    OutlinedButton(
                        onClick = onViewApplications,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Applications")
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationsList(
    applications: List<ProjectApplication>,
    onProjectClick: (String) -> Unit
) {
    if (applications.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "No Applications",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Applications",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
            Text(
                "You haven't applied to any projects yet",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(applications, key = { it.id }) { application ->
                ApplicationCard(
                    application = application,
                    onClick = { onProjectClick(application.projectId) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ApplicationCard(
    application: ProjectApplication,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    application.applicantName.firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    application.applicantName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    application.message.take(40) + if (application.message.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    application.skills.take(3).forEach { skill ->
                        Text(
                            skill,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (application.status) {
                    "pending" -> MaterialTheme.colorScheme.secondaryContainer
                    "accepted" -> Color(0xFFE8F5E9)
                    "rejected" -> Color(0xFFFFEBEE)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    application.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (String, String, List<String>, Int, Date?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skillInput by remember { mutableStateOf("") }
    var requiredSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var maxTeamSize by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create New Project",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    placeholder = { Text("e.g., Smart Campus App") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe your project idea...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )

                // Skills input
                Column {
                    Text(
                        "Required Skills",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = skillInput,
                            onValueChange = { skillInput = it },
                            placeholder = { Text("Add skill (e.g., Kotlin, UI/UX)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (skillInput.isNotEmpty()) {
                                    requiredSkills = requiredSkills + skillInput.trim()
                                    skillInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Skill")
                        }
                    }

                    // Skills chips
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        requiredSkills.forEach { skill ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    requiredSkills = requiredSkills.filter { it != skill }
                                },
                                label = { Text(skill) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = maxTeamSize,
                    onValueChange = { maxTeamSize = it },
                    label = { Text("Team Size") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && description.isNotEmpty()) {
                        onCreateProject(
                            title,
                            description,
                            requiredSkills,
                            maxTeamSize.toIntOrNull() ?: 5,
                            null
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotEmpty() && description.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailDialog(
    project: Project,
    currentUserId: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    var applicationMessage by remember { mutableStateOf("") }
    var showApplyForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                project.title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(onClick = {}, label = { Text(project.department) })
                    SuggestionChip(onClick = {}, label = { Text("${project.teamMembers.size}/${project.maxTeamSize} members") })
                    SuggestionChip(onClick = {}, label = { Text(project.status.replaceFirstChar { it.uppercase() }) })
                }

                Text(
                    project.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Required Skills
                if (project.requiredSkills.isNotEmpty()) {
                    Column {
                        Text(
                            "Required Skills",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            project.requiredSkills.forEach { skill ->
                                AssistChip(onClick = {}, label = { Text(skill) })
                            }
                        }
                    }
                }

                // Creator Info
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                project.creatorName.firstOrNull()?.uppercase() ?: "C",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Column {
                            Text(
                                "Created by",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                project.creatorName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                // Apply form (if not creator and not already a member)
                if (project.creatorId != currentUserId &&
                    !project.teamMembers.contains(currentUserId) &&
                    project.status == "open") {

                    AnimatedVisibility(
                        visible = showApplyForm,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = applicationMessage,
                                onValueChange = { applicationMessage = it },
                                label = { Text("Application Message") },
                                placeholder = { Text("Why do you want to join this project?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onApply(applicationMessage)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = applicationMessage.isNotEmpty()
                            ) {
                                Text("Submit Application")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (project.creatorId != currentUserId &&
                !project.teamMembers.contains(currentUserId) &&
                project.status == "open") {
                Button(
                    onClick = {
                        if (showApplyForm) {
                            onApply(applicationMessage)
                            onDismiss()
                        } else {
                            showApplyForm = true
                        }
                    },
                    enabled = !showApplyForm || applicationMessage.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Apply")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showApplyForm) "Submit Application" else "Apply to Project")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectApplicationsDialog(
    project: Project,
    applications: List<ProjectApplication>,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Project Applications",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        "${applications.size} applications for ${project.title}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Applications list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(applications, key = { it.id }) { application ->
                    ApplicationDetailCard(
                        application = application,
                        onAccept = { onAccept(application.id) },
                        onReject = { onReject(application.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ApplicationDetailCard(
    application: ProjectApplication,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            application.applicantName.firstOrNull()?.uppercase() ?: "A",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Column {
                        Text(
                            application.applicantName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            "Applied ${formatApplicationTime(application.appliedAt.toDate())}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (application.status) {
                        "pending" -> MaterialTheme.colorScheme.secondaryContainer
                        "accepted" -> Color(0xFFE8F5E9)
                        "rejected" -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        application.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = when (application.status) {
                            "pending" -> MaterialTheme.colorScheme.secondary
                            "accepted" -> Color(0xFF2E7D32)
                            "rejected" -> Color(0xFFD32F2F)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Text(
                application.message,
                style = MaterialTheme.typography.bodyMedium
            )

            // Skills
            if (application.skills.isNotEmpty()) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    application.skills.forEach { skill ->
                        AssistChip(onClick = {}, label = { Text(skill) })
                    }
                }
            }

            // Actions (only for pending applications)
            if (application.status == "pending") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject")
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Accept")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accept")
                    }
                }
            }
        }
    }
}

private fun formatApplicationTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 7 -> "$days days ago"
        else -> java.text.SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
