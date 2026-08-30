package com.trademaster.pro

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.trademaster.pro.data.db.AppDatabase
import com.trademaster.pro.data.remote.FirestoreDataSource
import com.trademaster.pro.data.remote.MarketDataRepository
import com.trademaster.pro.data.repo.AdminAuthRepository
import com.trademaster.pro.data.repo.TradeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TradeMasterApplication : Application() {
    // Lives exactly as long as the process -- the Firestore listeners this
    // drives are meant to run for the whole app lifetime, not tied to any
    // single screen's ViewModel.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: TradeRepository
        private set
    lateinit var adminAuth: AdminAuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        adminAuth = AdminAuthRepository()
        repository = TradeRepository(
            db = db,
            remote = FirestoreDataSource(),
            marketData = MarketDataRepository(),
            syncScope = appScope
        )
        appScope.launch {
            // Order matters: once /firestore.rules locks reads to signed-in
            // users, opening a listener before sign-in completes gets a
            // PERMISSION_DENIED that closes the listener for good --
            // Firestore does not auto-retry that, unlike a transient
            // network error. Auth has to land first.
            adminAuth.ensureSignedIn()
            repository.seedIfEmpty()
            repository.startCloudSync()
        }

        // All devices subscribe to one topic; a Cloud Function (see
        // /functions in the project root) publishes to it whenever a new
        // signal is written to Firestore, so "admin publishes -> everyone's
        // phone gets a push" without the app needing to know who's listening.
        // Independent of Firestore auth, safe to fire in parallel.
        FirebaseMessaging.getInstance().subscribeToTopic("new_signals")
    }
}
