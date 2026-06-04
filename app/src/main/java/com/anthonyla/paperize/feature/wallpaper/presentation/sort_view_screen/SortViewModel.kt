package com.anthonyla.paperize.feature.wallpaper.presentation.sort_view_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the sort view screen to hold the folders and wallpapers
 */
class SortViewModel @Inject constructor (): ViewModel() {
    private val _state = MutableStateFlow<SortState>(SortState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SortState()
    )

    fun onEvent(event: SortEvent) {
        when (event) {
            is SortEvent.LoadSortView -> {
                _state.update { it.copy(
                    folders = event.folders,
                    wallpapers = event.wallpapers
                ) }
            }

            is SortEvent.ShiftFolder -> {
                _state.update { currentState ->
                    val currentFolders = currentState.folders.toMutableList()
                    val fromIndex = currentFolders.indexOfFirst { it.folderUri == event.from.key }
                    val toIndex = currentFolders.indexOfFirst { it.folderUri == event.to.key }
                    if (fromIndex != -1 && toIndex != -1) {
                        val movedFolder = currentFolders.removeAt(fromIndex)
                        currentFolders.add(toIndex, movedFolder)
                        currentFolders.forEachIndexed { index, folder ->
                            currentFolders[index] = folder.copy(order = index)
                        }
                    }
                    currentState.copy(folders = currentFolders)
                }
            }

            is SortEvent.ShiftFolderWallpaper -> {
                _state.update { currentState ->
                    val folder = currentState.folders.find { it.folderUri == event.folderId }
                    val currentWallpapers = folder?.wallpapers?.toMutableList() ?: return@update currentState
                    val fromIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.from.key }
                    val toIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.to.key }
                    if (fromIndex != -1 && toIndex != -1) {
                        val movedWallpaper = currentWallpapers.removeAt(fromIndex)
                        currentWallpapers.add(toIndex, movedWallpaper)
                        currentWallpapers.forEachIndexed { index, wallpaper ->
                            currentWallpapers[index] = wallpaper.copy(order = index)
                        }
                    }
                    val updatedFolders = currentState.folders.map { f ->
                        if (f.folderUri == event.folderId) {
                            f.copy(
                                wallpapers = currentWallpapers,
                                coverUri = currentWallpapers.firstOrNull()?.wallpaperUri ?: f.coverUri
                            )
                        } else {
                            f
                        }
                    }
                    currentState.copy(folders = updatedFolders)
                }
            }

            is SortEvent.ShiftWallpaper -> {
                _state.update { currentState ->
                    val currentWallpapers = currentState.wallpapers.toMutableList()
                    val fromIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.from.key }
                    val toIndex = currentWallpapers.indexOfFirst { it.wallpaperUri == event.to.key }
                    if (fromIndex != -1 && toIndex != -1) {
                        val movedWallpaper = currentWallpapers.removeAt(fromIndex)
                        currentWallpapers.add(toIndex, movedWallpaper)
                        currentWallpapers.forEachIndexed { index, wallpaper ->
                            currentWallpapers[index] = wallpaper.copy(order = index)
                        }
                    }
                    currentState.copy(wallpapers = currentWallpapers)
                }
            }

            is SortEvent.Reset -> {
                _state.update { SortState() }
            }

            is SortEvent.SortAlphabetically -> {
                _state.update { currentState ->
                    val sortedFolders = currentState.folders
                        .map { folder ->
                            val sortedFolderWallpapers = folder.wallpapers
                                .sortedBy { it.fileName }
                                .mapIndexed { index, wallpaper -> 
                                    wallpaper.copy(order = index)
                                }
                            folder.copy(wallpapers = sortedFolderWallpapers)
                        }
                        .sortedBy { it.folderName }
                        .mapIndexed { index, folder -> 
                            folder.copy(order = index)
                        }
                    
                    val sortedWallpapers = currentState.wallpapers
                        .sortedBy { it.fileName }
                        .mapIndexed { index, wallpaper -> 
                            wallpaper.copy(order = index)
                        }
                    
                    currentState.copy(
                        folders = sortedFolders,
                        wallpapers = sortedWallpapers
                    )
                }
            }

            is SortEvent.SortAlphabeticallyReverse -> {
                _state.update { currentState ->
                    val sortedFolders = currentState.folders
                        .map { folder ->
                            val sortedFolderWallpapers = folder.wallpapers
                                .sortedByDescending { it.fileName }
                                .mapIndexed { index, wallpaper -> 
                                    wallpaper.copy(order = index)
                                }
                            folder.copy(wallpapers = sortedFolderWallpapers)
                        }
                        .sortedByDescending { it.folderName }
                        .mapIndexed { index, folder -> 
                            folder.copy(order = index)
                        }
                    
                    val sortedWallpapers = currentState.wallpapers
                        .sortedByDescending { it.fileName }
                        .mapIndexed { index, wallpaper -> 
                            wallpaper.copy(order = index)
                        }
                    
                    currentState.copy(
                        folders = sortedFolders,
                        wallpapers = sortedWallpapers
                    )
                }
            }

            is SortEvent.SortByLastModified -> {
                _state.update { currentState ->
                    val sortedFolders = currentState.folders
                        .map { folder ->
                            val sortedFolderWallpapers = folder.wallpapers
                                .sortedBy { it.dateModified }
                                .mapIndexed { index, wallpaper -> 
                                    wallpaper.copy(order = index)
                                }
                            folder.copy(wallpapers = sortedFolderWallpapers)
                        }
                        .sortedBy { it.dateModified }
                        .mapIndexed { index, folder -> 
                            folder.copy(order = index)
                        }
                    
                    val sortedWallpapers = currentState.wallpapers
                        .sortedBy { it.dateModified }
                        .mapIndexed { index, wallpaper -> 
                            wallpaper.copy(order = index)
                        }
                    
                    currentState.copy(
                        folders = sortedFolders,
                        wallpapers = sortedWallpapers
                    )
                }
            }

            is SortEvent.SortByLastModifiedReverse -> {
                _state.update { currentState ->
                    val sortedFolders = currentState.folders
                        .map { folder ->
                            val sortedFolderWallpapers = folder.wallpapers
                                .sortedByDescending { it.dateModified }
                                .mapIndexed { index, wallpaper -> 
                                    wallpaper.copy(order = index)
                                }
                            folder.copy(wallpapers = sortedFolderWallpapers)
                        }
                        .sortedByDescending { it.dateModified }
                        .mapIndexed { index, folder -> 
                            folder.copy(order = index)
                        }
                    
                    val sortedWallpapers = currentState.wallpapers
                        .sortedByDescending { it.dateModified }
                        .mapIndexed { index, wallpaper -> 
                            wallpaper.copy(order = index)
                        }
                    
                    currentState.copy(
                        folders = sortedFolders,
                        wallpapers = sortedWallpapers
                    )
                }
            }
        }
    }
}
