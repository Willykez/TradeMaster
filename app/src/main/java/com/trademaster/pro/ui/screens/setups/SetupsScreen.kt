package com.trademaster.pro.ui.screens.setups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trademaster.pro.ui.components.SectionHeader
import com.trademaster.pro.ui.components.StatusBadge
import com.trademaster.pro.ui.components.TagChip
import com.trademaster.pro.ui.theme.*

private val checklist = listOf(
    "Trend aligned with higher timeframe",
    "Clear support/resistance level identified",
    "RSI not overbought/oversold",
    "Volume confirmation on breakout",
    "Risk under 2% of account",
    "News calendar checked — no high impact"
)

@Composable
fun SetupsScreen() {
    val checked = remember { mutableStateListOf(*BooleanArray(checklist.size).toTypedArray()) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollStateCompat())) {
        SectionHeader("Trade Setups", "Detailed technical analysis with chart annotations and entry plans.")

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("📈 EUR/USD Bullish Breakout Setup", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    StatusBadge("ACTIVE", Green)
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Bg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 40.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Chart Analysis with S/R Levels", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Entry: 1.0845 | TP: 1.0890 | SL: 1.0820", color = TextMute, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Technical Analysis", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Price has broken above the descending trendline resistance on the 4H timeframe. RSI showing bullish divergence at 52. MACD histogram turning positive with a golden cross forming. Key support now at 1.0820 (previous resistance turned support). Targeting the next major resistance zone at 1.0890, aligning with the 0.618 Fibonacci retracement.",
                    color = TextDim, fontSize = 13.sp, lineHeight = 19.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("EURUSD", "Breakout", "4H", "Bullish", "Fibonacci").forEach { TagChip(it) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("⚙️ Setup Parameters", fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ParamBox("RISK/REWARD", "1:2.5", Green, Modifier.weight(1f))
                    ParamBox("CONFIDENCE", "85%", Gold, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ParamBox("TIMEFRAME", "4H / D1", TextPrimary, Modifier.weight(1f))
                    ParamBox("EXP. MOVE", "45 pips", Blue, Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("📋 Pre-Trade Checklist", fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                checklist.forEachIndexed { i, item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Checkbox(
                            checked = checked[i],
                            onCheckedChange = { checked[i] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = androidx.compose.ui.graphics.Color.Black)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item, color = TextDim, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ParamBox(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Bg, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Text(label, fontSize = 10.sp, color = TextMute, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun rememberScrollStateCompat() = androidx.compose.foundation.rememberScrollState()
