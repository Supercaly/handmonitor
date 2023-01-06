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
    actionsTime: Map<Action.Type, Action.TimeRange>,
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
        for (t in Action.Type.values()) {
            item {
                ActionButton(
                    actionType = t,
                    actionTimeRange = actionsTime[t]!!,
                    onClick = { onActionSelected(t) }
                )
            }
        }
    }
}
