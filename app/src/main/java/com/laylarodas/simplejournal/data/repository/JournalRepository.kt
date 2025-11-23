package com.laylarodas.simplejournal.data.repository

import com.laylarodas.simplejournal.data.model.JournalEntry
import com.laylarodas.simplejournal.data.remote.JournalRemoteDataSource
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun observeEntries(userId: String): Flow<List<JournalEntry>>
    suspend fun addEntry(userId: String, entry: JournalEntry)
    suspend fun updateEntry(userId: String, entry: JournalEntry)
    suspend fun deleteEntry(entryId: String)
}

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

