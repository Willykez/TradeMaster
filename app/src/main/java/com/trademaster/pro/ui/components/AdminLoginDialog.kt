package com.trademaster.pro.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trademaster.pro.ui.theme.CardBg
import com.trademaster.pro.ui.theme.Gold
import com.trademaster.pro.ui.theme.Red
import com.trademaster.pro.ui.theme.TextDim
import com.trademaster.pro.ui.theme.TextMute
import com.trademaster.pro.ui.theme.TextPrimary

// Signing in here proves *who you are*; it does not by itself grant admin
// access. A successful login for an account that isn't in Firestore's
// admins/ collection still shows the same "not on the list" message --
// this dialog just replaces "dig through Firebase console for a random
// anonymous UID every reinstall" with "log into the one stable account
// that's already allowlisted".
@Composable
fun AdminLoginDialog(
    uidForDisplay: String?,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Admin sign in", color = TextPrimary) },
        text = {
            Column {
                Text(
                    "Sign in with the admin account. This is a one-time thing per account, not per device -- once it's set up, it survives reinstalls.",
                    color = TextDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                FormField("Email", email, { email = it })
                Spacer(Modifier.height(10.dp))
                FormField(
                    "Password", password, { password = it },
                    visualTransformation = PasswordVisualTransformation()
                )
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator(color = Gold, modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                }
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = Red, fontSize = 12.sp)
                }
                if (uidForDisplay != null) {
                    Spacer(Modifier.height(10.dp))
                    Text("Signed-in UID: $uidForDisplay", color = TextMute, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(email, password) },
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
            ) { Text("Sign in") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
