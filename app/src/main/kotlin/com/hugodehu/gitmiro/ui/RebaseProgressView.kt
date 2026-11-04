package com.hugodehu.gitmiro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hugodehu.gitmiro.app.generated.resources.Res
import com.hugodehu.gitmiro.app.generated.resources.done
import com.hugodehu.gitmiro.domain.models.RebaseProgress
import com.hugodehu.gitmiro.domain.models.RebaseProgressStep
import com.hugodehu.gitmiro.theme.graphColors
import com.hugodehu.gitmiro.theme.monoTypography
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import org.jetbrains.compose.resources.painterResource

/**
 * Timeline of the commits replayed by the rebase in progress, shown in place of the log.
 */
@Composable
fun RebaseProgressView(progress: RebaseProgress) {
    val amber = MaterialTheme.colors.graphColors[3]
    val green = MaterialTheme.colors.graphColors[2]
    val steps = progress.steps
    val doneCount = progress.doneCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "REPLAYING ${steps.size} COMMITS",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onBackgroundSecondary,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.1f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(if (steps.isEmpty()) 0f else doneCount / steps.size.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colors.primary)
                )
            }

            Text(
                text = "$doneCount done",
                fontFamily = monoTypography(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
            )
        }

        LazyColumn {
            itemsIndexed(steps) { index, step ->
                val (circleColor, borderColor) = when (step.status) {
                    RebaseProgressStep.Status.DONE -> green to green
                    RebaseProgressStep.Status.CURRENT -> amber.copy(alpha = 0.16f) to amber
                    RebaseProgressStep.Status.TODO -> Color.Transparent to MaterialTheme.colors.onBackground.copy(alpha = 0.2f)
                }
                val isCurrent = step.status == RebaseProgressStep.Status.CURRENT

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(2.dp, borderColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (step.status) {
                                RebaseProgressStep.Status.DONE -> Icon(
                                    painter = painterResource(Res.drawable.done),
                                    contentDescription = "Applied",
                                    tint = MaterialTheme.colors.background,
                                    modifier = Modifier.size(14.dp),
                                )

                                RebaseProgressStep.Status.CURRENT -> Text(
                                    text = "!",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.body2,
                                    color = amber,
                                )

                                RebaseProgressStep.Status.TODO -> Box(
                                    Modifier.size(4.dp).clip(CircleShape).background(borderColor)
                                )
                            }
                        }

                        if (index != steps.lastIndex) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .weight(1f)
                                    .heightIn(min = 22.dp)
                                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.1f))
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(top = 2.dp, bottom = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = step.message,
                            style = MaterialTheme.typography.body1,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (step.status == RebaseProgressStep.Status.DONE) {
                                MaterialTheme.colors.onBackgroundSecondary
                            } else {
                                MaterialTheme.colors.onBackground
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = when (step.status) {
                                RebaseProgressStep.Status.DONE -> "Applied"
                                RebaseProgressStep.Status.CURRENT -> "Stopped here, waiting for you"
                                RebaseProgressStep.Status.TODO -> "Waiting"
                            },
                            style = MaterialTheme.typography.caption,
                            color = if (isCurrent) amber else MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}
