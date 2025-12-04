package com.laylarodas.simplejournal.viewmodel

/**
 * Gestiona el formulario de creación/edición de entradas.
 *
 * MODOS DE OPERACIÓN:
 * - entryId = null: Modo creación (nueva entrada).
 * - entryId = "abc": Modo edición (carga entrada existente de Firestore).
 *
 * Valida datos, llama al repositorio y expone flags para la UI.
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EntryDetailViewModel(
    private val repository: JournalRepository,
    private val authManager: AuthManager,
    private val entryId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState.asStateFlow()

    // Guardamos la entrada original para mantener el timestamp al editar
    private var originalEntry: JournalEntry? = null

    init {
        // Si hay un entryId, cargamos la entrada existente
        if (!entryId.isNullOrBlank()) {
            loadEntry(entryId)
        }
    }

    /**
     * Carga una entrada existente de Firestore.
     *
     * Flujo:
     * 1. Mostramos loading.
     * 2. Obtenemos las entradas del usuario y buscamos la que coincide con el ID.
     * 3. Si la encontramos, llenamos el estado con sus datos.
     * 4. Si no existe, mostramos error.
     */
    private fun loadEntry(id: String) {
        val userId = authManager.currentUserId()
        if (userId == null) {
            _uiState.update { it.copy(message = "Sign in to edit entries.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            runCatching {
                // Obtenemos el primer snapshot y buscamos la entrada
                val entries = repository.observeEntries(userId).first()
                entries.find { it.id == id }
            }.onSuccess { entry ->
                if (entry != null) {
                    originalEntry = entry
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = entry.title,
                            content = entry.content,
                            isEditing = true,
                            shouldPopulateFields = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Entry not found."
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = throwable.localizedMessage ?: "Could not load entry."
                    )
                }
            }
        }
    }

    /**
     * Llamado por la Activity después de poblar los campos de texto.
     * Evita que se sobreescriban los cambios del usuario.
     */
    fun onFieldsPopulated() {
        _uiState.update { it.copy(shouldPopulateFields = false) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, showTitleError = false) }
    }

    fun updateContent(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    /**
     * Guarda la entrada en Firestore.
     *
     * Flujo:
     * 1. Validar que el título no esté vacío.
     * 2. Obtener el userId del usuario autenticado.
     * 3. Crear un JournalEntry con los datos del formulario.
     * 4. Si es edición (isEditing=true) → updateEntry().
     *    Si es creación → addEntry().
     * 5. Si tiene éxito → cerrar pantalla.
     * 6. Si falla → mostrar error.
     */
    fun saveEntry() {
        val current = _uiState.value

        // Paso 1: Validar título
        if (current.title.isBlank()) {
            _uiState.update {
                it.copy(showTitleError = true, message = null)
            }
            return
        }

        // Paso 2: Verificar sesión
        val userId = authManager.currentUserId()
        if (userId == null) {
            _uiState.update { it.copy(message = "Sign in to save your thoughts.") }
            return
        }

        // Paso 3-6: Guardar en Firestore
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null, closeScreen = false) }

            // Crear la entrada con los datos actuales
            val entry = JournalEntry(
                id = originalEntry?.id ?: "",
                title = current.title.trim(),
                content = current.content.trim(),
                timestamp = originalEntry?.timestamp ?: System.currentTimeMillis(),
                userId = userId
            )

            runCatching {
                if (current.isEditing && originalEntry != null) {
                    // Modo edición: actualizar documento existente
                    repository.updateEntry(userId, entry)
                } else {
                    // Modo creación: crear nuevo documento
                    repository.addEntry(userId, entry)
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        message = if (current.isEditing) "Entry updated!" else "Entry saved!",
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

/**
 * Estado de la UI del editor de entradas.
 *
 * @property title             Título actual en el campo de texto.
 * @property content           Contenido actual en el campo de texto.
 * @property isLoading         True mientras carga una entrada existente.
 * @property isSaving          True mientras guarda en Firestore.
 * @property isEditing         True si estamos editando (no creando).
 * @property showTitleError    True si el título está vacío y se intentó guardar.
 * @property shouldPopulateFields True cuando la Activity debe llenar los campos con los datos cargados.
 * @property message           Mensaje temporal para Snackbar.
 * @property closeScreen       True cuando la Activity debe cerrarse.
 */
data class EntryDetailUiState(
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val showTitleError: Boolean = false,
    val shouldPopulateFields: Boolean = false,
    val message: String? = null,
    val closeScreen: Boolean = false
)

/**
 * Factory para crear EntryDetailViewModel con sus dependencias.
 *
 * @param entryId ID de la entrada a editar (null para crear nueva).
 */
class EntryDetailViewModelFactory(
    private val repository: JournalRepository,
    private val authManager: AuthManager,
    private val entryId: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryDetailViewModel::class.java)) {
            return EntryDetailViewModel(repository, authManager, entryId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
