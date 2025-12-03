package com.laylarodas.simplejournal.ui.main

/**
 * Pantalla principal: observa el estado del JournalViewModel y muestra la lista de entradas
 * con un RecyclerView. También abre el editor cuando el usuario toca el FAB.
 */

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.laylarodas.simplejournal.R
import com.laylarodas.simplejournal.databinding.ActivityMainBinding
import com.laylarodas.simplejournal.ui.auth.LoginActivity
import com.laylarodas.simplejournal.ui.detail.EntryDetailActivity
import com.laylarodas.simplejournal.utils.ServiceLocator
import com.laylarodas.simplejournal.viewmodel.JournalViewModel
import com.laylarodas.simplejournal.viewmodel.JournalViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val journalAdapter by lazy { JournalAdapter() }
    private val authManager by lazy { ServiceLocator.provideAuthManager() }

    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory(
            repository = ServiceLocator.provideJournalRepository(),
            authManager = authManager
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

    override fun onStart() {
        super.onStart()
        if (authManager.currentUserId() == null) {
            navigateToLogin()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    /**
     * Configura el RecyclerView con un LinearLayoutManager vertical
     * y le asigna el JournalAdapter.
     *
     * El Adapter usa ListAdapter + DiffUtil para actualizar eficientemente:
     * solo redibuja los ítems que realmente cambiaron.
     */
    private fun setupRecycler() {
        binding.journalRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = journalAdapter
        }
    }

    /**
     * Observa el StateFlow del ViewModel y actualiza la UI cuando cambia.
     *
     * FLUJO DE ACTUALIZACIÓN EN TIEMPO REAL:
     * 1. repeatOnLifecycle(STARTED) asegura que solo colectamos cuando la Activity es visible.
     * 2. Cada vez que el ViewModel emite un nuevo JournalUiState, este bloque se ejecuta.
     * 3. Actualizamos visibilidad del spinner y el empty state.
     * 4. Llamamos submitList() que compara la lista anterior con la nueva usando DiffUtil.
     * 5. DiffUtil calcula los cambios y el RecyclerView anima solo los ítems afectados.
     *
     * Resultado: cuando creas una entrada en el editor y vuelves aquí,
     * la lista ya muestra la nueva entrada sin hacer nada extra.
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Mostrar/ocultar indicador de carga
                    binding.progressBar.isVisible = state.isLoading

                    // Mostrar mensaje vacío si no hay entradas
                    binding.emptyView.isVisible = state.entries.isEmpty() && !state.isLoading

                    // Actualizar la lista del Adapter (DiffUtil calcula los cambios)
                    journalAdapter.submitList(state.entries)

                    // Mostrar mensaje temporal si existe
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                authManager.signOut()
                navigateToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun navigateToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}

