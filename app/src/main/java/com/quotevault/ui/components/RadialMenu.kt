/**
 * RadialMenu
 * 
 * A custom, drag-to-select radial menu component built entirely in Jetpack Compose.
 * Features dynamic angle calculation to prevent edge clipping, gesture tracking, 
 * and continuous physics-based selection.
 * 
 * Designed and originally developed by @gawwr4v (https://github.com/gawwr4v).
 * If you use or adapt this complex component in your own projects, a credit or 
 * mention would be greatly appreciated! :)
 *
 */




package com.quotevault.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

private const val TAG = "RadialMenu"
private const val LONG_PRESS_TIMEOUT_MS = 400L
private const val DOUBLE_TAP_TIMEOUT_MS = 300L
private const val DEAD_ZONE_PX = 50f
private const val MENU_RADIUS_DP = 90f
private const val ICON_SIZE_DP = 32f

enum class RadialAction { LIKE, SHARE, COLLECT }

// Removed discrete MenuZone enum - now using continuous angle calculation

data class RadialMenuState(
    val isVisible: Boolean = false,
    val touchPosition: Offset = Offset.Zero,
    val dragOffset: Offset = Offset.Zero,
    val currentSelection: RadialAction? = null,
    val centerAngle: Float = 270f  // Dynamic angle - icons fan from this direction
)

// Global state for radial menu - shared across all instances
private val globalMenuState = mutableStateOf(RadialMenuState())

