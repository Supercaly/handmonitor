package com.handmonitor.recorder.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.handmonitor.recorder.data.Action

@Composable
fun ActionButton(
    actionType: Action.Type,
    actionTimeRange: Action.TimeRange,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        label = {
            Text(text = stringResource(id = getTitleFromAction(actionType)))
        },
        secondaryLabel = {
            Text(
                text = "${formatTime(actionTimeRange.remaining)}/${formatTime(actionTimeRange.total)} min",
                color = if (actionTimeRange.remaining < actionTimeRange.total) {
                    MaterialTheme.colors.onSurface
                } else {
                    MaterialTheme.colors.error
                }
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = getIconFromAction(actionType)),
                contentDescription = "action",
                tint = MaterialTheme.colors.secondary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        colors = ChipDefaults.chipColors(
            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.2f)
        )
    )
}

private fun formatTime(timeMs: Long): String {
    val min = (timeMs / 1_000) / 60
    val sec = (timeMs / 1_000) % 60
    return "%d".format(min) + if (sec > 0) ".%1d".format(sec) else ""
}
