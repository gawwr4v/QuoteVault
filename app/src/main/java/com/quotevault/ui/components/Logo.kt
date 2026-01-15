package com.quotevault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun QuoteVaultLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    androidx.compose.material3.Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
