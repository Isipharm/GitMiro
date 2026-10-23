package com.hugodehu.gitmiro.theme

import androidx.compose.ui.graphics.Color

val defaultGraphColors = listOf(
    Color(0xFF42a5f5),
    Color(0xFFef5350),
    Color(0xFF78909c),
    Color(0xFFff7043),
    Color(0xFF66bb6a),
    Color(0xFFec407a),
)

val lightTheme = ColorsScheme(
    primary = Color(0xFF0070D8),
    primaryVariant = Color(0xFF0070D8),
    onPrimary = Color(0xFFFFFFFFF),
    secondary = Color(0xFF9c27b0),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF212934),
    onBackgroundSecondary = Color(0xFF595858),
    error = Color(0xFFc93838),
    onError = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    backgroundSelected = Color(0xC0cee1f2),
    surface = Color(0xFFF8F8F8),
    secondarySurface = Color(0xFFE9ECEF),
    tertiarySurface = Color(0xFFE9ECEF),
    addFile = Color(0xFF32A852),
    deletedFile = Color(0xFFc93838),
    modifiedFile = Color(0xFF0070D8),
    conflictingFile = Color(0xFFFFB638),
    dialogOverlay = Color(0xAA000000),
    normalScrollbar = Color(0xFFCCCCCC),
    hoverScrollbar = Color(0xFF0070D8),
    diffLineAdded = Color(0xAAd7ebd0),
    diffContentAdded = Color(0x4570df70),
    diffLineRemoved = Color(0xAAf0d4d4),
    diffContentRemoved = Color(0x45FF0000),
    diffKeyword = Color(0xff3b83cf),
    diffAnnotation = Color(0xff9f9927),
    diffComment = Color(0xff0d9141),
    isLight = true,
)


val darkTheme = ColorsScheme(
    primary = Color(0xFF0169cb),
    primaryVariant = Color(0xFF9FD1FF),
    onPrimary = Color(0xFFFFFFFFF),
    secondary = Color(0xFFe9c754),
    onSecondary = Color(0xFF0E1621),
    onBackground = Color(0xFFFFFFFF),
    onBackgroundSecondary = Color(0xFFCCCBCB),
    error = Color(0xFFc93838),
    onError = Color(0xFFFFFFFF),
    background = Color(0xff0F0F0F),
    backgroundSelected = Color(0xFF2f3640),
    surface = Color(0xFF1F1F22),
    secondarySurface = Color(0xFF1a2b3d),
    tertiarySurface = Color(0xFF0a335c),
    addFile = Color(0xFF32A852),
    deletedFile = Color(0xFFc93838),
    modifiedFile = Color(0xFF0070D8),
    conflictingFile = Color(0xFFFFB638),
    dialogOverlay = Color(0xAA000000),
    normalScrollbar = Color(0xFF888888),
    hoverScrollbar = Color(0xFFCCCCCC),
    diffLineAdded = Color(0xAA566f5a),
    diffContentAdded = Color(0x4500FF00),
    diffLineRemoved = Color(0xAA6f585e),
    diffContentRemoved = Color(0x45FF0000),
    diffKeyword = Color(0xffa3caf1),
    diffAnnotation = Color(0xFFB3AE5F),
    diffComment = Color(0xFF70C290),
    isLight = false,
)

// Dark theme used before the 2.0 redesign, with blueish background and surfaces
val darkBlueTheme = darkTheme.copy(
    background = Color(0xFF10161e),
    surface = Color(0xff1d2630),
)
