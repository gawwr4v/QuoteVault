package com.quotevault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quotevault.domain.model.Quote

@Composable
fun MinimalTemplate(quote: Quote) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(300.dp)
            .background(Color.White)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\"${quote.content}\"",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "- ${quote.author ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BoldTemplate(quote: Quote) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(300.dp)
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = quote.content.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = quote.author?.uppercase() ?: "UNKNOWN",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Red
            )
        }
    }
}

@Composable
fun ArtisticTemplate(quote: Quote) {
    Box(
        modifier = Modifier
            .width(300.dp)
            .height(300.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6200EA), Color(0xFF03DAC5))
                )
            )
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
             Text(
                text = "\"",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = quote.content,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MaterialTheme.typography.titleLarge.fontFamily),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "~ ${quote.author}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}
