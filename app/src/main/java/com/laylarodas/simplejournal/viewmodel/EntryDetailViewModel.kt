package com.laylarodas.simplejournal.viewmodel

/**
 * Gestiona el formulario de creación/edición:
 * valida datos, llama al repositorio y expone flags para habilitar el botón,
 * mostrar errores y cerrar la pantalla cuando termina.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laylarodas.simplejournal.auth.AuthManager
import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.data.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EntryDetailViewModel(
    private val repository: JournalRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, showTitleError = false) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    /**
     * Guarda la entrada en Firestore.
     *
     * Flujo completo:
     * 1. Validar que el título no esté vacío.
     * 2. Obtener el userId del usuario autenticado.
     * 3. Crear un objeto JournalEntry con los datos del formulario.
     * 4. Llamar a repository.addEntry() dentro de una corrutina.
     * 5. Si tiene éxito → mostrar mensaje y marcar closeScreen = true.
     * 6. Si falla → mostrar el error en un Snackbar.
     *
     * La Activity observa closeScreen y llama finish() para volver a Home.
     */
    fun saveEntry() {
        val current = _uiState.value

        // Paso 1: Validar título
        if (current.title.isBlank()) {
            _uiState.update {
                it.copy(
                    showTitleError = true,
                    message = null
                )
            }
            return
        }

        // Paso 2: Verificar sesión
        val userId = authManager.currentUserId()
        if (userId == null) {
            _uiState.update {
                it.copy(message = "Sign in to save your thoughts.")
            }
            return
        }

        // Paso 3-6: Guardar en Firestore
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, closeScreen = false) }

            // Crear la entrada con los datos actuales
            val entry = JournalEntry(
                title = current.title.trim(),
                content = current.content.trim(),
                userId = userId
            )

            runCatching {
                // Llamada al repositorio (suspende hasta que Firestore confirme)
                repository.addEntry(userId, entry)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = "Entry saved!",
                        closeScreen = true
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = throwable.localizedMessage ?: "Could not save your entry. Try again."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Marca el evento de cierre como consumido para que la Activity no se cierre dos veces.
     */
    fun consumeCloseEvent() {
        _uiState.update { it.copy(closeScreen = false) }
    }
}

data class EntryDetailUiState(
    val title: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val showTitleError: Boolean = false,
    val message: String? = null,
    val closeScreen: Boolean = false
)

class EntryDetailViewModelFactory(
    private val repository: JournalRepository,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryDetailViewModel::class.java)) {
            return EntryDetailViewModel(repository, authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

