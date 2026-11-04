package com.hugodehu.gitmiro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.ConcurrentHashMap

val TITLE_BAR_HEIGHT = 48.dp

class TitleBarScope internal constructor(
    rowScope: RowScope,
    // Read by the AWT mouse listener only, so it does not need to be observable state
    private val clientAreas: MutableMap<Any, Rect>,
) : RowScope by rowScope {

    /**
     * Marks an interactive part of the title bar (tabs, buttons...). The rest of the title bar moves the window when
     * dragged and maximizes it when double-clicked.
     */
    @Composable
    fun Modifier.clientArea(): Modifier {
        val key = remember { Any() }

        DisposableEffect(key) {
            onDispose { clientAreas.remove(key) }
        }

        return this.onGloballyPositioned { clientAreas[key] = it.boundsInWindow() }
    }
}

/**
 * Header drawn in the title bar area of the window, next to the native minimize/maximize/close buttons.
 *
 * Requires the JetBrains Runtime. On another JDK the system title bar is kept and the header is laid out below it.
 */
@Composable
fun WindowTitleBar(
    window: ComposeWindow,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.() -> Unit,
) {
    val decorations = remember { if (JBR.isWindowDecorationsSupported()) JBR.getWindowDecorations() else null }
    val clientAreas = remember { ConcurrentHashMap<Any, Rect>() }
    var rightInset by remember { mutableStateOf(0.dp) }

    if (decorations != null) {
        val titleBar = remember { decorations.createCustomTitleBar() }

        DisposableEffect(titleBar) {
            titleBar.height = TITLE_BAR_HEIGHT.value
            titleBar.putProperty("controls.dark", isDark)
            decorations.setCustomTitleBar(window, titleBar)
            rightInset = titleBar.rightInset.dp

            // JBR needs to know, for every mouse event, if it lands on the title bar (native drag, double-click
            // and snap) or on our own controls, which then receive the event as usual
            val listener = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) = updateHitTest(e)
                override fun mouseReleased(e: MouseEvent) = updateHitTest(e)
                override fun mouseClicked(e: MouseEvent) = updateHitTest(e)
                override fun mouseEntered(e: MouseEvent) = updateHitTest(e)
                override fun mouseDragged(e: MouseEvent) = updateHitTest(e)
                override fun mouseMoved(e: MouseEvent) = updateHitTest(e)

                private fun updateHitTest(e: MouseEvent) {
                    val transform = window.graphicsConfiguration.defaultTransform
                    val point = Offset((e.x * transform.scaleX).toFloat(), (e.y * transform.scaleY).toFloat())
                    titleBar.forceHitTest(clientAreas.values.any { it.contains(point) })
                }
            }

            window.addMouseListener(listener)
            window.addMouseMotionListener(listener)

            onDispose {
                window.removeMouseListener(listener)
                window.removeMouseMotionListener(listener)
                decorations.setCustomTitleBar(window, null)
            }
        }

        LaunchedEffect(isDark) {
            titleBar.putProperty("controls.dark", isDark)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TITLE_BAR_HEIGHT)
            .padding(end = rightInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val scope = remember(this) { TitleBarScope(this, clientAreas) }
        scope.content()
    }
}
