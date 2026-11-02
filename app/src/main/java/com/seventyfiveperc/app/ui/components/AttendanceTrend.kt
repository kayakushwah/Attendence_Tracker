package com.seventyfiveperc.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AttendanceTrend(
    trend: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6200EE)
) {
    if (trend.size < 2) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxVal = 100f
        val minVal = 0f
        
        val path = Path().apply {
            trend.forEachIndexed { index, value ->
                val x = index * (width / (trend.size - 1))
                val y = height - ((value.toFloat() - minVal) / (maxVal - minVal) * height)
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
