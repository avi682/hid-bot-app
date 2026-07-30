package com.example.hidbotcontroller.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

data class EspStatus(val armed: Boolean, val running: Boolean, val wifi: Boolean, val ip: String)

class EspClient {
    private val timeout = 3000

    suspend fun ping(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/ping")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStatus(ip: String): EspStatus? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/status")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val json = JSONObject(response.toString())
                val status = EspStatus(
                    armed = json.optBoolean("armed", false),
                    running = json.optBoolean("running", false),
                    wifi = json.optBoolean("wifi", false),
                    ip = json.optString("ip", "")
                )
                connection.disconnect()
                status
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun start(ip: String, isHebrew: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/start?hebrew=$isHebrew")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "POST"
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun stop(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/stop")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.requestMethod = "POST"
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
}
