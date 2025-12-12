package kz.cranels.expensetracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kz.cranels.expensetracker.data.NotionRepository
import kz.cranels.expensetracker.data.local.AppDatabase
import kz.cranels.expensetracker.data.notion.NotionClient

class SettingsViewModel(private val notionRepository: NotionRepository) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncCategories(token: String, databaseId: String, onResult: (Boolean) -> Unit) {
        if (_isSyncing.value) return
        _isSyncing.value = true

        viewModelScope.launch {
            try {
                val success = notionRepository.syncCategories(token, databaseId)
                onResult(success)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = NotionRepository(NotionClient.apiService, database.categoryDao())
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
