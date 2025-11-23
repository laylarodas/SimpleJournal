package com.laylarodas.simplejournal.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.laylarodas.simplejournal.auth.AuthManager
import com.laylarodas.simplejournal.data.remote.JournalRemoteDataSource
import com.laylarodas.simplejournal.data.repository.FirestoreJournalRepository
import com.laylarodas.simplejournal.data.repository.JournalRepository

object ServiceLocator {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun provideAuthManager(): AuthManager = AuthManager(firebaseAuth)

    fun provideJournalRepository(): JournalRepository =
        FirestoreJournalRepository(
            remoteDataSource = JournalRemoteDataSource(firestore)
        )
}

