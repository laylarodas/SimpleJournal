package com.laylarodas.simplejournal.data.repository

/**
 * Capa de repositorio: expone una API limpia para la app sin importar si los datos
 * vienen de Firestore u otra fuente en el futuro.
 */

import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.data.remote.JournalRemoteDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de entradas.
 * Define las operaciones CRUD que la app necesita sin exponer detalles de Firestore.
 */
interface JournalRepository {
    /** Devuelve un Flow que emite la lista de entradas cada vez que cambian en Firestore. */
    fun observeEntries(userId: String): Flow<List<JournalEntry>>

    /** Crea una nueva entrada en la base de datos. */
    suspend fun addEntry(userId: String, entry: JournalEntry)

    /** Actualiza una entrada existente (requiere entry.id válido). */
    suspend fun updateEntry(userId: String, entry: JournalEntry)

    /** Elimina una entrada por su ID. */
    suspend fun deleteEntry(entryId: String)
}

/**
 * Implementación del repositorio usando Firestore como fuente de datos.
 * Delega todas las operaciones al JournalRemoteDataSource.
 *
 * ¿Por qué esta capa extra?
 * - Permite cambiar la fuente de datos (ej: Room local) sin tocar ViewModels.
 * - Facilita testing: puedes crear un FakeRepository para pruebas unitarias.
 */
class FirestoreJournalRepository(
    private val remoteDataSource: JournalRemoteDataSource
) : JournalRepository {

    override fun observeEntries(userId: String): Flow<List<JournalEntry>> =
        remoteDataSource.streamEntries(userId)

    override suspend fun addEntry(userId: String, entry: JournalEntry) {
        remoteDataSource.addEntry(userId, entry)
    }

    override suspend fun updateEntry(userId: String, entry: JournalEntry) {
        remoteDataSource.updateEntry(userId, entry)
    }

    override suspend fun deleteEntry(entryId: String) {
        remoteDataSource.deleteEntry(entryId)
    }
}

