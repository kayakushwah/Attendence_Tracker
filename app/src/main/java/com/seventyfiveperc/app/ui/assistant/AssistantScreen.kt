package com.seventyfiveperc.app.ui.assistant

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.seventyfiveperc.app.R
import com.seventyfiveperc.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(viewModel: AssistantViewModel, onNavigateBack: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isTyping by viewModel.isTyping.collectAsState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AURA", style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, letterSpacing = 2.sp))
                        Text("UR ATTENDANCE GENIE 🤖", style = MaterialTheme.typography.titleMedium.copy(color = OffWhite, fontWeight = FontWeight.Bold))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PureBlack)
            )
        },
        containerColor = PureBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }

                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip("AM I COOKED? 💀", BloodRed) { viewModel.sendMessage("Am I cooked?") }
                QuickActionChip("WHAT'S NEXT? ⏰", NeonCyan) { viewModel.sendMessage("What's my next class?") }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassWhite)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask me literally anything...", color = SecondaryGrey) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = NeonCyan,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    )
                )
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(NeonCyan, HotMagenta)),
                            shape = CircleShape
                        )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = PureBlack, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .background(NeonCyan.copy(alpha = 0.1f))
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.aura_4),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Aura is thinking...",
            style = MaterialTheme.typography.labelSmall.copy(color = OffWhite, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun QuickActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall.copy(
                color = color, 
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) HotMagenta.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.1f)
    val borderColor = if (isUser) HotMagenta.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.4f)
    
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)) {
                Text("AURA ⚡", style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Black))
                Spacer(modifier = Modifier.width(8.dp))
                Text("JUST NOW", style = MaterialTheme.typography.labelSmall.copy(color = SecondaryGrey, fontSize = 8.sp))
            }
        }
        
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubbleColor)
                .border(1.dp, borderColor, shape)
                .padding(16.dp)
        ) {
            Column {
                if (message.imageRes != null) {
                    Image(
                        painter = painterResource(id = message.imageRes),
                        contentDescription = "Aura reaction",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(bottom = 8.dp)
                    )
                }
                Text(
                    message.text, 
                    style = MaterialTheme.typography.bodyMedium.copy(color = OffWhite, lineHeight = 22.sp)
                )
            }
        }
    }
}
