package com.laylarodas.simplejournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.laylarodas.simplejournal.auth.AuthManager
import com.laylarodas.simplejournal.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Maneja el estado del login/registro: validaciones, llamadas a AuthManager y eventos de navegación.
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

    private fun emitError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isLoading = false,
                messageRes = R.string.auth_generic_error,
                messageArg = throwable.localizedMessage
            )
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

