package kz.cranels.expensetracker

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kz.cranels.expensetracker.data.NotionRepository
import kz.cranels.expensetracker.data.local.AppDatabase
import kz.cranels.expensetracker.data.local.Category
import kz.cranels.expensetracker.data.notion.NotionClient
import java.util.Date

class ExpenseViewModel(private val notionRepository: NotionRepository) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val categories: StateFlow<List<Category>> = notionRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveExpense(
        token: String,
        databaseId: String,
        description: String,
        amount: String,
        categoryId: String,
        date: Date,
        onResult: (Boolean) -> Unit
    ) {
        if (_isSaving.value) return // Prevent multiple clicks
        _isSaving.value = true

        viewModelScope.launch {
            val amountAsDouble = amount.toDoubleOrNull()
            if (amountAsDouble == null) {
                onResult(false)
                _isSaving.value = false
                return@launch
            }
            try {
                val success = notionRepository.createExpense(
                    token = token,
                    databaseId = databaseId,
                    description = description,
                    amount = amountAsDouble,
                    categoryId = categoryId,
                    date = date
                )
                onResult(success)
            } finally {
                _isSaving.value = false
            }
        }
    }
}

class ExpenseViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = NotionRepository(NotionClient.apiService, database.categoryDao())
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
