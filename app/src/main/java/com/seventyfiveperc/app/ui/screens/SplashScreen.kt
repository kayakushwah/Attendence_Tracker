package com.seventyfiveperc.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.ui.theme.*
import kotlinx.coroutines.delay

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.seventyfiveperc.app.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = startAnimation, label = "splash")

    val logoScale by transition.animateFloat(
        transitionSpec = { tween(1000, easing = FastOutSlowInEasing) },
        label = "logoScale"
    ) { if (it) 1f else 0.5f }

    val logoAlpha by transition.animateFloat(
        transitionSpec = { tween(1000) },
        label = "logoAlpha"
    ) { if (it) 1f else 0f }

    val textAlpha by transition.animateFloat(
        transitionSpec = { tween(1000, delayMillis = 1000) },
        label = "textAlpha"
    ) { if (it) 1f else 0f }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.aura_4),
                contentDescription = "Attend.ai Logo",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(
                        scaleX = logoScale,
                        scaleY = logoScale,
                        alpha = logoAlpha
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Attend.ai",
                color = NeonCyan,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer(alpha = textAlpha)
            )
        }
    }
}
