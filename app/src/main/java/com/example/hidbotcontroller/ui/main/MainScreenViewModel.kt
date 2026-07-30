package com.example.hidbotcontroller.ui.main

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hidbotcontroller.network.EspClient
import com.example.hidbotcontroller.network.EspDiscovery
import com.example.hidbotcontroller.update.UpdateInfo
import com.example.hidbotcontroller.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _espIp = MutableStateFlow("")
    val espIp: StateFlow<String> = _espIp.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val espClient = EspClient()
    private val discovery = EspDiscovery()
    private val updateManager = UpdateManager()
    private var statusPollingJob: Job? = null
    private var discoveryStarted = false

    init {
        startDiscovery()
    }

    fun startDiscovery() {
        if (discoveryStarted) return
        discoveryStarted = true
        discovery.startDiscovery { ip ->
            viewModelScope.launch {
                _espIp.value = ip
                val pingSuccess = espClient.ping(ip)
                if (pingSuccess) {
                    _isConnected.value = true
                    startStatusPolling()
                }
            }
        }
    }

    /**
     * Set IP manually (from settings or saved preferences)
     */
    fun setManualIp(ip: String) {
        _espIp.value = ip
        viewModelScope.launch {
            val pingSuccess = espClient.ping(ip)
            if (pingSuccess) {
                _isConnected.value = true
                startStatusPolling()
            }
        }
    }

    /**
     * Test connection to the current IP and report result via callback
     */
    fun testConnection(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ip = _espIp.value
            if (ip.isEmpty()) {
                onResult(false)
                return@launch
            }
            val success = espClient.ping(ip)
            _isConnected.value = success
            if (success) {
                startStatusPolling()
            }
            onResult(success)
        }
    }

    fun toggleBot() {
        viewModelScope.launch {
            val ip = _espIp.value
            if (ip.isEmpty()) return@launch

            if (_isRunning.value) {
                val success = espClient.stop(ip)
                if (success) {
                    _isRunning.value = false
                }
            } else {
                val success = espClient.start(ip)
                if (success) {
                    _isRunning.value = true
                }
            }
        }
    }

    private fun startStatusPolling() {
        statusPollingJob?.cancel()
        statusPollingJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                val ip = _espIp.value
                if (ip.isNotEmpty()) {
                    val status = espClient.getStatus(ip)
                    if (status != null) {
                        _isConnected.value = true
                        _isRunning.value = status.running
                    } else {
                        _isConnected.value = false
                        discoveryStarted = false
                        startDiscovery()
                        break
                    }
                }
            }
        }
    }

    fun checkForUpdates(context: Context) {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("hid_bot_prefs", Context.MODE_PRIVATE)
            val defaultUrl = "https://raw.githubusercontent.com/avi682/hid-bot-app/main/version.json"
            val updateUrl = prefs.getString("update_url", defaultUrl) ?: defaultUrl
            val info = updateManager.checkForUpdate(context, updateUrl)
            if (info != null) {
                _updateInfo.value = info
                _showUpdateDialog.value = true
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "האפליקציה בגרסה המעודכנת ביותר", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        discovery.stopDiscovery()
        statusPollingJob?.cancel()
    }
}
