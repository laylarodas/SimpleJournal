package com.laylarodas.simplejournal.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val firebaseAuth: FirebaseAuth
) {

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    suspend fun signIn(email: String, password: String): FirebaseUser {
        return firebaseAuth.signInWithEmailAndPassword(email, password).await().user
            ?: error("FirebaseAuth returned null user")
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        return firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("FirebaseAuth returned null user")
    }

    suspend fun signInAnonymously(): FirebaseUser {
        return firebaseAuth.signInAnonymously().await().user
            ?: error("FirebaseAuth returned null user")
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}

