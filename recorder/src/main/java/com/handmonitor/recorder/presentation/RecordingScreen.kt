package com.handmonitor.recorder.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.handmonitor.recorder.R
import kotlinx.coroutines.delay

@Composable
fun RecordingScreen(
    onStopRecording: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var timeElapsedSec by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000)
                timeElapsedSec++
            }
        }

        Text(
            text = "recording",
            color = Color(0XFF979797),
            style = MaterialTheme.typography.title3
        )
        Text(
            text = formatTime(timeElapsedSec),
            color = MaterialTheme.colors.secondary,
            style = MaterialTheme.typography.display2
        )
        Button(onClick = onStopRecording) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pause),
                contentDescription = "pause button"
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val min = (seconds - (hours * 3600)) / 60
    val sec = seconds - (hours * 3600) - (min * 60)

    return "%02d:%02d".format(min, sec)
}
