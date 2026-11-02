package com.seventyfiveperc.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.R
import com.seventyfiveperc.app.ui.theme.NeonCyan
import com.seventyfiveperc.app.ui.theme.PureBlack
import kotlin.random.Random

@Composable
fun AuraLoadingScreen() {
    val loadingAura = R.drawable.aura_4

    val genZMessages = listOf(
        "Vibing with the attendance data...",
        "Aura check in progress...",
        "No cap, this loading is fast...",
        "Hold up, let us cook...",
        "Getting ready to be main character...",
        "Calculated aura: Infinity...",
        "Loading peak performance fr fr...",
        "Don't let your attendance be mid..."
    )

    val randomMessage = remember { genZMessages.random() }

    val infiniteTransition = rememberInfiniteTransition(label = "aura_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

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
            Box(contentAlignment = Alignment.Center) {
                // Background glow/aura
                Image(
                    painter = painterResource(id = loadingAura),
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .rotate(rotation)
                        .alpha(0.3f),
                    contentScale = ContentScale.Fit
                )
                
                // Primary Aura image
                Image(
                    painter = painterResource(id = loadingAura),
                    contentDescription = "Aura persona",
                    modifier = Modifier
                        .size(180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = randomMessage,
                color = NeonCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}

// Extension for alpha since it wasn't imported directly and often used via Modifier
private fun Modifier.alpha(alpha: Float): Modifier = this.then(
    Modifier.graphicsLayer(alpha = alpha)
)
