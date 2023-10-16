package com.handmonitor.recorder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.handmonitor.recorder.R
import com.handmonitor.recorder.data.Action
import com.handmonitor.recorder.presentation.theme.HandMonitorTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun RecordingScreen(
    elapsedTimeString: StateFlow<String>,
    recordedAction: SharedFlow<Action.Type?>,
    onStopRecording: () -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val timeString by elapsedTimeString.collectAsState()
    var action by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        recordedAction.collect {
            action = if (it != null) getTitleFromAction(it) else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.recording_screen_title),
            color = Color(0XFF979797),
            style = MaterialTheme.typography.title3
        )
        Text(
            text = if (action != null) stringResource(id = action!!) else "",
            color = Color(0XFF979797),
            style = MaterialTheme.typography.caption3
        )
        Text(
            text = timeString,
            color = MaterialTheme.colors.secondary,
            style = MaterialTheme.typography.display2
        )
        Button(onClick = {
            onStopRecording()
            showDialog = true
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pause),
                contentDescription = "pause"
            )
        }
    }
    Dialog(
        showDialog = showDialog,
        onDismissRequest = { showDialog = false }
    ) {
        Alert(
            title = { Text(text = stringResource(id = R.string.recording_dialog_title)) },
            negativeButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onDiscard()
                    },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = "cancel"
                    )
                }
            },
            positiveButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onConfirm()
                    },
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "save"
                    )
                }
            }
        ) {
            Text(text = stringResource(id = R.string.recording_dialog_msg))
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun RecordingScreenPreview() {
    HandMonitorTheme {
        val a = MutableSharedFlow<Action.Type>(0)
        LaunchedEffect(Unit) {
            a.emit(Action.Type.Eating)
        }
        RecordingScreen(
            MutableStateFlow("00:00"),
            a,
            onStopRecording = {},
            onConfirm = {},
            onDiscard = {},
        )
    }
}
