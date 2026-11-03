package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.hugodehu.gitmiro.theme.tertiarySurface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.ui.resizePointerIconEast

const val MAX_SIDE_PANE_PROPORTION = 0.4f

@Composable
fun TripleVerticalSplitPanel(
    modifier: Modifier = Modifier,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    firstWidth: Float,
    thirdWidth: Float,
    onFirstSizeDragStarted: (Float) -> Unit,
    onFirstSizeChange: (Float) -> Unit,
    onFirstSizeDragStopped: () -> Unit,
    onThirdSizeDragStarted: (Float) -> Unit,
    onThirdSizeChange: (Float) -> Unit,
    onThirdSizeDragStopped: () -> Unit,
) {
    val density = LocalDensity.current.density

    // The panes width used to be calculated in the remember lambda, but as it required the view to compose once to get
    // the composable width, it was flickering, so we only update them when size of the parent component changes
    var firstWidthLimited by remember(firstWidth) { mutableStateOf(firstWidth) }
    var thirdWidthLimited by remember(thirdWidth) { mutableStateOf(thirdWidth) }

    fun updateMaxPaneWidthToComponentWidth(screenWidth: Int) {
        val screenWidthInDp = screenWidth / density
        firstWidthLimited = calcLimitedWidth(screenWidthInDp, firstWidth)
        thirdWidthLimited = calcLimitedWidth(screenWidthInDp, thirdWidth)
    }

    Row(
        modifier = modifier
            .onSizeChanged {
                updateMaxPaneWidthToComponentWidth(it.width)
            }
    ) {
        if (firstWidth > 0) {
            Box(
                modifier = Modifier
                    .width(firstWidth.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colors.tertiarySurface)
            ) {
                first()
            }
            PaneDragHandle(
                modifier = Modifier
                    .draggable(
                        state = rememberDraggableState {
                            onFirstSizeChange(it)
                        },
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            onFirstSizeDragStarted(firstWidthLimited)
                        },
                        onDragStopped = {
                            onFirstSizeDragStopped()
                        },
                    )
                    .pointerHoverIcon(resizePointerIconEast)
            )
        }

        Box(
            Modifier
                .weight(1f, true)
                .fillMaxHeight()
        ) {
            second()
        }

        if (thirdWidth > 0) {
            PaneDragHandle(
                modifier = Modifier
                    .draggable(
                        state = rememberDraggableState {
                            onThirdSizeChange(it)
                        },
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            onThirdSizeDragStarted(thirdWidthLimited)
                        },
                        onDragStopped = {
                            onThirdSizeDragStopped()
                        },
                    )
                    .pointerHoverIcon(resizePointerIconEast)
            )

            Box(
                modifier = Modifier
                    .width(thirdWidth.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colors.tertiarySurface)
            ) {
                third()
            }
        }
    }
}

/**
 * Drag area between two panes, drawn as the one pixel line of the mockup.
 */
@Composable
private fun PaneDragHandle(modifier: Modifier) {
    // Wider than the visible line to leave a comfortable area to grab
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(5.dp)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.08f))
        )
    }
}

private fun calcLimitedWidth(maxSize: Float, currentSize: Float): Float {
    return if (currentSize > maxSize * MAX_SIDE_PANE_PROPORTION) {
        maxSize * MAX_SIDE_PANE_PROPORTION
    } else {
        currentSize
    }
}