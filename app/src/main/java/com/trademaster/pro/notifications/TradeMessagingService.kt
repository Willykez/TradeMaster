package com.trademaster.pro.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Receives pushes sent to the "new_signals" topic (see the Cloud Function
// in /functions). This only handles the client side of push -- the actual
// "notify everyone" trigger lives server-side, since a client app can never
// be the thing that decides to message every other install.
class TradeMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "TradeMaster Pro"
        val body = message.notification?.body ?: message.data["body"] ?: "New activity on TradeMaster Pro"
        NotificationHelper.show(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        // Per-device token -- only needed if you later want to target
        // specific users instead of the shared topic (e.g. "your signal
        // hit TP"). Not used yet; left here as the natural extension point.
    }
}
