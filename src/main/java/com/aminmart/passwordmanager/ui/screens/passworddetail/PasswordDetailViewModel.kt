package com.aminmart.passwordmanager.ui.screens.passworddetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.PasswordRepository
import com.aminmart.passwordmanager.domain.model.PasswordEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasswordDetailUiState(
    val password: PasswordEntry? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class PasswordDetailViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordDetailUiState())
    val uiState: StateFlow<PasswordDetailUiState> = _uiState.asStateFlow()

    fun loadPassword(passwordId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val password = passwordRepository.getPasswordById(passwordId)
                _uiState.value = _uiState.value.copy(
                    password = password,
                    isLoading = false
                )
                if (password == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Password not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
}
