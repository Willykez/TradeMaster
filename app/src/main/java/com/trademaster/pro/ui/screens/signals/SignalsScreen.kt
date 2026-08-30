package com.trademaster.pro.ui.screens.signals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trademaster.pro.AppMode
import com.trademaster.pro.data.model.SignalEntity
import com.trademaster.pro.data.model.SignalStatus
import com.trademaster.pro.data.model.SignalType
import com.trademaster.pro.data.repo.SeedData
import com.trademaster.pro.ui.components.*
import com.trademaster.pro.ui.theme.*

@Composable
fun SignalsScreen(viewModel: SignalsViewModel, mode: AppMode) {
    val signals by viewModel.signals.collectAsState()
    var filter by remember { mutableStateOf("All") }
    var editing by remember { mutableStateOf<SignalEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val filtered = when (filter) {
        "Active" -> signals.filter { it.status == SignalStatus.ACTIVE }
        "Pending" -> signals.filter { it.status == SignalStatus.PENDING }
        "Closed" -> signals.filter { it.status == SignalStatus.CLOSED }
        else -> signals
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            SectionHeader("Live Trading Signals", "Real-time trade setups with entry, TP and SL levels.")
            FilterTabRow(listOf("All", "Active", "Pending", "Closed"), filter) { filter = it }
            Spacer(Modifier.height(8.dp))

            if (showForm && mode == AppMode.ADMIN) {
                SignalForm(
                    existing = editing,
                    onCancel = { showForm = false; editing = null },
                    onSave = { pair, type, entry, tp, sl, status, pips, notes ->
                        val err = viewModel.save(editing, pair, type, entry, tp, sl, status, pips, notes)
                        if (err == null) { showForm = false; editing = null }
                        err
                    },
                    onDelete = editing?.let { sig -> { viewModel.delete(sig); showForm = false; editing = null } }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (filtered.isEmpty()) {
                EmptyState("📊", "No signals here", "Try a different filter tab.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { signal ->
                        SignalCard(
                            signal, mode,
                            onEdit = { editing = signal; showForm = true },
                            onDelete = { viewModel.delete(signal) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (mode == AppMode.ADMIN) {
            AdminFab(
                expanded = showForm,
                onClick = {
                    if (showForm) { showForm = false; editing = null }
                    else { editing = null; showForm = true }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun SignalCard(signal: SignalEntity, mode: AppMode, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("${SeedData.flags[signal.pair] ?: "💱"}  ${signal.pair}", fontWeight = FontWeight.Bold, color = TextPrimary)
                StatusBadge(signal.type.name, if (signal.type == SignalType.BUY) Green else if (signal.type == SignalType.SELL) Red else Blue)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column { Text("Entry", fontSize = 10.sp, color = TextMute); Text(signal.entry, fontWeight = FontWeight.Bold, color = TextPrimary) }
                Column { Text("TP", fontSize = 10.sp, color = TextMute); Text(signal.tp, fontWeight = FontWeight.Bold, color = Green) }
                Column { Text("SL", fontSize = 10.sp, color = TextMute); Text(signal.sl, fontWeight = FontWeight.Bold, color = Red) }
                Column { Text("Pips", fontSize = 10.sp, color = TextMute); Text(signal.pips, fontWeight = FontWeight.Bold, color = if (signal.pips.startsWith("+")) Green else TextMute) }
            }
            if (signal.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(signal.notes, fontSize = 12.sp, color = TextDim)
            }
            if (mode == AppMode.ADMIN) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit) { Text("Edit") }
                    OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun SignalForm(
    existing: SignalEntity?,
    onCancel: () -> Unit,
    onSave: (String, SignalType, String, String, String, SignalStatus, String, String) -> String?,
    onDelete: (() -> Unit)?
) {
    var pair by remember(existing) { mutableStateOf(existing?.pair ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: SignalType.BUY) }
    var entry by remember(existing) { mutableStateOf(existing?.entry ?: "") }
    var tp by remember(existing) { mutableStateOf(existing?.tp ?: "") }
    var sl by remember(existing) { mutableStateOf(existing?.sl ?: "") }
    var status by remember(existing) { mutableStateOf(existing?.status ?: SignalStatus.ACTIVE) }
    var pips by remember(existing) { mutableStateOf(existing?.pips ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (existing == null) "Create New Signal" else "Edit Signal", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            FormField("Currency Pair *", pair, { pair = it })
            Spacer(Modifier.height(10.dp))
            EnumPicker("Type", SignalType.entries, type) { type = it }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("Entry *", entry, { entry = it }, Modifier.weight(1f))
                FormField("TP *", tp, { tp = it }, Modifier.weight(1f))
                FormField("SL *", sl, { sl = it }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            EnumPicker("Status", SignalStatus.entries, status) { status = it }
            Spacer(Modifier.height(10.dp))
            FormField("Pips result", pips, { pips = it })
            Spacer(Modifier.height(10.dp))
            FormField("Analysis notes", notes, { notes = it }, singleLine = false, minLines = 3)
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error!!, color = Red, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { error = onSave(pair, type, entry, tp, sl, status, pips, notes) },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) { Text(if (existing == null) "Publish" else "Update") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumPicker(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextDim)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt == selected,
                    onClick = { onSelect(opt) },
                    label = { Text(opt.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = androidx.compose.ui.graphics.Color.Black)
                )
            }
        }
    }
}
