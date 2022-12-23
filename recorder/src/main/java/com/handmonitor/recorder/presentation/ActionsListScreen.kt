package com.handmonitor.recorder.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.handmonitor.recorder.data.Action

@Composable
fun ActionsList(
    onActionSelected: (action: Action.Type) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Record Action",
                modifier = Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.title2
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.HandWash,
                actionTimeRange = Action.TimeRange(4, 10),
                onClick = { onActionSelected(Action.Type.HandWash) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.HandRub,
                actionTimeRange = Action.TimeRange(5, 10),
                onClick = { onActionSelected(Action.Type.HandRub) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.Eat,
                actionTimeRange = Action.TimeRange(6, 10),
                onClick = { onActionSelected(Action.Type.Eat) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.TeethBrush,
                actionTimeRange = Action.TimeRange(8, 10),
                onClick = { onActionSelected(Action.Type.TeethBrush) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.FaceWash,
                actionTimeRange = Action.TimeRange(10, 10),
                onClick = { onActionSelected(Action.Type.FaceWash) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.Write,
                actionTimeRange = Action.TimeRange(11, 10),
                onClick = { onActionSelected(Action.Type.Write) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.Type,
                actionTimeRange = Action.TimeRange(15, 10),
                onClick = { onActionSelected(Action.Type.Type) }
            )
        }
        item {
            ActionButton(
                actionType = Action.Type.Housework,
                actionTimeRange = Action.TimeRange(30, 10),
                onClick = { onActionSelected(Action.Type.Housework) }
            )
        }
    }
}
