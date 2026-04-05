package com.example.voicerecorder.ui.screens.recording

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val barWidth = size.width / 60f
        val maxBarHeight = size.height * 0.9f
        val minBarHeight = size.height * 0.04f
        val centerY = size.height / 2f

        amplitudes.forEachIndexed { index, amp ->
            val clampedAmp = amp.coerceIn(0f, 1f)
            val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * clampedAmp
            val x = index * barWidth + barWidth / 2f

            drawLine(
                color = barColor.copy(alpha = 0.3f + 0.7f * clampedAmp),
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = barWidth * 0.6f,
                cap = StrokeCap.Round
            )
        }
    }
}
