package com.pencil2d.animation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.pencil2d.animation.ui.screens.EditorScreen
import com.pencil2d.animation.ui.screens.HomeScreen
import com.pencil2d.animation.ui.theme.Pencil2DTheme
import com.pencil2d.animation.ui.viewmodel.EditorViewModel
import com.pencil2d.animation.ui.viewmodel.ProjectListViewModel

class MainActivity : ComponentActivity() {

    private val projectListViewModel: ProjectListViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Pencil2DTheme {
                var currentProjectId by remember { mutableStateOf<String?>(null) }

                val projectId = currentProjectId
                if (projectId == null) {
                    HomeScreen(
                        viewModel = projectListViewModel,
                        onOpenProject = { id ->
                            currentProjectId = id
                        }
                    )
                } else {
                    EditorScreen(
                        projectId = projectId,
                        viewModel = editorViewModel,
                        onBack = {
                            currentProjectId = null
                            projectListViewModel.loadProjects()
                        }
                    )
                }
            }
        }
    }
}
