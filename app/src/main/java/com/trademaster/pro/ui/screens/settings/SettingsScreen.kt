package com.trademaster.pro.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trademaster.pro.ui.components.SectionHeader
import com.trademaster.pro.ui.theme.CardBg
import com.trademaster.pro.ui.theme.Red
import com.trademaster.pro.ui.theme.TextPrimary

@Composable
fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("Settings", "Platform preferences and data controls.")
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Data lives in a local Room database on this device.", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "This mirrors the original web app's export/import/reset controls conceptually -- " +
                        "wire these buttons to a backup/restore flow (e.g. export the Room DB file, or sync " +
                        "via your own backend) when you're ready to go further than local-only storage.",
                    color = TextPrimary
                )
            }
        }
    }
}
