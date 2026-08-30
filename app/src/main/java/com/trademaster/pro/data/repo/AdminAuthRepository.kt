package com.trademaster.pro.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// This is the real security boundary -- the AppMode.ADMIN toggle in the UI
// is just a display switch, it grants nothing by itself. Whether a write
// actually succeeds is decided by Firestore's security rules (see
// /firestore.rules), which only allow writes from a signed-in user whose
// UID has a document under admins/{uid}. Every install signs in
// anonymously so request.auth is never null, but that alone grants
// read-only access -- someone has to be added to the admins collection by
// hand, from the Firebase console, before their device can actually publish
// anything. Decompiling the APK and flipping the client toggle changes
// nothing without that.
class AdminAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    /** True once this device's UID is present in the admins/ collection server-side. */
    fun observeIsAdmin(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val registration = db.collection("admins").document(uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot != null && snapshot.exists())
        }
        awaitClose { registration.remove() }
    }

    fun currentUid(): String? = auth.currentUser?.uid
}
