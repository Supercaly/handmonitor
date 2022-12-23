package com.handmonitor.recorder.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.handmonitor.recorder.R
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
                text = "${actionTimeRange.remaining}/${actionTimeRange.total} min",
                color = if (actionTimeRange.remaining < actionTimeRange.total)
                    MaterialTheme.colors.onSurface else MaterialTheme.colors.error
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = getIconFromAction(actionType)),
                contentDescription = "wash",
                tint = MaterialTheme.colors.secondary
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ChipDefaults.chipColors(
            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.2f)
        )
    )
}

private fun getTitleFromAction(action: Action.Type) =
    when (action) {
        Action.Type.HandWash -> R.string.action_hand_wash
        Action.Type.HandRub -> R.string.action_hand_rub
        Action.Type.Eat -> R.string.action_eat
        Action.Type.TeethBrush -> R.string.action_teeth_brush
        Action.Type.FaceWash -> R.string.action_face_wash
        Action.Type.Write -> R.string.action_write
        Action.Type.Type -> R.string.action_type
        Action.Type.Housework -> R.string.action_housework
    }

private fun getIconFromAction(action: Action.Type) =
    when (action) {
        Action.Type.HandWash -> R.drawable.ic_wash
        Action.Type.HandRub -> R.drawable.ic_rub
        Action.Type.Eat -> R.drawable.ic_eat
        Action.Type.TeethBrush -> R.drawable.ic_teethbrush
        Action.Type.FaceWash -> R.drawable.ic_facewash
        Action.Type.Write -> R.drawable.ic_write
        Action.Type.Type -> R.drawable.ic_keyboard
        Action.Type.Housework -> R.drawable.ic_housework
    }