package com.aminmart.passwordmanager.ui.screens.passwordlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aminmart.passwordmanager.domain.model.PasswordEntry
import com.aminmart.passwordmanager.ui.components.PasswordCategoryBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
    viewModel: PasswordListViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            viewModel.clearError()
        }
    }

    // Refresh passwords when entering the screen
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    // Refresh passwords when returning from other screens (e.g., after adding/editing)
    LaunchedEffect(uiState.refreshTrigger) {
        if (uiState.refreshTrigger > 0) {
            viewModel.loadPasswords()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aminmart Password Manager") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add password")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = { Text("Search passwords...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.passwords.isEmpty()) {
                EmptyPasswordList(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.passwords, key = { it.id }) { password ->
                        PasswordListItem(
                            password = password,
                            onClick = { onNavigateToDetail(password.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPasswordList(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🔒",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No passwords yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Tap the + button to add your first password",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PasswordListItem(
    password: PasswordEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = getCategoryIcon(password.category),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // Password info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = password.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = password.username.ifEmpty { "No username" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Category badge
            PasswordCategoryBadge(category = password.category)
        }
    }
}

private fun getCategoryIcon(category: com.aminmart.passwordmanager.domain.model.PasswordCategory): String {
    return when (category) {
        com.aminmart.passwordmanager.domain.model.PasswordCategory.SOCIAL -> "📱"
        com.aminmart.passwordmanager.domain.model.PasswordCategory.EMAIL -> "📧"
        com.aminmart.passwordmanager.domain.model.PasswordCategory.SHOPPING -> "🛒"
        com.aminmart.passwordmanager.domain.model.PasswordCategory.FINANCE -> "💰"
        com.aminmart.passwordmanager.domain.model.PasswordCategory.ENTERTAINMENT -> "🎬"
        com.aminmart.passwordmanager.domain.model.PasswordCategory.WORK -> "💼"
        com.aminmart.passwordmanager.domain.model.PasswordCategory.OTHER -> "🔐"
    }
}
