package com.aminmart.passwordmanager.ui.screens.addeditpassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aminmart.passwordmanager.domain.model.PasswordCategory
import com.aminmart.passwordmanager.ui.components.FormActions
import com.aminmart.passwordmanager.ui.components.PasswordTextField
import com.aminmart.passwordmanager.ui.components.RegularTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPasswordScreen(
    passwordId: Long?,
    viewModel: AddEditPasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Password" else "Add Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            RegularTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = "Title",
                placeholder = "e.g., Gmail, Facebook",
                isError = uiState.errorMessage != null && uiState.title.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            
            // Username
            RegularTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = "Username",
                placeholder = "Email or username",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            
            // Password
            PasswordTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                placeholder = "Enter password",
                isError = uiState.errorMessage != null && uiState.password.isBlank(),
                keyboardActions = KeyboardActions(onNext = { viewModel.generatePassword() })
            )
            
            // Password generator button
            OutlinedButton(
                onClick = { viewModel.generatePassword() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Strong Password")
            }
            
            // Password strength indicator
            uiState.passwordStrength?.let { strength ->
                PasswordStrengthIndicator(strength)
            }
            
            // Website
            RegularTextField(
                value = uiState.website,
                onValueChange = viewModel::onWebsiteChange,
                label = "Website",
                placeholder = "https://example.com",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            
            // Category
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    PasswordCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.displayName) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Notes
            RegularTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = "Notes",
                placeholder = "Optional notes",
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Save/Cancel buttons
            FormActions(
                onSave = viewModel::savePassword,
                onCancel = onNavigateBack,
                isLoading = uiState.isLoading,
                saveText = if (uiState.isEditMode) "Update" else "Save"
            )
        }
    }
    
    // Password generator dialog
    if (uiState.showPasswordGenerator) {
        PasswordGeneratorDialog(
            passwordLength = uiState.passwordLength,
            onLengthChange = viewModel::setPasswordLength,
            onGenerate = {
                viewModel.generatePassword()
            },
            onUsePassword = {
                viewModel.generatePassword()
            },
            onDismiss = { }
        )
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: com.aminmart.passwordmanager.data.security.PasswordStrength) {
    val (color, label) = when (strength) {
        com.aminmart.passwordmanager.data.security.PasswordStrength.WEAK ->
            MaterialTheme.colorScheme.error to "Weak"
        com.aminmart.passwordmanager.data.security.PasswordStrength.FAIR ->
            MaterialTheme.colorScheme.error to "Fair"
        com.aminmart.passwordmanager.data.security.PasswordStrength.GOOD ->
            MaterialTheme.colorScheme.primary to "Good"
        com.aminmart.passwordmanager.data.security.PasswordStrength.STRONG ->
            MaterialTheme.colorScheme.primary to "Strong"
        com.aminmart.passwordmanager.data.security.PasswordStrength.VERY_STRONG ->
            MaterialTheme.colorScheme.primary to "Very Strong"
    }

    Column {
        LinearProgressIndicator(
            progress = {
                when (strength) {
                    com.aminmart.passwordmanager.data.security.PasswordStrength.WEAK -> 0.2f
                    com.aminmart.passwordmanager.data.security.PasswordStrength.FAIR -> 0.4f
                    com.aminmart.passwordmanager.data.security.PasswordStrength.GOOD -> 0.6f
                    com.aminmart.passwordmanager.data.security.PasswordStrength.STRONG -> 0.8f
                    com.aminmart.passwordmanager.data.security.PasswordStrength.VERY_STRONG -> 1.0f
                }
            },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun PasswordGeneratorDialog(
    passwordLength: Int,
    onLengthChange: (Int) -> Unit,
    onGenerate: () -> Unit,
    onUsePassword: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Password") },
        text = {
            Column {
                Text("Password Length: $passwordLength")
                Slider(
                    value = passwordLength.toFloat(),
                    onValueChange = { onLengthChange(it.toInt()) },
                    valueRange = 8f..64f,
                    steps = 56
                )
            }
        },
        confirmButton = {
            Button(onClick = { onUsePassword(); onDismiss() }) {
                Text("Use Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private val PasswordCategory.displayName: String
    get() = when (this) {
        PasswordCategory.SOCIAL -> "Social"
        PasswordCategory.EMAIL -> "Email"
        PasswordCategory.SHOPPING -> "Shopping"
        PasswordCategory.FINANCE -> "Finance"
        PasswordCategory.ENTERTAINMENT -> "Entertainment"
        PasswordCategory.WORK -> "Work"
        PasswordCategory.OTHER -> "Other"
    }
