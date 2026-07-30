package com.example.hidbotcontroller.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hidbotcontroller.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentIp: String,
    updateUrl: String,
    onIpChange: (String) -> Unit,
    onUpdateUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onCheckUpdates: () -> Unit,
    connectionTestResult: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("הגדרות", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Updates Section
            SettingsCard(title = "עדכונים") {
                Button(
                    onClick = onCheckUpdates,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("בדוק עדכונים")
                }
            }
            
            // Info Section
            SettingsCard(title = "אודות") {
                Text(
                    text = "ESP32-C3 HID Bot Controller",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "גרסה: 1.3",
                    color = TextGray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = BluePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}
