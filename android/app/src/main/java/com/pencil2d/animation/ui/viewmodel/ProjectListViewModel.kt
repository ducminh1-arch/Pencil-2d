package com.pencil2d.animation.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pencil2d.animation.data.model.AnimationProject
import com.pencil2d.animation.data.model.CanvasRatio
import com.pencil2d.animation.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _projects = MutableStateFlow<List<AnimationProject>>(emptyList())
    val projects: StateFlow<List<AnimationProject>> = _projects.asStateFlow()

    private val _selectedFilter = MutableStateFlow("all") // "all", "animation", "image"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val filteredProjects: StateFlow<List<AnimationProject>> = combine(_projects, _selectedFilter) { list, filter ->
        when (filter) {
            "image" -> list.filter { it.isPhotoProject }
            "animation" -> list.filter { !it.isPhotoProject }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProjects()
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _projects.value = repository.getAllProjects()
            _isLoading.value = false
        }
    }

    fun createProject(
        title: String,
        ratio: CanvasRatio,
        fps: Int,
        type: String = AnimationProject.TYPE_ANIMATION,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newProject = repository.createNewProject(title, ratio, fps, type)
            loadProjects()
            onCreated(newProject.id)
        }
    }

    fun importPhotoFromUri(uri: Uri, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val title = "Photo ${System.currentTimeMillis() % 1000}"
                    val newProject = repository.createPhotoProjectFromBitmap(title, bitmap)
                    loadProjects()
                    onCreated(newProject.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            loadProjects()
        }
    }
}
