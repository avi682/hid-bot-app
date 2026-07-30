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
                    onToggle = { viewModel.toggleBot() },
                    onSettingsClick = { backStack.add(Settings) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<Settings> {
                var currentIp by remember { mutableStateOf(espIp) }
                val prefs = context.getSharedPreferences("hid_bot_prefs", Context.MODE_PRIVATE)
                var updateUrl by remember { mutableStateOf(prefs.getString("update_url", "") ?: "") }
                var connectionTestResult by remember { mutableStateOf<String?>(null) }
                
                SettingsScreen(
                    currentIp = currentIp,
                    updateUrl = updateUrl,
                    onIpChange = { newIp ->
                        currentIp = newIp
                        prefs.edit().putString("esp_ip", newIp).apply()
                        viewModel.setManualIp(newIp)
                    },
                    onUpdateUrlChange = { newUrl ->
                        updateUrl = newUrl
                        prefs.edit().putString("update_url", newUrl).apply()
                    },
                    onTestConnection = {
                        viewModel.testConnection { success ->
                            connectionTestResult = if (success) "מחובר!" else "לא מצליח להתחבר"
                        }
                    },
                    onCheckUpdates = { viewModel.checkForUpdates(context) },
                    connectionTestResult = connectionTestResult,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
    )
}
