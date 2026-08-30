package com.trademaster.pro.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trademaster.pro.data.model.*
import com.trademaster.pro.data.repo.TradeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val ticker: List<TickerEntity> = emptyList(),
    val recentSignals: List<SignalEntity> = emptyList(),
    val recentPosts: List<PostEntity> = emptyList(),
    val stats: PlatformStats = PlatformStats(0, 0.0, 0, 0, 0, 0.0, 0, 0),
    val loading: Boolean = true,
    val liveMarketData: Boolean = false
)

class DashboardViewModel(private val repo: TradeRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.ticker, repo.signals, repo.posts
    ) { ticker, signals, posts ->
        DashboardUiState(
            ticker = ticker,
            recentSignals = signals.take(5),
            recentPosts = (posts.filter { it.pinned } + posts.filterNot { it.pinned }).take(3),
            stats = repo.computeStats(signals),
            loading = false,
            liveMarketData = repo.hasLiveMarketData
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
        // 15s keeps a single batched quote request comfortably under Twelve
        // Data's free-tier rate limit (8 req/min) with room to spare, and
        // still feels live on a ticker. Falls back to the local simulator
        // automatically when no API key/network is available -- see
        // TradeRepository.refreshTicker.
        viewModelScope.launch {
            while (true) {
                repo.refreshTicker(uiState.value.ticker)
                delay(15_000)
            }
        }
    }

    fun toggleLike(post: PostEntity) = viewModelScope.launch { repo.toggleLike(post) }
}
