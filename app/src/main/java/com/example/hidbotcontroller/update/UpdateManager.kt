package com.example.hidbotcontroller.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val versionCode: Int, val apkUrl: String, val changelog: String)

class UpdateManager {
    suspend fun checkForUpdate(context: Context, updateUrl: String): UpdateInfo? = withContext(Dispatchers.IO) {
        if (updateUrl.isEmpty()) return@withContext null
        
        try {
            val url = URL(updateUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                connection.disconnect()
                
                val json = JSONObject(response.toString())
                val version = json.optString("version", "")
                val versionCode = json.optInt("versionCode", 0)
                val apkUrl = json.optString("apkUrl", "")
                val changelog = json.optString("changelog", "")
                
                val currentVersionCode = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pInfo.versionCode
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    0
                }
                
                if (versionCode > currentVersionCode) {
                    return@withContext UpdateInfo(version, versionCode, apkUrl, changelog)
                }
            } else {
                connection.disconnect()
            }
        } catch (e: Exception) {
            // Ignore
        }
        return@withContext null
    }

    suspend fun downloadAndInstallApk(context: Context, apkUrl: String, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        if (apkUrl.isEmpty()) return@withContext
        
        try {
            val url = URL(apkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.requestMethod = "GET"
            
            val fileLength = connection.contentLength
            
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && !downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val apkFile = File(downloadDir, "update.apk")
            
            val input = connection.inputStream
            val output = FileOutputStream(apkFile)
            
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            
            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress((total * 100 / fileLength).toInt())
                }
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
            connection.disconnect()
            
            // Install
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    apkFile
                )
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.startActivity(intent)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
