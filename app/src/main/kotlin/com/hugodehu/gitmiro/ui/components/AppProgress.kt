package com.hugodehu.gitmiro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thin rounded progress bar. Without [progress] a segment slides back and forth.
 */
@Composable
fun AppProgressBar(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = MaterialTheme.colors.primary,
) {
    val track = MaterialTheme.colors.onBackground.copy(alpha = 0.1f)
    val transition = rememberInfiniteTransition(label = "progress")
    val position by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "slide",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(track),
    ) {
        val start = if (progress == null) (size.width * position).coerceAtLeast(0f) else 0f
        val end = if (progress == null) {
            (size.width * (position + 0.35f)).coerceAtMost(size.width)
        } else {
            size.width * progress.coerceIn(0f, 1f)
        }

        if (end > start) drawRoundRect(
            color = color,
            topLeft = Offset(start, 0f),
            size = Size(end - start, size.height),
            cornerRadius = CornerRadius(size.height / 2),
        )
    }
}

/**
 * Small spinner, a rotating arc.
 */
@Composable
fun AppSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = MaterialTheme.colors.primary,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "rotation",
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension / 8f

        rotate(angle) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(this.size.width - strokeWidth, this.size.height - strokeWidth),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}
