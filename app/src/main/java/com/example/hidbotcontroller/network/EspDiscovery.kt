package com.example.hidbotcontroller.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class EspDiscovery {
    @Volatile
    var isSearching: Boolean = false
        private set
        
    private var searchJob: Job? = null
    private var socket: DatagramSocket? = null

    fun startDiscovery(onFound: (String) -> Unit) {
        if (isSearching) return
        isSearching = true
        
        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(4210))
                }
                
                val buffer = ByteArray(1024)
                while (isSearching) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    
                    if (message.startsWith("ESP32BOT:")) {
                        val ip = message.removePrefix("ESP32BOT:")
                        onFound(ip)
                    }
                }
            } catch (e: Exception) {
                // Ignore socket closed exceptions
            } finally {
                socket?.close()
                socket = null
                isSearching = false
            }
        }
    }

    fun stopDiscovery() {
        isSearching = false
        socket?.close()
        searchJob?.cancel()
    }
}
