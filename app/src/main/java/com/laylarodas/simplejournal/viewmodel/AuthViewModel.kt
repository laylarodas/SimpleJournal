package com.laylarodas.simplejournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.laylarodas.simplejournal.auth.AuthManager
import com.laylarodas.simplejournal.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Maneja el estado del login/registro: validaciones, llamadas a AuthManager y eventos de navegación.
 *
 * MANEJO DE ERRORES DE FIREBASE:
 * ==============================
 * Firebase Auth lanza diferentes tipos de excepciones según el error:
 * - FirebaseAuthUserCollisionException → El email ya está registrado.
 * - FirebaseAuthInvalidUserException → No existe cuenta con ese email.
 * - FirebaseAuthInvalidCredentialsException → Contraseña incorrecta o email mal formado.
 * - FirebaseAuthWeakPasswordException → Contraseña muy corta (menos de 6 caracteres).
 * - FirebaseTooManyRequestsException → Demasiados intentos fallidos.
 * - FirebaseNetworkException → Sin conexión a internet.
 *
 * El método mapFirebaseError() detecta el tipo de excepción y devuelve
 * el recurso de string apropiado para mostrar un mensaje amigable al usuario.
 */
class AuthViewModel(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email.trim(), emailErrorRes = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password.trim(), passwordErrorRes = null) }
    }

    fun login() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        if (!validateInputs(email, password)) return

        viewModelScope.launch {
            setLoading(true)
            runCatching {
                authManager.signIn(email, password)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messageRes = R.string.auth_success_login,
                        navigateHome = true
                    )
                }
            }.onFailure { throwable ->
                emitError(throwable)
            }
        }
    }

    fun register() {
        val email = _uiState.value.email
        val password = _uiState.value.password
        if (!validateInputs(email, password)) return

        viewModelScope.launch {
            setLoading(true)
            runCatching {
                authManager.signUp(email, password)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messageRes = R.string.auth_success_register,
                        navigateHome = true
                    )
                }
            }.onFailure { throwable ->
                emitError(throwable)
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var valid = true
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(emailErrorRes = R.string.auth_invalid_email) }
            valid = false
        }
        if (password.length < 6) {
            _uiState.update { it.copy(passwordErrorRes = R.string.auth_invalid_password) }
            valid = false
        }
        return valid
    }

    private fun setLoading(isLoading: Boolean) {
        _uiState.update { it.copy(isLoading = isLoading, messageRes = null, navigateHome = false) }
    }

    /**
     * Detecta el tipo de error de Firebase y muestra un mensaje específico.
     *
     * Esto mejora la experiencia del usuario porque:
     * - Si el email ya existe → le sugerimos iniciar sesión.
     * - Si la contraseña es incorrecta → le decimos exactamente eso.
     * - Si no hay conexión → le pedimos que verifique su red.
     *
     * En lugar de mostrar siempre "Algo salió mal", el usuario sabe
     * exactamente qué corregir.
     */
    private fun emitError(throwable: Throwable) {
        val errorRes = mapFirebaseError(throwable)
        _uiState.update {
            it.copy(
                isLoading = false,
                messageRes = errorRes
            )
        }
    }

    /**
     * Mapea excepciones de Firebase Auth a recursos de string.
     *
     * @param throwable La excepción lanzada por Firebase.
     * @return ID del recurso de string con el mensaje apropiado.
     */
    private fun mapFirebaseError(throwable: Throwable): Int {
        return when (throwable) {
            // El correo ya está registrado con otra cuenta
            is FirebaseAuthUserCollisionException -> R.string.auth_error_email_in_use

            // No existe una cuenta con ese correo
            is FirebaseAuthInvalidUserException -> R.string.auth_error_user_not_found

            // Contraseña incorrecta o credenciales inválidas
            is FirebaseAuthInvalidCredentialsException -> R.string.auth_error_invalid_credential

            // Contraseña muy débil (menos de 6 caracteres)
            is FirebaseAuthWeakPasswordException -> R.string.auth_error_weak_password

            // Demasiados intentos fallidos (rate limiting)
            is FirebaseTooManyRequestsException -> R.string.auth_error_too_many_requests

            // Sin conexión a internet
            is FirebaseNetworkException -> R.string.auth_error_network

            // Cualquier otro error desconocido
            else -> R.string.auth_generic_error
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(navigateHome = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(messageRes = null, messageArg = null) }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val emailErrorRes: Int? = null,
    val passwordErrorRes: Int? = null,
    val isLoading: Boolean = false,
    val messageRes: Int? = null,
    val messageArg: String? = null,
    val navigateHome: Boolean = false
)

class AuthViewModelFactory(
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

