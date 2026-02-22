package com.aminmart.passwordmanager.ui.screens.addeditpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminmart.passwordmanager.data.repository.PasswordRepository
import com.aminmart.passwordmanager.data.security.PasswordGeneratorService
import com.aminmart.passwordmanager.data.security.PasswordStrength
import com.aminmart.passwordmanager.domain.model.CreatePasswordInput
import com.aminmart.passwordmanager.domain.model.PasswordCategory
import com.aminmart.passwordmanager.domain.model.UpdatePasswordInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditPasswordUiState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",
    val category: PasswordCategory = PasswordCategory.OTHER,
    
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    
    // Password generator
    val showPasswordGenerator: Boolean = false,
    val generatedPassword: String = "",
    val passwordLength: Int = 16,
    val passwordStrength: PasswordStrength? = null
)

@HiltViewModel
class AddEditPasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val passwordGeneratorService: PasswordGeneratorService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val passwordId: Long? = savedStateHandle.get<Long>("passwordId")
    
    private val _uiState = MutableStateFlow(AddEditPasswordUiState())
    val uiState: StateFlow<AddEditPasswordUiState> = _uiState.asStateFlow()

    init {
        if (passwordId != null) {
            loadPassword(passwordId)
        }
    }

    private fun loadPassword(passwordId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val password = passwordRepository.getPasswordById(passwordId)
                if (password != null) {
                    _uiState.value = _uiState.value.copy(
                        title = password.title,
                        username = password.username,
                        password = password.password,
                        website = password.website,
                        notes = password.notes,
                        category = password.category,
                        isEditMode = true,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
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

    fun onTitleChange(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordStrength = passwordGeneratorService.getPasswordStrength(
                passwordGeneratorService.calculateEntropy(password)
            )
        )
    }

    fun onWebsiteChange(website: String) {
        _uiState.value = _uiState.value.copy(website = website)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onCategoryChange(category: PasswordCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun generatePassword() {
        val length = _uiState.value.passwordLength
        val generated = passwordGeneratorService.generatePassword(length = length)
        _uiState.value = _uiState.value.copy(
            generatedPassword = generated,
            password = generated,
            passwordStrength = passwordGeneratorService.getPasswordStrength(
                passwordGeneratorService.calculateEntropy(generated)
            )
        )
    }

    fun setPasswordLength(length: Int) {
        _uiState.value = _uiState.value.copy(passwordLength = length)
    }

    fun savePassword() {
        viewModelScope.launch {
            val state = _uiState.value
            
            if (state.title.isBlank()) {
                _uiState.value = state.copy(errorMessage = "Title is required")
                return@launch
            }
            
            if (state.password.isBlank()) {
                _uiState.value = state.copy(errorMessage = "Password is required")
                return@launch
            }

            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            try {
                if (state.isEditMode && passwordId != null) {
                    passwordRepository.updatePassword(
                        UpdatePasswordInput(
                            id = passwordId,
                            title = state.title,
                            username = state.username,
                            password = state.password,
                            website = state.website,
                            notes = state.notes,
                            category = state.category
                        )
                    )
                } else {
                    passwordRepository.createPassword(
                        CreatePasswordInput(
                            title = state.title,
                            username = state.username,
                            password = state.password,
                            website = state.website,
                            notes = state.notes,
                            category = state.category
                        )
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