@Composable
fun RadialMenuWrapper(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onCollect: () -> Unit,
    isLiked: Boolean,
    isCollected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var containerPosition by remember { mutableStateOf(Offset.Zero) }
    
    val touchSlop = with(density) { 20.dp.toPx() }
    val menuRadiusPx = with(density) { MENU_RADIUS_DP.dp.toPx() }
    
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    fun vibrate(ms: Long = 30) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }
    
    /**
     * Calculate dynamic center angle based on touch position.
     * CRITICAL: Menu should NEVER point downward (finger covers it).
     * Always fan upward or sideways.
     * 
     * Angle guide (0° = right, 90° = down, 180° = left, 270° = up):
     * - We want angles between 180° and 360° (upper half only)
     * - Left edge: ~330° (strongly up-right to avoid left edge clip)
     * - Center: ~270° (straight up)  
     * - Right edge: ~210° (strongly up-left to avoid right edge clip)
     */
    fun calculateCenterAngle(x: Float, y: Float, screenWidth: Float, screenHeight: Float): Float {
        // Calculate horizontal position ratio (0 = left edge, 1 = right edge)
        val xRatio = (x / screenWidth).coerceIn(0f, 1f)
        
        // Calculate vertical position ratio (0 = top, 1 = bottom)
        val yRatio = (y / screenHeight).coerceIn(0f, 1f)
        
        // Base angle: smoothly interpolate based on X position
        // Far left (xRatio=0): angle = 330° (up-right, avoiding left edge)
        // Center (xRatio=0.5): angle = 270° (straight up)
        // Far right (xRatio=1): angle = 210° (up-left, avoiding right edge)
        val baseAngle = 330f - (xRatio * 120f)  // 330° -> 270° -> 210°
        
        // Near edges, tilt even more aggressively sideways to prevent clipping
        val edgeBoost = when {
            xRatio < 0.15f -> (0.15f - xRatio) * 100f  // Extra tilt right at left edge
            xRatio > 0.85f -> (xRatio - 0.85f) * -100f // Extra tilt left at right edge
            else -> 0f
        }
        
        // Near top of screen, tilt more sideways (but still upward, never down!)
        val topAdjust = if (yRatio < 0.25f) {
            // Near top: tilt more towards the center of screen
            if (xRatio < 0.5f) {
                -20f  // Near top-left: tilt less right (more toward center)
            } else {
                20f   // Near top-right: tilt less left (more toward center)
            }
        } else 0f
        
        // Final angle: always between 180° and 360° (upper half - never points down)
        val finalAngle = (baseAngle + edgeBoost + topAdjust).coerceIn(195f, 345f)
        
        return finalAngle
    }
    
    fun normalizeAngle(angle: Float): Float = ((angle % 360f) + 360f) % 360f
    
    fun getSelectionFromDrag(dragOffset: Offset, centerAngle: Float): RadialAction? {
        val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
        if (distance < DEAD_ZONE_PX) return null
        
        val rawAngle = Math.toDegrees(atan2(dragOffset.y.toDouble(), dragOffset.x.toDouble())).toFloat()
        val angle = normalizeAngle(rawAngle)
        val relativeAngle = normalizeAngle(angle - centerAngle)
        
        // Fan spans 120° centered on centerAngle
        // Divide into 3 sections of 40° each
        return when {
            relativeAngle in 300f..340f -> RadialAction.LIKE      // Left section
            relativeAngle in 340f..360f || relativeAngle in 0f..20f -> RadialAction.SHARE  // Center
            relativeAngle in 20f..60f -> RadialAction.COLLECT    // Right section
            else -> null
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                containerPosition = coords.positionInRoot()
            }
    ) {
        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    val screenWidth = view.width.toFloat()
                    val screenHeight = view.height.toFloat()
                    
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPosition = down.position
                        val pointerId = down.id
                        val downTime = System.currentTimeMillis()
                        
                        // Calculate absolute screen position
                        val absolutePosition = containerPosition + startPosition
                        
                        Log.d(TAG, "DOWN local=$startPosition, absolute=$absolutePosition")
                        
                        var isLongPress = false
                        var moved = false
                        var released = false
                        var upTime = 0L
                        
                        // Poll for events and check time manually
                        while (!released && !isLongPress) {
                            val currentTime = System.currentTimeMillis()
                            val elapsed = currentTime - downTime
                            
                            if (elapsed >= LONG_PRESS_TIMEOUT_MS && !moved) {
                                isLongPress = true
                                Log.d(TAG, "LONG PRESS! elapsed=$elapsed")
                                break
                            }
                            
                            try {
                                val event = withTimeoutOrNull(50) {
                                    awaitPointerEvent(PointerEventPass.Main)
                                }
                                
                                if (event != null) {
                                    val change = event.changes.find { it.id == pointerId }
                                    
                                    if (change == null || !change.pressed) {
                                        released = true
                                        upTime = System.currentTimeMillis()
                                        break
                                    }
                                    
                                    val dist = sqrt(
                                        (change.position.x - startPosition.x).let { it * it } +
                                        (change.position.y - startPosition.y).let { it * it }
                                    )
                                    if (dist > touchSlop) {
                                        moved = true
                                        Log.d(TAG, "Moved: $dist")
                                    }
                                }
                            } catch (e: Exception) {
                                // Timeout - continue
                            }
                        }
                        
                        if (isLongPress) {
                            vibrate(50)
                            
                            // Calculate dynamic angle based on position
                            val centerAngle = calculateCenterAngle(
                                absolutePosition.x, 
                                absolutePosition.y, 
                                screenWidth, 
                                screenHeight
                            )
                            
                            // No clamping - menu appears at exact touch position like Pinterest
                            globalMenuState.value = RadialMenuState(
                                isVisible = true,
                                touchPosition = absolutePosition,
                                dragOffset = Offset.Zero,
                                currentSelection = null,
                                centerAngle = centerAngle
                            )
                            
                            Log.d(TAG, "Menu at ${absolutePosition.x}, ${absolutePosition.y}, angle=$centerAngle")
                            
                            // Track drag with explicit position tracking
                            var lastPosition = startPosition
                            var currentDrag = Offset.Zero
                            var currentSelection: RadialAction? = null
                            
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.find { it.id == pointerId }
                                
                                if (change == null) {
                                    Log.d(TAG, "Pointer lost!")
                                    break
                                }
                                
                                // Calculate delta manually
                                val currentPos = change.position
                                val delta = currentPos - lastPosition
                                lastPosition = currentPos
                                currentDrag += delta
                                
                                change.consume()
                                
                                if (!change.pressed) {
                                    Log.d(TAG, "Released, selection=$currentSelection, totalDrag=$currentDrag")
                                    when (currentSelection) {
                                        RadialAction.LIKE -> {
                                            vibrate(30)
                                            onLike()
                                        }
                                        RadialAction.SHARE -> {
                                            vibrate(30)
                                            onShare()
                                        }
                                        RadialAction.COLLECT -> {
                                            vibrate(30)
                                            onCollect()
                                        }
                                        null -> {}
                                    }
                                    break
                                }
                                
                                // Only log significant drags
                                val dragDist = sqrt(currentDrag.x * currentDrag.x + currentDrag.y * currentDrag.y)
                                if (dragDist > 30f) {
                                    val newSelection = getSelectionFromDrag(currentDrag, centerAngle)
                                    
                                    if (newSelection != currentSelection) {
                                        if (newSelection != null) {
                                            vibrate(20)
                                        }
                                        Log.d(TAG, "Selection changed: $currentSelection -> $newSelection (drag=$currentDrag)")
                                        currentSelection = newSelection
                                    }
                                    
                                    globalMenuState.value = globalMenuState.value.copy(
                                        dragOffset = currentDrag,
                                        currentSelection = currentSelection
                                    )
                                }
                            }
                            
                            globalMenuState.value = RadialMenuState()
                            
                        } else if (!moved && released) {
                            val timeSinceLastTap = downTime - lastTapTime
                            
                            if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && timeSinceLastTap > 0) {
                                Log.d(TAG, "DOUBLE TAP!")
                                onDoubleTap()
                                lastTapTime = 0L
                            } else {
                                Log.d(TAG, "TAP")
                                lastTapTime = downTime
                                onTap()
                            }
                        }
                    }
                }
        ) {
            content()
        }
    }
}

