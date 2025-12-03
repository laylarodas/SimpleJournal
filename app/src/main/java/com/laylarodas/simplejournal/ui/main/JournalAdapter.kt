package com.laylarodas.simplejournal.ui.main

/**
 * Adapter que conecta la lista de JournalEntry con el RecyclerView.
 *
 * ¿POR QUÉ USAMOS ListAdapter?
 * ============================
 * ListAdapter es una subclase de RecyclerView.Adapter que integra DiffUtil.
 * Cuando llamamos submitList(newList), internamente compara la lista anterior
 * con la nueva y calcula qué ítems se agregaron, eliminaron o modificaron.
 *
 * Beneficios:
 * - Solo redibuja los ítems que cambiaron (eficiente).
 * - Anima automáticamente inserciones/eliminaciones.
 * - Evita parpadeos innecesarios (no hace notifyDataSetChanged).
 *
 * Esto es clave para la sincronización en tiempo real: cada vez que Firestore
 * emite una lista nueva, DiffUtil detecta las diferencias y actualiza solo lo necesario.
 */

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.databinding.ItemJournalEntryBinding

class JournalAdapter :
    ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DIFF_CALLBACK) {

    /**
     * Crea un nuevo ViewHolder inflando el layout item_journal_entry.xml.
     * Se llama solo cuando el RecyclerView necesita una nueva vista (reciclaje).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemJournalEntryBinding.inflate(inflater, parent, false)
        return JournalViewHolder(binding)
    }

    /**
     * Vincula los datos de una entrada con el ViewHolder en la posición dada.
     * Se llama cada vez que un ítem entra en pantalla o cambia.
     */
    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que mantiene las referencias a las vistas de un ítem.
     * Usar ViewBinding evita findViewById y mejora la seguridad de tipos.
     */
    class JournalViewHolder(
        private val binding: ItemJournalEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Pinta los datos del JournalEntry en la tarjeta.
         * - Título: muestra "Sin título" si está vacío.
         * - Cuerpo: muestra "..." si está vacío (preview de máx 2 líneas).
         * - Fecha: usa formattedDate() para mostrar "02 Dec 2025".
         */
        fun bind(entry: JournalEntry) {
            binding.entryTitle.text = entry.title.ifBlank { "Sin título" }
            binding.entryBody.text = entry.content.ifBlank { "..." }
            binding.entryDate.text = entry.formattedDate()
        }
    }

    companion object {
        /**
         * Callback que DiffUtil usa para comparar ítems.
         *
         * areItemsTheSame: ¿es el mismo ítem? (compara IDs).
         *   - Si devuelve false, el ítem se considera nuevo/eliminado.
         *
         * areContentsTheSame: ¿tiene el mismo contenido? (compara todos los campos).
         *   - Si devuelve false, el ítem se redibuja con los nuevos datos.
         *
         * Ejemplo: si editas el título de una entrada, areItemsTheSame devuelve true
         * (mismo ID), pero areContentsTheSame devuelve false (título diferente),
         * entonces solo ESE ítem se redibuja.
         */
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<JournalEntry>() {
            override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean =
                oldItem == newItem
        }
    }
}

