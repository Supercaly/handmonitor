package com.handmonitor.recorder.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.handmonitor.recorder.R
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
                text = stringResource(id = R.string.action_list_title),
                modifier = Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.title2
            )
        }
        for (element in actionsTime.iterator()) {
            item {
                ActionButton(
                    actionType = element.key,
                    actionTimeRange = element.value,
                    onClick = { onActionSelected(element.key) }
                )
            }
        }
    }
}
