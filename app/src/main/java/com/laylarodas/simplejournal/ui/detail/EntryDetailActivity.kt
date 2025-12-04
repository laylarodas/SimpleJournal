package com.laylarodas.simplejournal.ui.detail

/**
 * Pantalla para crear o editar una entrada.
 *
 * MODOS DE OPERACIÓN:
 * - Sin EXTRA_ENTRY_ID: modo creación (nueva entrada).
 * - Con EXTRA_ENTRY_ID: modo edición (carga datos existentes).
 *
 * Conecta los campos de texto con el ViewModel y reacciona al estado
 * (loading, errores, cierre del formulario).
 */

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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

    companion object {
        /** Clave para pasar el ID de la entrada a editar via Intent. */
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private lateinit var binding: ActivityEntryDetailBinding

    /**
     * Obtenemos el ID de la entrada del Intent (null si es nueva).
     * Lo pasamos al Factory para que el ViewModel cargue los datos.
     */
    private val entryId: String? by lazy {
        intent.getStringExtra(EXTRA_ENTRY_ID)
    }

    private val viewModel: EntryDetailViewModel by viewModels {
        EntryDetailViewModelFactory(
            repository = ServiceLocator.provideJournalRepository(),
            authManager = ServiceLocator.provideAuthManager(),
            entryId = entryId
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

    /**
     * Configura el toolbar con título según el modo (crear/editar).
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.detailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (entryId != null) {
            getString(R.string.detail_toolbar_title_edit)
        } else {
            getString(R.string.detail_toolbar_title_new)
        }
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

    /**
     * Conecta los botones con el ViewModel.
     * - Guardar: valida y guarda en Firestore.
     * - Eliminar: muestra diálogo de confirmación y luego borra.
     */
    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            viewModel.saveEntry()
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    /**
     * Muestra un diálogo de confirmación antes de eliminar.
     * Esto evita borrados accidentales.
     */
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.detail_delete_confirm_title)
            .setMessage(R.string.detail_delete_confirm_message)
            .setPositiveButton(R.string.detail_delete_confirm_yes) { _, _ ->
                viewModel.deleteEntry()
            }
            .setNegativeButton(R.string.detail_delete_confirm_no, null)
            .show()
    }

    /**
     * Observa el StateFlow del ViewModel y actualiza la UI en consecuencia.
     *
     * - isLoading: muestra spinner mientras carga la entrada existente.
     * - isSaving/isDeleting: deshabilita botones y muestra texto de progreso.
     * - isEditing: muestra el botón de eliminar solo en modo edición.
     * - showTitleError: muestra error en el campo de título.
     * - message: muestra un Snackbar con el texto (éxito o error).
     * - closeScreen: cuando es true, cierra esta Activity y vuelve a Home.
     */
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Mostrar/ocultar contenido mientras carga la entrada
                    binding.buttonsContainer.isVisible = !state.isLoading
                    binding.titleLayout.isVisible = !state.isLoading
                    binding.contentLayout.isVisible = !state.isLoading

                    // Rellenar campos si el ViewModel cargó una entrada existente
                    if (state.shouldPopulateFields) {
                        binding.inputTitle.setText(state.title)
                        binding.inputContent.setText(state.content)
                        viewModel.onFieldsPopulated()
                    }

                    // Mostrar botón eliminar solo en modo edición
                    binding.buttonDelete.isVisible = state.isEditing

                    // Estado del botón guardar
                    val isBusy = state.isSaving || state.isDeleting
                    binding.buttonSave.isEnabled = !isBusy
                    binding.buttonSave.text = if (state.isSaving) {
                        getString(R.string.detail_saving_label)
                    } else {
                        getString(R.string.detail_save_button)
                    }

                    // Estado del botón eliminar
                    binding.buttonDelete.isEnabled = !isBusy
                    binding.buttonDelete.text = if (state.isDeleting) {
                        getString(R.string.detail_deleting_label)
                    } else {
                        getString(R.string.detail_delete_button)
                    }

                    // Mostrar error de título si aplica
                    binding.titleLayout.error = if (state.title.isBlank() && state.showTitleError) {
                        getString(R.string.detail_empty_title_error)
                    } else {
                        null
                    }

                    // Mostrar mensaje en Snackbar
                    state.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        viewModel.clearMessage()
                    }

                    // Cerrar pantalla si el guardado/borrado fue exitoso
                    if (state.closeScreen) {
                        viewModel.consumeCloseEvent()
                        finish()
                    }
                }
            }
        }
    }
}

