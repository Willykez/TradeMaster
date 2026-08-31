package com.trademaster.pro.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// This is the real security boundary -- the AppMode.ADMIN toggle in the UI
// is just a display switch, it grants nothing by itself. Whether a write
// actually succeeds is decided by Firestore's security rules (see
// /firestore.rules), which only allow writes from a signed-in user whose
// UID has a document under admins/{uid}.
//
// Two kinds of signed-in user exist here:
//  - Anonymous (default for every install): can read, cannot write. Gets a
//    brand-new random UID on every reinstall/data-clear, which is exactly
//    why it's wrong to allowlist an anonymous UID for anything long-lived.
//  - Email/password admin account: a real, stable identity. Its UID stays
//    the same across reinstalls and devices, so it only ever needs adding
//    to admins/{uid} once, ever -- not once per device.
class AdminAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    suspend fun signInAdmin(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Drops the admin account and falls back to a fresh anonymous session (read-only). */
    suspend fun signOutAdminAccount() {
        auth.signOut()
        ensureSignedIn()
    }

    fun isAdminAccount(): Boolean = auth.currentUser?.isAnonymous == false

    /** One-shot check, used right after a login attempt for immediate feedback. */
    suspend fun checkIsAdminOnce(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * True whenever the *currently signed-in* UID has a document under
     * admins/. Re-subscribes automatically when the signed-in user changes
     * (e.g. anonymous -> admin login, or admin -> sign-out) -- a plain
     * snapshot listener captured against the UID at flow-creation time would
     * keep watching the old user's document forever after a sign-in change.
     */
    fun observeIsAdmin(): Flow<Boolean> = callbackFlow {
        var firestoreReg: ListenerRegistration? = null

        fun subscribeFor(uid: String?) {
            firestoreReg?.remove()
            firestoreReg = if (uid == null) {
                trySend(false)
                null
            } else {
                db.collection("admins").document(uid).addSnapshotListener { snapshot, _ ->
                    trySend(snapshot != null && snapshot.exists())
                }
            }
        }

        subscribeFor(auth.currentUser?.uid)
        val authListener = FirebaseAuth.AuthStateListener { a -> subscribeFor(a.currentUser?.uid) }
        auth.addAuthStateListener(authListener)

        awaitClose {
            firestoreReg?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    fun currentUid(): String? = auth.currentUser?.uid
    fun currentEmail(): String? = auth.currentUser?.email
}
