package com.laylarodas.simplejournal.data.model

/**
 * Representa una entrada del diario tal como se guarda en Firestore.
 * Incluye helpers para formatear la fecha y convertir a Map (para subir/persistir).
 */

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @property id        ID único del documento en Firestore (vacío si es nueva).
 * @property title     Título de la entrada (obligatorio).
 * @property content   Cuerpo/texto de la entrada (puede estar vacío).
 * @property timestamp Milisegundos desde epoch; se usa para ordenar.
 * @property userId    UID del usuario dueño de esta entrada.
 */
data class JournalEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = ""
) {
    /** Devuelve la fecha en formato legible (ej: "02 Dec 2025"). */
    fun formattedDate(): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Convierte la entrada a un Map para guardar en Firestore.
     * Nota: el ID no se incluye porque Firestore lo maneja como nombre del documento.
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "content" to content,
        "timestamp" to timestamp,
        "userId" to userId
    )

    companion object {
        /**
         * Construye un JournalEntry desde un documento de Firestore.
         * @param id   ID del documento (document.id).
         * @param data Campos del documento (document.data).
         */
        fun fromMap(id: String, data: Map<String, Any?>): JournalEntry {
            return JournalEntry(
                id = id,
                title = data["title"] as? String ?: "",
                content = data["content"] as? String ?: "",
                timestamp = (data["timestamp"] as? Number)?.toLong()
                    ?: System.currentTimeMillis(),
                userId = data["userId"] as? String ?: ""
            )
        }
    }
}

