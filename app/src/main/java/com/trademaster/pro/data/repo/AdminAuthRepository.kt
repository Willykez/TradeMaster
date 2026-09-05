package com.trademaster.pro.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Snapshot of who's currently signed in, for anything in the UI that needs to react to it live. */
data class AuthSummary(
    val isAnonymous: Boolean,
    val isGoogleUser: Boolean,
    val isAdminAccount: Boolean,
    val email: String?,
    val displayName: String?,
    val uid: String?
)

// Three kinds of signed-in user exist here, all Firebase Auth users, but
// meaning different things:
//  - Anonymous (default on first launch): read-only, brand-new random UID
//    every reinstall. Nobody chose this, it just lets browsing work
//    immediately with no login wall.
//  - Google account (regular users, opt-in): a real, persistent identity.
//    Signing in *links* the existing anonymous account to it when possible,
//    so anything the user already did anonymously (likes, votes) carries
//    over onto the same UID instead of starting over.
//  - Email/password admin account: also a real, persistent identity, but
//    deliberately a separate flow from Google -- the project owner signs
//    in "their own way", not through the same button regular users see.
// Whether any of these can actually WRITE anything is still decided by
// Firestore's security rules (see /firestore.rules), not by this class.
class AdminAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    // ---- Admin (email/password), unchanged from before ----

    suspend fun signInAdmin(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOutAdminAccount() = signOutToAnonymous()

    // ---- Regular users (Google) ----

    /**
     * Exchanges a Google ID token for a Firebase identity. If the current
     * session is still anonymous, this *links* the Google account onto it
     * so the same UID (and anything already written under it) carries
     * forward. If that Google account already belongs to a different
     * Firebase user (e.g. signed in on another device before), linking
     * fails with a collision -- in that case we just sign into the
     * existing account instead, which is the correct outcome, it just
     * means the current device's anonymous data doesn't merge into it.
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val current = auth.currentUser
            if (current != null && current.isAnonymous) {
                try {
                    current.linkWithCredential(credential).await()
                } catch (e: FirebaseAuthUserCollisionException) {
                    auth.signInWithCredential(credential).await()
                }
            } else {
                auth.signInWithCredential(credential).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Signs out of Google/admin and drops back to a fresh anonymous (read-only) session. */
    suspend fun signOutToAnonymous() {
        auth.signOut()
        ensureSignedIn()
    }

    // ---- Shared ----

    fun isAdminAccount(): Boolean = auth.currentUser?.isAnonymous == false &&
        auth.currentUser?.providerData?.any { it.providerId == "password" } == true

    fun isGoogleUser(): Boolean = auth.currentUser?.providerData?.any { it.providerId == "google.com" } == true

    /** One-shot check, used right after an admin login attempt for immediate feedback. */
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
     * (anonymous -> Google, anonymous -> admin, sign-out, etc.) -- a plain
     * snapshot listener captured against the UID at flow-creation time
     * would keep watching the old user's document forever after any of
     * those transitions.
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

    /** Reactive version of the sync getters below -- for anything in the UI (e.g. top bar icons) that needs to update live across sign-in/out. */
    fun observeAuthSummary(): Flow<AuthSummary> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { a ->
            val user = a.currentUser
            trySend(
                AuthSummary(
                    isAnonymous = user?.isAnonymous ?: true,
                    isGoogleUser = user?.providerData?.any { it.providerId == "google.com" } == true,
                    isAdminAccount = user?.isAnonymous == false &&
                        user.providerData.any { it.providerId == "password" },
                    email = user?.email,
                    displayName = user?.displayName,
                    uid = user?.uid
                )
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUid(): String? = auth.currentUser?.uid
    fun currentEmail(): String? = auth.currentUser?.email
}
