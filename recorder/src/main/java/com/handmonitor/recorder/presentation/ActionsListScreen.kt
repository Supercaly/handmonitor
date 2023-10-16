package com.handmonitor.recorder.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.handmonitor.recorder.R
import com.handmonitor.recorder.data.Action
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ActionsList(
    actions: StateFlow<Map<Action.Type, Action.TimeRange>>,
    onActionSelected: (action: Action.Type) -> Unit
) {
    val actionsState by actions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.action_list_title),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title2
        )
        for (element in actionsState.iterator()) {
            ActionButton(
                actionType = element.key,
                actionTimeRange = element.value,
                onClick = { onActionSelected(element.key) }
            )
        }
    }
}
