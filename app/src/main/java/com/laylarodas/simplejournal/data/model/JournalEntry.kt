package com.laylarodas.simplejournal.data.model

/**
 * Representa una entrada del diario tal como se guarda en Firestore.
 * Incluye helpers para formatear la fecha y convertir a Map (para subir/persistir).
 */

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JournalEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = ""
) {
    fun formattedDate(): String {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "content" to content,
        "timestamp" to timestamp,
        "userId" to userId
    )

    companion object {
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

