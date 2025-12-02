package com.laylarodas.simplejournal.data.remote

/**
 * Fuente de datos remota: se encarga de leer/escribir en Cloud Firestore
 * y ofrecer Flows para escuchar cambios en tiempo real.
 */

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.utils.FirestoreConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class JournalRemoteDataSource(
    private val firestore: FirebaseFirestore
) {

    /**
     * Escucha en tiempo real los documentos de la colección "journalEntries"
     * filtrados por el userId. Cada vez que Firestore detecta un cambio
     * (crear, editar, borrar), emite una nueva lista completa.
     *
     * Usa callbackFlow para convertir el listener de Firestore en un Flow de Kotlin.
     */
    fun streamEntries(userId: String): Flow<List<JournalEntry>> = callbackFlow {
        val query = firestore.collection(FirestoreConstants.JOURNAL_COLLECTION)
            .whereEqualTo(FirestoreConstants.FIELD_USER_ID, userId)
            .orderBy(FirestoreConstants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            error?.let {
                close(it)
                return@addSnapshotListener
            }
            val entries = snapshot?.documents?.map { document ->
                JournalEntry.fromMap(document.id, document.data ?: emptyMap())
            }.orEmpty()
            trySend(entries)
        }

        awaitClose { registration.remove() }
    }

    /**
     * Crea un nuevo documento en Firestore.
     *
     * Flujo:
     * 1. Si entry.id está vacío, Firestore genera un ID único automáticamente.
     * 2. Copiamos la entrada con el ID generado, el userId y un timestamp si no existe.
     * 3. Llamamos doc.set() que guarda el Map en la colección.
     * 4. await() suspende hasta que Firestore confirme la escritura.
     *
     * Si falla (sin conexión, reglas de seguridad, etc.), lanza una excepción.
     */
    suspend fun addEntry(userId: String, entry: JournalEntry) {
        // Si la entrada no tiene ID, Firestore crea uno nuevo
        val doc = if (entry.id.isBlank()) {
            firestore.collection(FirestoreConstants.JOURNAL_COLLECTION).document()
        } else {
            firestore.collection(FirestoreConstants.JOURNAL_COLLECTION).document(entry.id)
        }

        // Preparamos el payload con valores definitivos
        val payload = entry.copy(
            id = doc.id,
            userId = userId,
            timestamp = entry.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
        )

        // Guardamos en Firestore y esperamos confirmación
        doc.set(payload.toMap()).await()
    }

    /**
     * Actualiza un documento existente en Firestore.
     * Requiere que entry.id no esté vacío (debe existir previamente).
     */
    suspend fun updateEntry(userId: String, entry: JournalEntry) {
        require(entry.id.isNotBlank()) { "Entry id cannot be blank when updating" }
        firestore.collection(FirestoreConstants.JOURNAL_COLLECTION)
            .document(entry.id)
            .set(entry.copy(userId = userId).toMap())
            .await()
    }

    /**
     * Elimina un documento de Firestore por su ID.
     * Si el documento no existe, Firestore no lanza error (operación idempotente).
     */
    suspend fun deleteEntry(entryId: String) {
        firestore.collection(FirestoreConstants.JOURNAL_COLLECTION)
            .document(entryId)
            .delete()
            .await()
    }
}

