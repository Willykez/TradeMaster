package com.trademaster.pro.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trademaster.pro.data.model.PostEntity
import com.trademaster.pro.data.model.TickerEntity
import com.trademaster.pro.data.repo.SeedData
import com.trademaster.pro.ui.components.EmptyState
import com.trademaster.pro.ui.components.SectionHeader
import com.trademaster.pro.ui.components.StatCard
import com.trademaster.pro.ui.components.StatusBadge
import com.trademaster.pro.ui.components.TagChip
import com.trademaster.pro.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SectionHeader("Dashboard", "Welcome back. Here's what's happening in the markets today.")

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            StatusBadge(
                if (state.liveMarketData) "● Live prices" else "○ Demo prices",
                if (state.liveMarketData) Green else TextMute
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.ticker) { quote -> TickerTile(quote) }
        }

        Spacer(Modifier.height(16.dp))

        if (state.recentSignals.isNotEmpty()) {
            val stats = state.stats
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Active Signals", stats.activeSignals.toString(), "${stats.newSignalsToday} new today", Gold, Modifier.weight(1f))
                StatCard("Win Rate", "${stats.winRate}%", "+${stats.winDelta}%", Green, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Pips", "+${stats.totalPips}", "+${stats.pipsDelta} this wk", Blue, Modifier.weight(1f))
                StatCard("Members", stats.members.toString(), "+${stats.memberDelta} this wk", Purple, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("🔥 Recent Signals", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("● Live", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                if (state.recentSignals.isEmpty()) {
                    EmptyState("📊", "No signals yet", "New trade signals will appear here.")
                } else {
                    state.recentSignals.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${SeedData.flags[s.pair] ?: "💱"}  ${s.pair}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text(s.entry, fontWeight = FontWeight.Bold, color = TextDim, fontSize = 13.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TP ${s.tp}", color = Green, fontSize = 11.sp)
                                Text("SL ${s.sl}", color = Red, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("💬 Latest Community Activity", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                if (state.recentPosts.isEmpty()) {
                    EmptyState("💬", "No posts yet", "Community posts will show up here.")
                } else {
                    state.recentPosts.forEach { post ->
                        DashboardPostRow(post, onLike = { viewModel.toggleLike(post) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TickerTile(quote: TickerEntity) {
    val color = if (quote.up) Green else Red
    Card(
        modifier = Modifier.width(130.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(quote.pair, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDim)
            Spacer(Modifier.height(4.dp))
            Text(String.format(Locale.US, "%.4f", quote.price), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(
                "${if (quote.up) "▲" else "▼"} ${String.format(Locale.US, "%+.2f", quote.changePct)}%",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color
            )
        }
    }
}

@Composable
private fun DashboardPostRow(post: PostEntity, onLike: () -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(post.author + if (post.pinned) "  📌" else "", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Text(post.text, fontSize = 12.sp, color = TextDim, maxLines = 3)
        if (post.tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                post.tags.take(3).forEach { TagChip(it) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${if (post.liked) "❤️" else "🤍"} ${post.likes}   💬 ${post.comments}",
            fontSize = 11.sp, color = TextMute,
            modifier = Modifier.clickable(onClick = onLike)
        )
    }
}
