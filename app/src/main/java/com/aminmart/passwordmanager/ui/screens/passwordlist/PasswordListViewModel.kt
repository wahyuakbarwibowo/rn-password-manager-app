package com.aminmart.passwordmanager.ui.screens.passwordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.PasswordRepository
import com.aminmart.passwordmanager.domain.model.PasswordEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasswordListUiState(
    val passwords: List<PasswordEntry> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val refreshTrigger: Int = 0
)

@HiltViewModel
class PasswordListViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordListUiState())
    val uiState: StateFlow<PasswordListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        loadPasswords()
    }

    fun loadPasswords() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                passwordRepository.getAllPasswords(),
                _searchQuery
            ) { passwords, query ->
                if (query.isBlank()) {
                    passwords
                } else {
                    passwords.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true) ||
                        it.website.contains(query, ignoreCase = true)
                    }
                }
            }.catch { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }.collect { passwords ->
                _uiState.value = _uiState.value.copy(
                    passwords = passwords,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun refresh() {
        // Increment refresh trigger to force reload
        _uiState.value = _uiState.value.copy(
            refreshTrigger = _uiState.value.refreshTrigger + 1
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
