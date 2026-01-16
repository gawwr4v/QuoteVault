package com.quotevault.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quotevault.domain.model.Quote

enum class QuoteCardStyle {
    Standard,
    Detailed
}

@Composable
fun QuoteCard(
    quote: Quote,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToCollectionClick: () -> Unit = {},
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: QuoteCardStyle = QuoteCardStyle.Standard,
    isSaved: Boolean = false
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    var showHeartAnimation by remember { mutableStateOf(false) }

    val animatedScale by animateFloatAsState(
        targetValue = if (showHeartAnimation) 1.2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeartScale",
        finishedListener = { if (it == 1.2f) showHeartAnimation = false }
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (showHeartAnimation) 1f else 0f,
        animationSpec = tween(300),
        label = "HeartAlpha"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        RadialMenuWrapper(
            onTap = onItemClick,
            onDoubleTap = {
                onFavoriteClick()
                showHeartAnimation = true
            },
            onLike = onFavoriteClick,
            onShare = onShareClick,
            onCollect = onAddToCollectionClick,
            isLiked = quote.isFavorite,
            isCollected = isSaved
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp) // Room for shadow
                    .border(
                        width = if (!isSystemInDarkTheme) 1.5.dp else 0.dp,
                        color = if (!isSystemInDarkTheme) Color.Black else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSystemInDarkTheme) 0.dp else 6.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        Color.White
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "\"${quote.content}\"",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif,
                            lineHeight = 32.sp
                        ),
                        color = if (isSystemInDarkTheme) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color(0xFF1E1E1E)  // Soft black
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 2.dp)
                                    .background(
                                        if (isSystemInDarkTheme) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = quote.author ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isSystemInDarkTheme) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                } else {
                                    Color(0xFF666666)  // Medium gray
                                }
                            )
                        }

                        // Premium Chip: Solid background, no border
                        Surface(
                            color = if (isSystemInDarkTheme) {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = null, 
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = quote.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isSystemInDarkTheme) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Animated Heart Overlay
        if (showHeartAnimation || animatedAlpha > 0f) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                    .scale(animatedScale)
                    .alpha(animatedAlpha),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
