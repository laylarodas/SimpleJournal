package com.laylarodas.simplejournal.ui.main

/**
 * Pantalla principal: observa el estado del JournalViewModel y muestra la lista de entradas
 * con un RecyclerView. También abre el editor cuando el usuario toca el FAB.
 */

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.laylarodas.simplejournal.databinding.ActivityMainBinding
import com.laylarodas.simplejournal.ui.detail.EntryDetailActivity
import com.laylarodas.simplejournal.utils.ServiceLocator
import com.laylarodas.simplejournal.viewmodel.JournalViewModel
import com.laylarodas.simplejournal.viewmodel.JournalViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val journalAdapter by lazy { JournalAdapter() }

    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory(
            repository = ServiceLocator.provideJournalRepository(),
            authManager = ServiceLocator.provideAuthManager()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecycler()
        observeUiState()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecycler() {
        binding.journalRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = journalAdapter
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.emptyView.isVisible = state.entries.isEmpty() && !state.isLoading
                    journalAdapter.submitList(state.entries)

                    state.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddEntry.setOnClickListener {
            startActivity(Intent(this, EntryDetailActivity::class.java))
        }
    }
}