private suspend fun <T> withTimeoutOrNull(timeMillis: Long, block: suspend () -> T): T? {
    return try {
        kotlinx.coroutines.withTimeoutOrNull(timeMillis) { block() }
    } catch (e: Exception) {
        null
    }
}

/**
 * Place this at the root of your app (e.g., in MainActivity or NavHost wrapper)
 * to display the radial menu overlay.
 */
@Composable
fun RadialMenuOverlay(
    isLiked: Boolean = false,
    isCollected: Boolean = false
) {
    val menuState by globalMenuState
    
    if (menuState.isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            RadialMenuCanvas(
                center = menuState.touchPosition,
                dragOffset = menuState.dragOffset,
                selection = menuState.currentSelection,
                isLiked = isLiked,
                isCollected = isCollected,
                centerAngle = menuState.centerAngle
            )
        }
    }
}

@Composable
fun RadialMenuCanvas(
    center: Offset,
    dragOffset: Offset,
    selection: RadialAction?,
    isLiked: Boolean,
    isCollected: Boolean,
    centerAngle: Float
) {
    val density = LocalDensity.current
    val menuRadius = with(density) { MENU_RADIUS_DP.dp.toPx() }
    val iconSize = with(density) { ICON_SIZE_DP.dp.toPx() }
    
    val likePainter = rememberVectorPainter(
        if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    )
    val sharePainter = rememberVectorPainter(Icons.Filled.Share)
    val collectPainter = rememberVectorPainter(
        if (isCollected) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd
    )
    
    val likeScale by animateFloatAsState(
        targetValue = if (selection == RadialAction.LIKE) 1.4f else 1.0f,
        animationSpec = tween(100),
        label = "likeScale"
    )
    val shareScale by animateFloatAsState(
        targetValue = if (selection == RadialAction.SHARE) 1.4f else 1.0f,
        animationSpec = tween(100),
        label = "shareScale"
    )
    val collectScale by animateFloatAsState(
        targetValue = if (selection == RadialAction.COLLECT) 1.4f else 1.0f,
        animationSpec = tween(100),
        label = "collectScale"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Center indicator
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = 16f,
            center = center
        )
        
        // Icon positions - spread 45° apart for better visibility
        val baseAngle = centerAngle
        val angles = listOf(baseAngle - 45f, baseAngle, baseAngle + 45f)
        val actions = listOf(
            Triple(RadialAction.LIKE, likePainter, likeScale),
            Triple(RadialAction.SHARE, sharePainter, shareScale),
            Triple(RadialAction.COLLECT, collectPainter, collectScale)
        )
        
        actions.forEachIndexed { index, (action, painter, scale) ->
            val angleRad = Math.toRadians(angles[index].toDouble())
            val iconCenter = Offset(
                center.x + (menuRadius * cos(angleRad)).toFloat(),
                center.y + (menuRadius * sin(angleRad)).toFloat()
            )
            
            val isSelected = selection == action
            val scaledSize = iconSize * scale
            val bgRadius = scaledSize * 0.75f
            
            // Background circle
            drawCircle(
                color = if (isSelected) Color.White else Color(0xFF424242),
                radius = bgRadius,
                center = iconCenter
            )
            
            // Icon
            translate(
                left = iconCenter.x - scaledSize / 2,
                top = iconCenter.y - scaledSize / 2
            ) {
                with(painter) {
                    draw(
                        size = Size(scaledSize, scaledSize),
                        alpha = 1f,
                        colorFilter = ColorFilter.tint(
                            if (isSelected) Color.Black else Color.White
                        )
                    )
                }
            }
        }
        
        // Drag direction indicator
        val dragDist = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)
        if (dragDist > 20f) {
            val indicatorLen = minOf(dragDist * 0.6f, menuRadius * 0.5f)
            val normalized = Offset(dragOffset.x / dragDist, dragOffset.y / dragDist)
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = center,
                end = Offset(
                    center.x + normalized.x * indicatorLen,
                    center.y + normalized.y * indicatorLen
                ),
                strokeWidth = 4f
            )
        }
    }
}
