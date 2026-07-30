package com.example.hidbotcontroller.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    isConnected: Boolean,
    isRunning: Boolean,
    espIp: String,
    onToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "מחובר: $espIp" else "מחפש בקר...",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            
            IconButton(onClick = onSettingsClick) {
                Text("⚙", fontSize = 24.sp)
            }
        }

        // Center Content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val buttonColor by animateColorAsState(
                targetValue = if (isRunning) Color(0xFFF44336) else Color(0xFF2196F3),
                label = "buttonColor"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by if (isRunning) {
                infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
            } else {
                remember { mutableFloatStateOf(1.0f) }
            }

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .shadow(if (isRunning) 20.dp else 10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                        enabled = isConnected,
                        onClick = onToggle
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Use unicode symbols instead of Material Icons to avoid import issues
                Text(
                    text = if (isRunning) "⏹" else "▶",
                    fontSize = 64.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            var dotCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(isRunning) {
                if (isRunning) {
                    while (true) {
                        delay(500)
                        dotCount = (dotCount + 1) % 4
                    }
                } else {
                    dotCount = 0
                }
            }

            val dots = ".".repeat(dotCount)

            Text(
                text = when {
                    !isConnected -> "מנותק"
                    isRunning -> "פועל$dots"
                    else -> "לחץ להפעלה"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Text
        Text(
            text = "גרסה 1.3",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    MainScreen(
        isConnected = true,
        isRunning = false,
        espIp = "192.168.1.100",
        onToggle = {},
        onSettingsClick = {}
    )
}
