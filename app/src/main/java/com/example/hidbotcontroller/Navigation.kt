package com.example.hidbotcontroller

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.hidbotcontroller.ui.main.MainScreen
import com.example.hidbotcontroller.ui.main.MainScreenViewModel
import com.example.hidbotcontroller.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() }
    val context = LocalContext.current
    
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val espIp by viewModel.espIp.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isHebrew by viewModel.isHebrew.collectAsStateWithLifecycle()

    if (showUpdateDialog && updateInfo != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = { androidx.compose.material3.Text("עדכון חדש זמין!") },
            text = { 
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text("גרסה: ${updateInfo?.version}")
                    androidx.compose.material3.Text("מה חדש:\n${updateInfo?.changelog}")
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    viewModel.dismissUpdateDialog()
                    viewModel.downloadAndInstallUpdate(context) 
                }) {
                    androidx.compose.material3.Text("עדכן עכשיו")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    androidx.compose.material3.Text("ביטול")
                }
            }
        )
    }

    // Load saved IP on first composition
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("hid_bot_prefs", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("esp_ip", "") ?: ""
        if (savedIp.isNotEmpty()) {
            viewModel.setManualIp(savedIp)
        }
        viewModel.checkForUpdates(context)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    isConnected = isConnected,
                    isRunning = isRunning,
                    espIp = espIp,
                    isHebrew = isHebrew,
                    onHebrewChange = { viewModel.setHebrew(it) },
                    onToggle = { viewModel.toggleBot() },
                    onSettingsClick = { backStack.add(Settings) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Settings> {
                SettingsScreen(
                    onCheckUpdates = { viewModel.checkForUpdates(context) },
                    onCheckEspUpdate = { viewModel.checkEspUpdates(context) },
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
    )
}
