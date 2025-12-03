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
     * Escucha cambios en tiempo real de la colección "journalEntries".
     *
     * ¿CÓMO FUNCIONA LA SINCRONIZACIÓN EN TIEMPO REAL?
     * =================================================
     * 1. addSnapshotListener() crea una conexión WebSocket con Firestore.
     * 2. Firestore envía inmediatamente el estado actual de la query.
     * 3. Cada vez que CUALQUIER documento que cumple la query cambia
     *    (creado, editado o borrado), Firestore envía un nuevo snapshot.
     * 4. Convertimos los documentos a JournalEntry y los emitimos via trySend().
     * 5. El Flow nunca termina por sí solo; se mantiene escuchando hasta que
     *    se cancele (awaitClose limpia el listener para evitar memory leaks).
     *
     * ¿POR QUÉ USAMOS callbackFlow?
     * callbackFlow convierte callbacks tradicionales (como addSnapshotListener)
     * en un Flow de Kotlin que se puede colectar con corrutinas.
     *
     * QUERY:
     * - whereEqualTo(userId): solo entradas del usuario actual.
     * - orderBy(timestamp, DESC): las más recientes primero.
     */
    fun streamEntries(userId: String): Flow<List<JournalEntry>> = callbackFlow {
        // Construimos la query: entradas del usuario ordenadas por fecha
        val query = firestore.collection(FirestoreConstants.JOURNAL_COLLECTION)
            .whereEqualTo(FirestoreConstants.FIELD_USER_ID, userId)
            .orderBy(FirestoreConstants.FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)

        // Registramos el listener; Firestore llamará este callback cada vez que haya cambios
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            // Si hay error (ej: permisos, red), cerramos el Flow con la excepción
            error?.let {
                close(it)
                return@addSnapshotListener
            }

            // Convertimos cada documento a JournalEntry
            val entries = snapshot?.documents?.map { document ->
                JournalEntry.fromMap(document.id, document.data ?: emptyMap())
            }.orEmpty()

            // Emitimos la lista al Flow (el ViewModel la recibirá)
            trySend(entries)
        }

        // Cuando el Flow se cancela (ej: el ViewModel muere), removemos el listener
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

