package com.hugodehu.gitmiro.ui.dialogs.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.theme.onBackgroundSecondary

/**
 * Left aligned dialog header: a tinted icon badge, the title and a subtitle.
 */
@Composable
fun DialogHeader(
    icon: Painter,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colors.primaryVariant,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 3.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}
