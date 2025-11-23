package com.laylarodas.simplejournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laylarodas.simplejournal.auth.AuthManager
import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.data.repository.JournalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JournalViewModel(
    private val repository: JournalRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private var entriesJob: Job? = null

    init {
        ensureSession()
        observeAuth()
    }

    private fun ensureSession() {
        viewModelScope.launch {
            if (authManager.currentUserId() == null) {
                runCatching { authManager.signInAnonymously() }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = throwable.localizedMessage ?: "No se pudo iniciar sesión anónima"
                            )
                        }
                    }
            }
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authManager.authState.collect { user ->
                if (user == null) {
                    entriesJob?.cancel()
                    _uiState.update {
                        it.copy(
                            entries = emptyList(),
                            isLoading = false,
                            message = "Inicia sesión para ver tus entradas"
                        )
                    }
                } else {
                    startListening(user.uid)
                }
            }
        }
    }

    private fun startListening(userId: String) {
        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            repository.observeEntries(userId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = throwable.localizedMessage ?: "Error al cargar tus entradas"
                        )
                    }
                }
                .collect { entries ->
                    _uiState.update {
                        it.copy(
                            entries = entries,
                            isLoading = false,
                            message = null
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

class JournalViewModelFactory(
    private val repository: JournalRepository,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(repository, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

