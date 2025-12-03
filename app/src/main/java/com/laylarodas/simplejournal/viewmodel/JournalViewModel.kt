package com.laylarodas.simplejournal.viewmodel

/**
 * Mantiene el estado de la pantalla principal:
 * - Observa la sesión actual (AuthState) y reacciona cuando cambia.
 * - Escucha la colección en Firestore según el usuario autenticado.
 * - Expone un StateFlow con lista de entradas, loading y mensajes.
 */

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

/**
 * ViewModel de la pantalla principal.
 *
 * FLUJO DE SINCRONIZACIÓN EN TIEMPO REAL:
 * ========================================
 * 1. Al inicializarse, observa authManager.authState (Flow de Firebase Auth).
 * 2. Cuando hay un usuario autenticado, llama a startListening(userId).
 * 3. startListening() se suscribe a repository.observeEntries(userId).
 * 4. El repositorio delega al DataSource que usa addSnapshotListener de Firestore.
 * 5. Cada vez que Firestore detecta un cambio (crear/editar/borrar), emite una nueva lista.
 * 6. El ViewModel actualiza _uiState con la nueva lista.
 * 7. La Activity observa uiState y llama submitList() en el Adapter.
 * 8. DiffUtil calcula qué ítems cambiaron y solo redibuja esos.
 *
 * Resultado: la UI se actualiza automáticamente sin necesidad de refresh manual.
 */
class JournalViewModel(
    private val repository: JournalRepository,
    private val authManager: AuthManager
) : ViewModel() {

    // Estado observable de la UI (lista de entradas, loading, mensajes)
    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    // Job que mantiene la suscripción a Firestore; se cancela si el usuario cambia
    private var entriesJob: Job? = null

    init {
        // Al crear el ViewModel, empezamos a observar cambios de autenticación
        observeAuth()
    }

    /**
     * Observa el estado de autenticación de Firebase.
     *
     * - Si user es null → cancela la escucha de entradas y limpia la lista.
     * - Si user existe → arranca la escucha de Firestore con su UID.
     *
     * Esto permite que si el usuario cierra sesión, la lista se vacíe automáticamente.
     */
    private fun observeAuth() {
        viewModelScope.launch {
            authManager.authState.collect { user ->
                if (user == null) {
                    // Usuario cerró sesión: cancelar escucha y limpiar
                    entriesJob?.cancel()
                    _uiState.update {
                        it.copy(
                            entries = emptyList(),
                            isLoading = false,
                            message = "Inicia sesión para ver tus entradas"
                        )
                    }
                } else {
                    // Usuario autenticado: empezar a escuchar sus entradas
                    startListening(user.uid)
                }
            }
        }
    }

    /**
     * Inicia la suscripción en tiempo real a las entradas del usuario.
     *
     * Flujo interno:
     * 1. Cancela cualquier Job anterior (por si el usuario cambió).
     * 2. Lanza una nueva corrutina que colecta el Flow del repositorio.
     * 3. onStart: muestra el indicador de carga.
     * 4. catch: si hay error (ej: sin conexión), muestra mensaje.
     * 5. collect: cada vez que llega una lista nueva, actualiza el estado.
     *
     * IMPORTANTE: Este Flow nunca termina mientras la app esté activa.
     * Firestore mantiene una conexión WebSocket y emite cada cambio.
     */
    private fun startListening(userId: String) {
        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            repository.observeEntries(userId)
                .onStart {
                    // Mostrar spinner mientras carga la primera vez
                    _uiState.update { it.copy(isLoading = true) }
                }
                .catch { throwable ->
                    // Error de red o permisos de Firestore
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = throwable.localizedMessage ?: "Error al cargar tus entradas"
                        )
                    }
                }
                .collect { entries ->
                    // Nueva lista recibida de Firestore → actualizar UI
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

    /**
     * Limpia el mensaje mostrado en la UI para evitar repetir Snackbars.
     */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/**
 * Estado de la UI de la pantalla principal.
 *
 * @property entries   Lista de entradas a mostrar (vacía si no hay o está cargando).
 * @property isLoading True mientras esperamos la primera respuesta de Firestore.
 * @property message   Mensaje temporal para mostrar en Snackbar (error o información).
 */
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

