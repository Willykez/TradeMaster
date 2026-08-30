package com.trademaster.pro.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.trademaster.pro.data.repo.TradeRepository

// Manual factory (no Hilt) so the sample stays dependency-light -- one
// repository instance flows into every screen ViewModel from here.
class RepoViewModelFactory(private val repository: TradeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return modelClass.getConstructor(TradeRepository::class.java).newInstance(repository)
    }
}
