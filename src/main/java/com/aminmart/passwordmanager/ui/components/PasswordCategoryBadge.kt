package com.aminmart.passwordmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aminmart.passwordmanager.domain.model.PasswordCategory

@Composable
fun PasswordCategoryBadge(
    category: PasswordCategory,
    modifier: Modifier = Modifier
) {
    val (color, letter) = when (category) {
        PasswordCategory.SOCIAL -> MaterialTheme.colorScheme.primary to "S"
        PasswordCategory.EMAIL -> MaterialTheme.colorScheme.secondary to "E"
        PasswordCategory.SHOPPING -> MaterialTheme.colorScheme.tertiary to "S"
        PasswordCategory.FINANCE -> MaterialTheme.colorScheme.error to "F"
        PasswordCategory.ENTERTAINMENT -> MaterialTheme.colorScheme.primaryContainer to "E"
        PasswordCategory.WORK -> MaterialTheme.colorScheme.secondaryContainer to "W"
        PasswordCategory.OTHER -> MaterialTheme.colorScheme.outline to "O"
    }
    
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
