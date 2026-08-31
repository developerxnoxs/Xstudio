package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ProjectsDashboardScreen
import com.example.ui.screens.StudioWorkspaceScreen
import com.example.ui.theme.AndroidStudioTheme
import com.example.ui.theme.StudioBackground
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    private val studioViewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBackground
                ) {
                    AndroidStudioApp(viewModel = studioViewModel)
                }
            }
        }
    }
}

@Composable
fun AndroidStudioApp(viewModel: StudioViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "workspace"
    ) {
        composable("workspace") {
            StudioWorkspaceScreen(
                viewModel = viewModel,
                uiState = uiState,
                onNavigateToDashboard = {
                    navController.navigate("dashboard")
                }
            )
        }
        composable("dashboard") {
            ProjectsDashboardScreen(
                projects = uiState.projects,
                onSelectProject = { project ->
                    viewModel.selectProject(project)
                    navController.popBackStack()
                },
                onNewProjectClick = {
                    viewModel.toggleNewProjectDialog(true)
                },
                onOpenGitHubImport = {
                    viewModel.toggleGitHubImportDialog(true)
                },
                onCreateFromTemplate = { template ->
                    viewModel.createProject(
                        template = template,
                        name = template.title.replace(" ", ""),
                        packageName = "com.example.${template.title.lowercase().replace(" ", "")}",
                        description = template.description
                    )
                    navController.popBackStack()
                },
                onDeleteProject = { id ->
                    viewModel.deleteProject(id)
                }
            )
        }
    }
}

