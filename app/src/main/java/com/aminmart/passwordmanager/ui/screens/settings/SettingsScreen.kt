package com.aminmart.passwordmanager.ui.screens.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.aminmart.passwordmanager.data.repository.ImportMode
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.requestExportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.requestImportBackup(it, ImportMode.MERGE) }
    }

    // Biometric authentication launcher
    val biometricLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Handle biometric authentication result if needed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        ) {
            // Security section
            SettingsSection(title = "Security") {
                // Biometric toggle
                SettingsSwitchItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = if (uiState.biometricAvailable) {
                        "Use fingerprint or face to unlock"
                    } else {
                        "Not available - check device settings"
                    },
                    checked = uiState.biometricEnabled && uiState.biometricAvailable,
                    enabled = uiState.biometricAvailable,
                    onCheckedChange = viewModel::toggleBiometric
                )

                HorizontalDivider()

                // Change master password
                SettingsNavItem(
                    icon = Icons.Default.LockReset,
                    title = "Change Master Password",
                    onClick = { viewModel.showChangePasswordDialog() }
                )

                HorizontalDivider()

                // Export backup
                SettingsNavItem(
                    icon = Icons.Default.Upload,
                    title = "Export Backup",
                    subtitle = "Save encrypted backup",
                    onClick = {
                        val fileName = "aminmart-backup-${System.currentTimeMillis()}.aminmartbackup"
                        exportLauncher.launch(fileName)
                    }
                )

                HorizontalDivider()

                // Import backup
                SettingsNavItem(
                    icon = Icons.Default.Download,
                    title = "Import Backup",
                    subtitle = "Restore from backup",
                    onClick = {
                        importLauncher.launch("*/*")
                    }
                )
            }

            // App section
            SettingsSection(title = "App") {
                SettingsNavItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version 1.0.0",
                    onClick = { }
                )

                HorizontalDivider()

                // Lock now
                SettingsNavItem(
                    icon = Icons.Default.Lock,
                    title = "Lock Vault",
                    onClick = { viewModel.lockVault() }
                )
            }

            // Danger zone
            SettingsSection(title = "Danger Zone") {
                SettingsNavItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete All Data",
                    subtitle = "This cannot be undone",
                    onClick = { viewModel.showDeleteConfirmDialog() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Change password dialog
    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = viewModel::hideChangePasswordDialog,
            onChangePassword = { oldPassword, newPassword, confirmPassword ->
                viewModel.changePassword(oldPassword, newPassword, confirmPassword)
            }
        )
    }

    // Password verification dialog for biometric
    if (uiState.showPasswordDialogForBiometric) {
        PasswordVerificationDialog(
            title = "Verify Password",
            description = "Enter your master password to enable biometric unlock",
            onDismiss = viewModel::cancelBiometricSetup,
            onVerify = viewModel::enableBiometricAfterVerification
        )
    }

    // Password verification dialog for export
    if (uiState.showPasswordDialogForExport) {
        PasswordVerificationDialog(
            title = "Verify Password",
            description = "Enter your master password to export backup",
            onDismiss = viewModel::cancelExport,
            onVerify = viewModel::exportBackup
        )
    }

    // Password verification dialog for import
    if (uiState.showPasswordDialogForImport) {
        PasswordVerificationDialog(
            title = "Verify Password",
            description = "Enter your master password to import backup",
            onDismiss = viewModel::cancelImport,
            onVerify = viewModel::importBackup
        )
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirmDialog,
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null)
            },
            title = { Text("Delete All Data?") },
            text = {
                Text("This will permanently delete all passwords and settings. This action cannot be undone!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirmDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Status messages
    uiState.statusMessage?.let { message ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        ) {
            Text(message)
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled
        )
    }
}

@Composable
private fun PasswordVerificationDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(description)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Master Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotEmpty()) {
                        onVerify(password)
                    }
                }
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Master Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Old Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onChangePassword(oldPassword, newPassword, confirmPassword)
                }
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
