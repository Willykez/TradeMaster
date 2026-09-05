package com.trademaster.pro.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Launches the system "Choose an account" sheet and returns a Google ID
 * token, or null if the user cancelled, no account is available, or
 * [webClientId] isn't configured yet. That token is what
 * AdminAuthRepository.signInWithGoogleIdToken exchanges for a Firebase
 * credential -- this function only talks to Google, it never touches
 * Firebase directly.
 */
suspend fun requestGoogleIdToken(context: Context, webClientId: String): String? {
    if (webClientId.isBlank()) return null

    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false) // show every Google account on the device, not just ones already used with this app
        .setServerClientId(webClientId)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            null
        }
    } catch (e: GetCredentialException) {
        // Cancelled by the user, no accounts on device, Play Services
        // unavailable, etc. -- all normal, non-fatal outcomes here.
        null
    }
}
