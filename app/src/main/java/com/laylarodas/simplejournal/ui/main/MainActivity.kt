package com.laylarodas.simplejournal.ui.main

/**
 * Pantalla principal: observa el estado del JournalViewModel y muestra la lista de entradas
 * con un RecyclerView. También abre el editor cuando el usuario toca el FAB.
 */

import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    /**
     * El Adapter recibe un callback para manejar clicks en las entradas.
     * Cuando el usuario toca una entrada, abrimos el editor con su ID.
     */
    private val journalAdapter by lazy {
        JournalAdapter { entry ->
            openEntryDetail(entry.id)
        }
    }

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
     * Configura el RecyclerView con un LinearLayoutManager vertical,
     * el JournalAdapter, y swipe-to-delete.
     *
     * El Adapter usa ListAdapter + DiffUtil para actualizar eficientemente.
     * El swipe permite eliminar entradas deslizando hacia la izquierda.
     */
    private fun setupRecycler() {
        binding.journalRecycler.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = journalAdapter
        }

        // Configurar swipe-to-delete
        setupSwipeToDelete()
    }

    /**
     * Configura el gesto de deslizar para eliminar.
     *
     * FLUJO:
     * 1. Usuario desliza una tarjeta hacia la izquierda.
     * 2. Se pinta un fondo rojo mientras desliza (feedback visual).
     * 3. Al soltar, se llama viewModel.deleteEntry().
     * 4. La lista se actualiza automáticamente (real-time sync).
     */
    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0, // No soportamos drag & drop
            ItemTouchHelper.LEFT // Solo swipe hacia la izquierda
        ) {
            // Fondo rojo que se muestra al deslizar
            private val background = ColorDrawable(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light)
            )

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false // No soportamos mover ítems

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val entry = journalAdapter.currentList.getOrNull(position)
                if (entry != null) {
                    viewModel.deleteEntry(entry.id)
                }
            }

            // Dibuja el fondo rojo mientras el usuario desliza
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView

                // Solo dibujar fondo si estamos deslizando hacia la izquierda
                if (dX < 0) {
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.journalRecycler)
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

    /**
     * Configura el FAB para crear nuevas entradas.
     * Abre el editor sin ID (modo creación).
     */
    private fun setupListeners() {
        binding.fabAddEntry.setOnClickListener {
            openEntryDetail(entryId = null)
        }
    }

    /**
     * Abre el editor de entradas.
     *
     * @param entryId Si es null, abre en modo creación.
     *                Si tiene valor, abre en modo edición con esa entrada.
     */
    private fun openEntryDetail(entryId: String?) {
        val intent = Intent(this, EntryDetailActivity::class.java)
        if (entryId != null) {
            intent.putExtra(EntryDetailActivity.EXTRA_ENTRY_ID, entryId)
        }
        startActivity(intent)
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

