package com.seventyfiveperc.app.ui.predictor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seventyfiveperc.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictorScreen(viewModel: PredictorViewModel, onNavigateBack: () -> Unit) {
    var weeksRemaining by remember { mutableStateOf(10f) }
    var attendRate by remember { mutableStateOf(75f) }
    val state by viewModel.state.collectAsState()

    // Run prediction whenever sliders OR subjects change
    LaunchedEffect(weeksRemaining, attendRate, state.subjects) {
        if (state.subjects.isNotEmpty()) {
            viewModel.runFullSimulation(weeksRemaining.toInt(), attendRate.toInt(), 75)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "FUTURE ME",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = NeonCyan
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PureBlack)
            )
        },
        containerColor = PureBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Slider 1: How hard u gonna try?
            PredictorSlider(
                label = "How hard u gonna try? 🫡",
                value = attendRate,
                onValueChange = { attendRate = it },
                valueRange = 0f..100f,
                displayValue = "${attendRate.toInt()}%",
                accentColor = HotMagenta
            )

            // Slider 2: Weeks Remaining
            PredictorSlider(
                label = "Weeks Remaining in Sem 🙃",
                value = weeksRemaining,
                onValueChange = { weeksRemaining = it },
                valueRange = 1f..20f,
                displayValue = "${weeksRemaining.toInt()} weeks",
                accentColor = NeonCyan
            )

            Text(
                "OVERALL IMPACT", 
                style = MaterialTheme.typography.labelMedium.copy(color = SecondaryGrey, fontWeight = FontWeight.Bold)
            )

            state.overallPrediction?.let { prediction ->
                PredictionResultView(prediction)
            }

            Text(
                "SUBJECT-WISE PREDICTION", 
                style = MaterialTheme.typography.labelMedium.copy(color = SecondaryGrey, fontWeight = FontWeight.Bold)
            )

            state.subjects.forEach { subject ->
                val prediction = state.subjectPredictions[subject.id]
                SubjectPredictionCard(
                    subject = subject,
                    prediction = prediction
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SubjectPredictionCard(
    subject: com.seventyfiveperc.app.data.models.Subject,
    prediction: PredictionResult?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GlassWhite, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        subject.name.uppercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = OffWhite)
                    )
                    Text(
                        "Code: ${subject.subjectCode.ifEmpty { "N/A" }}",
                        style = MaterialTheme.typography.labelSmall.copy(color = SecondaryGrey)
                    )
                }
                
                prediction?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${it.finalPercentage.toInt()}%",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (it.isSafe) LimeGreen else BloodRed
                            )
                        )
                        if (it.requiredToReachGoal > 0) {
                            Text(
                                "NEED: ${it.requiredToReachGoal}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (it.isSafe) OffWhite else BloodRed
                                )
                            )
                        }
                    }
                }
            }
            
            prediction?.let {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (it.finalPercentage / 100.0).coerceIn(0.0, 1.0).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = if (it.isSafe) LimeGreen else BloodRed,
                    trackColor = PureBlack
                )
                Text(
                    text = it.breakdown,
                    style = MaterialTheme.typography.bodySmall.copy(color = OffWhite, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = if (it.isSafe) "Ur safe fr 🫡" else "Go to class bro 😭",
                    style = MaterialTheme.typography.labelSmall.copy(color = SecondaryGrey),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PredictorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: Color
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Bold))
            Text(displayValue, style = MaterialTheme.typography.bodyLarge.copy(color = accentColor, fontWeight = FontWeight.Black))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = GlassWhite
            )
        )
    }
}

@Composable
fun ScenarioCard(label: String, percentage: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassWhite)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Bold))
        Text(percentage, style = MaterialTheme.typography.bodyLarge.copy(color = color, fontWeight = FontWeight.Black))
    }
}

@Composable
fun PredictionResultView(prediction: PredictionResult) {
    val animatedProgress by animateFloatAsState(
        targetValue = prediction.finalPercentage.toFloat() / 100f,
        animationSpec = tween(1000),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (prediction.isSafe) "FINAL: ${prediction.finalPercentage.toInt()}% \nYOU'LL MAKE IT FREN 🥹"
            else "FINAL: ${prediction.finalPercentage.toInt()}% \nUR COOKED BESTIE 😭",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = if (prediction.isSafe) LimeGreen else BloodRed,
                lineHeight = 32.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(150.dp)) {
                // Background arc
                drawArc(
                    color = GlassWhite,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                // Progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to BloodRed,
                        0.5f to Color.Yellow,
                        1f to LimeGreen
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${prediction.finalPercentage.toInt()}%", 
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = OffWhite)
                )
                Text("PROBABILITY", style = MaterialTheme.typography.labelSmall.copy(color = SecondaryGrey))
            }
        }
    }
}
