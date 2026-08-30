package com.trademaster.pro.ui.screens.signals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trademaster.pro.data.model.SignalEntity
import com.trademaster.pro.data.model.SignalStatus
import com.trademaster.pro.data.model.SignalType
import com.trademaster.pro.data.repo.TradeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SignalsViewModel(private val repo: TradeRepository) : ViewModel() {

    val signals: StateFlow<List<SignalEntity>> =
        repo.signals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(
        existing: SignalEntity?,
        pair: String, type: SignalType, entry: String, tp: String, sl: String,
        status: SignalStatus, pips: String, notes: String
    ): String? {
        if (pair.isBlank() || entry.isBlank() || tp.isBlank() || sl.isBlank()) {
            return "Please fill in pair, entry, TP and SL"
        }
        val entity = (existing ?: SignalEntity(
            pair = pair, type = type, entry = entry, tp = tp, sl = sl, status = status,
            pips = pips.ifBlank { "--" }, notes = notes, createdAt = System.currentTimeMillis()
        )).let {
            if (existing != null) it.copy(
                pair = pair, type = type, entry = entry, tp = tp, sl = sl, status = status,
                pips = pips.ifBlank { "--" }, notes = notes
            ) else it
        }
        viewModelScope.launch { repo.saveSignal(entity) }
        return null
    }

    fun delete(signal: SignalEntity) = viewModelScope.launch { repo.deleteSignal(signal) }
}
