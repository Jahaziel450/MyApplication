package com.example.myapplication.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Note
import com.example.myapplication.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class NoteViewModel(
    private val repository: NoteRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val notes: StateFlow<List<Note>> = repository.allNotes
        .combine(_searchQuery) { notes, query ->
            if (query.isBlank()) notes
            else notes.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true) 
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun addNote(title: String, content: String, imageUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val localImageUri = imageUri?.let { 
                if (it.startsWith("content://")) saveImageToInternalStorage(it) else it 
            }
            val newNote = Note(
                title = title,
                content = content,
                date = System.currentTimeMillis(),
                imageUri = localImageUri
            )
            repository.insert(newNote)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentImageUri = note.imageUri
            val finalImageUri = if (currentImageUri != null && currentImageUri.startsWith("content://")) {
                saveImageToInternalStorage(currentImageUri)
            } else {
                currentImageUri
            }
            repository.update(note.copy(
                date = System.currentTimeMillis(),
                imageUri = finalImageUri
            ))
        }
    }

    private fun saveImageToInternalStorage(uriString: String): String? {
        return try {
            val context = getApplication<Application>()
            val uri = Uri.parse(uriString)
            
            if (uri.scheme == "file") return uriString

            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(note)
        }
    }

    suspend fun getNoteById(id: Long): Note? {
        return repository.getNoteById(id)
    }
}

class NoteViewModelFactory(
    private val repository: NoteRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
