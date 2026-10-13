package com.alchemedia.collagemaker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alchemedia.collagemaker.data.ProjectRepository
import com.alchemedia.collagemaker.ui.editor.EditorScreen
import com.alchemedia.collagemaker.ui.editor.EditorViewModel
import com.alchemedia.collagemaker.ui.home.HomeScreen
import com.alchemedia.collagemaker.ui.home.HomeViewModel
import com.alchemedia.collagemaker.ui.picker.NewCollageScreen
import com.alchemedia.collagemaker.ui.picker.NewCollageViewModel
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object NewCollage

@Serializable
data class Editor(val projectId: String)

@Composable
fun AppNavigation(repository: ProjectRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> {
            val vm = viewModel<HomeViewModel>(factory = viewModelFactory { initializer { HomeViewModel(repository) } })
            HomeScreen(
                viewModel = vm,
                onNewCollage = { navController.navigate(NewCollage) },
                onOpen = { navController.navigate(Editor(it)) },
            )
        }
        composable<NewCollage> {
            val vm = viewModel<NewCollageViewModel>(
                factory = viewModelFactory { initializer { NewCollageViewModel(repository) } },
            )
            NewCollageScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                // Replace the picker with the editor so Back returns to the home screen.
                onCreated = { id ->
                    navController.navigate(Editor(id)) { popUpTo<NewCollage> { inclusive = true } }
                },
            )
        }
        composable<Editor> { entry ->
            val projectId = entry.toRoute<Editor>().projectId
            val vm = viewModel<EditorViewModel>(
                factory = viewModelFactory { initializer { EditorViewModel(repository, projectId) } },
            )
            EditorScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
