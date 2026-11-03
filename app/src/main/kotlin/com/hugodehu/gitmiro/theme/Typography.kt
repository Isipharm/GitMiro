package com.hugodehu.gitmiro.theme

import androidx.compose.material.Colors
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hugodehu.gitmiro.app.generated.resources.Geist_Bold
import com.hugodehu.gitmiro.app.generated.resources.Geist_BoldItalic
import com.hugodehu.gitmiro.app.generated.resources.Geist_Italic
import com.hugodehu.gitmiro.app.generated.resources.Geist_Medium
import com.hugodehu.gitmiro.app.generated.resources.Geist_MediumItalic
import com.hugodehu.gitmiro.app.generated.resources.Geist_Regular
import com.hugodehu.gitmiro.app.generated.resources.Geist_SemiBold
import com.hugodehu.gitmiro.app.generated.resources.Geist_SemiBoldItalic
import com.hugodehu.gitmiro.app.generated.resources.GeistMono_Bold
import com.hugodehu.gitmiro.app.generated.resources.GeistMono_Medium
import com.hugodehu.gitmiro.app.generated.resources.GeistMono_Regular
import com.hugodehu.gitmiro.app.generated.resources.GeistMono_SemiBold
import com.hugodehu.gitmiro.app.generated.resources.Res
import org.jetbrains.compose.resources.Font


const val LETTER_SPACING = 0.1

@Composable
fun typography(composeColors: Colors): Typography {
    val geistFontFamily = FontFamily(
        Font(Res.font.Geist_Regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.Geist_Italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.Geist_Medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.Geist_MediumItalic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.Geist_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.Geist_SemiBoldItalic, FontWeight.SemiBold, FontStyle.Italic),
        Font(Res.font.Geist_Bold, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.Geist_BoldItalic, FontWeight.Bold, FontStyle.Italic),
    )

    return Typography(
        defaultFontFamily = geistFontFamily,
        h1 = TextStyle(
            fontSize = 32.sp,
            color = composeColors.onBackground,
            letterSpacing = LETTER_SPACING.sp,
        ),
        h2 = TextStyle(
            fontSize = 24.sp,
            color = composeColors.onBackground,
            letterSpacing = LETTER_SPACING.sp,
        ),
        h3 = TextStyle(
            fontSize = 20.sp,
            color = composeColors.onBackground,
            letterSpacing = LETTER_SPACING.sp,
        ),
        h4 = TextStyle(
            fontSize = 16.sp,
            color = composeColors.onBackground,
            letterSpacing = LETTER_SPACING.sp,
        ),
        body1 = TextStyle(
            fontSize = 14.sp,
            color = composeColors.onBackground,
            letterSpacing = LETTER_SPACING.sp,
        ),
        body2 = TextStyle(
            fontSize = 13.sp,
            color = composeColors.onBackground,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.1.sp,
        ),
        caption = TextStyle(
            fontSize = 11.sp,
            color = composeColors.onBackground,
            letterSpacing = LETTER_SPACING.sp,
        )
    )
}

@Composable
fun monoTypography(): FontFamily {
    val geistMonoFontFamily = FontFamily(
        Font(Res.font.GeistMono_Regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.GeistMono_Medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.GeistMono_SemiBold, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.GeistMono_Bold, FontWeight.Bold, FontStyle.Normal),
    )

    return geistMonoFontFamily
}