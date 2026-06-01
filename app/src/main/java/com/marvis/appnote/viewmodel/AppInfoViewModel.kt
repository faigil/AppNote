package com.marvis.appnote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marvis.appnote.data.entity.AppInfo
import com.marvis.appnote.repository.AppInfoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppInfoViewModel(private val repository: AppInfoRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _currentTab = MutableStateFlow(0) // 0=all, 1=system, 2=user

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = combine(_searchQuery, _currentTab) { query, tab ->
        Pair(query, tab)
    }.flatMapLatest { (query, tab) ->
        val baseFlow = when (tab) {
            1 -> repository.getSystemApps()
            2 -> repository.getUserApps()
            else -> repository.getAllApps()
        }
        if (query.isBlank()) baseFlow
        else repository.searchApps(query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) { _searchQuery.value = query }
    fun setTab(tab: Int) { _currentTab.value = tab }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.refreshAll()
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val repository: AppInfoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppInfoViewModel(repository) as T
        }
    }
}