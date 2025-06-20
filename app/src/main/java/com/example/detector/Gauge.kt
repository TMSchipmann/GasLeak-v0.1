package com.example.detector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Gauge(
    value: Int,
    modifier: Modifier = Modifier,
    max: Int = 1000,
    label: String = "ppm",
    color: Color = Color.Green  // 👈 Nuevo parámetro con valor por defecto
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val sweepAngle = (value.toFloat() / max) * 270f
            drawArc(
                color = Color.LightGray,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                size = Size(size.width, size.height),
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,  // 👈 Usa el color recibido
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                size = Size(size.width, size.height),
                style = Stroke(width = 30f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontSize = 48.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 16.sp, color = Color.Gray)
        }
    }
}
