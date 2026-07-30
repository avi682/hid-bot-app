package com.example.hidbotcontroller.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // Connection Section
            SettingsCard(title = "חיבור לבקר") {
                OutlinedTextField(
                    value = currentIp,
                    onValueChange = onIpChange,
                    label = { Text("כתובת IP של ה-ESP32", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = TextDimGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = BluePrimary,
                        cursorColor = BluePrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onTestConnection,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BluePrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("בדוק חיבור")
                }
                
                if (connectionTestResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = connectionTestResult,
                        color = if (connectionTestResult == "מחובר!") GreenOnline else RedActive,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            
            // Updates Section
            SettingsCard(title = "עדכונים") {
                Text(
                    text = "הכנס כתובת URL לקובץ version.json כדי לקבל עדכונים",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = updateUrl,
                    onValueChange = onUpdateUrlChange,
                    label = { Text("כתובת עדכון (URL)", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = TextDimGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedLabelColor = BluePrimary,
                        cursorColor = BluePrimary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
            SettingsCard(title = "מידע") {
                Text(
                    text = "ESP32-C3 HID Bot Controller",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "גרסה: 1.0.0",
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
