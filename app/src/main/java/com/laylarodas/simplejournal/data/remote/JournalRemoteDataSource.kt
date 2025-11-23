package com.laylarodas.simplejournal.data.remote

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

    suspend fun addEntry(userId: String, entry: JournalEntry) {
        val doc = if (entry.id.isBlank()) {
            firestore.collection(FirestoreConstants.JOURNAL_COLLECTION).document()
        } else {
            firestore.collection(FirestoreConstants.JOURNAL_COLLECTION).document(entry.id)
        }

        val payload = entry.copy(
            id = doc.id,
            userId = userId,
            timestamp = entry.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
        )

        doc.set(payload.toMap()).await()
    }

    suspend fun updateEntry(userId: String, entry: JournalEntry) {
        require(entry.id.isNotBlank()) { "Entry id cannot be blank when updating" }
        firestore.collection(FirestoreConstants.JOURNAL_COLLECTION)
            .document(entry.id)
            .set(entry.copy(userId = userId).toMap())
            .await()
    }

    suspend fun deleteEntry(entryId: String) {
        firestore.collection(FirestoreConstants.JOURNAL_COLLECTION)
            .document(entryId)
            .delete()
            .await()
    }
}

