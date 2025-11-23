package com.laylarodas.simplejournal.ui.detail

/**
 * Pantalla para crear o editar una entrada. Conecta los campos de texto con el ViewModel
 * y reacciona al estado (loading, errores, cierre del formulario).
 */

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
import com.laylarodas.simplejournal.databinding.ActivityEntryDetailBinding
import com.laylarodas.simplejournal.utils.ServiceLocator
import com.laylarodas.simplejournal.viewmodel.EntryDetailViewModel
import com.laylarodas.simplejournal.viewmodel.EntryDetailViewModelFactory
import kotlinx.coroutines.launch

class EntryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryDetailBinding

    private val viewModel: EntryDetailViewModel by viewModels {
        EntryDetailViewModelFactory(
            repository = ServiceLocator.provideJournalRepository(),
            authManager = ServiceLocator.provideAuthManager()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupInputs()
        setupListeners()
        observeUiState()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.detailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupInputs() {
        binding.inputTitle.doOnTextChanged { text, _, _, _ ->
            viewModel.updateTitle(text?.toString().orEmpty())
        }
        binding.inputContent.doOnTextChanged { text, _, _, _ ->
            viewModel.updateContent(text?.toString().orEmpty())
        }
    }

    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            viewModel.saveEntry()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.buttonSave.isEnabled = !state.isSaving
                    binding.buttonSave.text = if (state.isSaving) {
                        getString(R.string.detail_saving_label)
                    } else {
                        getString(R.string.detail_save_button)
                    }
                    binding.titleLayout.error = if (state.title.isBlank() && state.showTitleError) {
                        getString(R.string.detail_empty_title_error)
                    } else {
                        null
                    }

                    state.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessage()
                    }

                    if (state.closeScreen) {
                        viewModel.consumeCloseEvent()
                        finish()
                    }
                }
            }
        }
    }
}

