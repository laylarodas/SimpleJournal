package com.laylarodas.simplejournal.ui.auth

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.laylarodas.simplejournal.R
import com.laylarodas.simplejournal.databinding.ActivityLoginBinding
import com.laylarodas.simplejournal.utils.ServiceLocator
import com.laylarodas.simplejournal.viewmodel.AuthViewModel
import com.laylarodas.simplejournal.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

/**
 * Pantalla de login/registro. Conecta la UI con el AuthViewModel para manejar sesiones.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(ServiceLocator.provideAuthManager())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInputs()
        setupListeners()
        observeState()
    }

    private fun setupInputs() {
        binding.inputEmail.doOnTextChanged { text, _, _, _ ->
            viewModel.updateEmail(text?.toString().orEmpty())
        }
        binding.inputPassword.doOnTextChanged { text, _, _, _ ->
            viewModel.updatePassword(text?.toString().orEmpty())
        }
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener { viewModel.login() }
        binding.buttonRegister.setOnClickListener { viewModel.register() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.emailLayout.error = state.emailErrorRes?.let { getString(it) }
                    binding.passwordLayout.error = state.passwordErrorRes?.let { getString(it) }

                    binding.buttonLogin.isEnabled = !state.isLoading
                    binding.buttonRegister.isEnabled = !state.isLoading
                    binding.progressBar.isVisible = state.isLoading

                    state.messageRes?.let { resId ->
                        val text = if (state.messageArg != null && resId == R.string.auth_generic_error) {
                            "${getString(resId)} (${state.messageArg})"
                        } else {
                            getString(resId)
                        }
                        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }
}

