package com.innatedream.scrcpyandroid

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket

class AdbManager(private val context: Context) {
    companion object {
        private const val TAG = "AdbManager"
        private const val ADB_PORT = 5555
    }

    private var adbPath: String? = null

    init {
        setupAdb()
    }

    private fun setupAdb() {
        // Determine device architecture
        val abi = android.os.Build.SUPPORTED_ABIS[0]
        val adbFileName = when {
            abi.contains("arm64") -> "arm64-v8a/adb"
            abi.contains("arm") -> "armeabi-v7a/adb"
            else -> "arm64-v8a/adb" // Default fallback
        }
        
        // Extract ADB binary from assets to internal storage
        val adbFile = File(context.filesDir, "adb")
        if (!adbFile.exists()) {
            try {
                context.assets.open(adbFileName).use { input ->
                    adbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                adbFile.setExecutable(true)
                adbPath = adbFile.absolutePath
                Log.d(TAG, "ADB binary extracted to: $adbPath (architecture: $abi)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract ADB binary", e)
            }
        } else {
            adbPath = adbFile.absolutePath
        }
    }

    suspend fun connectToDevice(ipAddress: String, port: Int = ADB_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // First check if device is reachable
                if (!isDeviceReachable(ipAddress, port)) {
                    Log.e(TAG, "Device not reachable at $ipAddress:$port")
                    return@withContext false
                }

                // Execute ADB connect command
                val command = "$adbPath connect $ipAddress:$port"
                val process = Runtime.getRuntime().exec(command)
                val result = process.waitFor()
                
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                
                Log.d(TAG, "ADB connect output: $output")
                if (error.isNotEmpty()) {
                    Log.e(TAG, "ADB connect error: $error")
                }
                
                result == 0 && output.contains("connected")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to device", e)
                false
            }
        }
    }

    suspend fun disconnectFromDevice(ipAddress: String, port: Int = ADB_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val command = "$adbPath disconnect $ipAddress:$port"
                val process = Runtime.getRuntime().exec(command)
                val result = process.waitFor()
                
                val output = process.inputStream.bufferedReader().readText()
                Log.d(TAG, "ADB disconnect output: $output")
                
                result == 0
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect from device", e)
                false
            }
        }
    }

    suspend fun getConnectedDevices(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val command = "$adbPath devices"
                val process = Runtime.getRuntime().exec(command)
                process.waitFor()
                
                val output = process.inputStream.bufferedReader().readText()
                Log.d(TAG, "ADB devices output: $output")
                
                val devices = mutableListOf<String>()
                output.lines().forEach { line ->
                    if (line.contains("\tdevice") && !line.startsWith("List of devices")) {
                        val deviceId = line.split("\t")[0]
                        devices.add(deviceId)
                    }
                }
                devices
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get connected devices", e)
                emptyList()
            }
        }
    }

    suspend fun checkRootAccess(deviceId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val command = "$adbPath -s $deviceId shell su -c 'id'"
                val process = Runtime.getRuntime().exec(command)
                val result = process.waitFor()
                
                val output = process.inputStream.bufferedReader().readText()
                Log.d(TAG, "Root check output: $output")
                
                result == 0 && output.contains("uid=0")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check root access", e)
                false
            }
        }
    }

    suspend fun executeShellCommand(deviceId: String, command: String, useRoot: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            try {
                val fullCommand = if (useRoot) {
                    "$adbPath -s $deviceId shell su -c '$command'"
                } else {
                    "$adbPath -s $deviceId shell $command"
                }
                
                val process = Runtime.getRuntime().exec(fullCommand)
                process.waitFor()
                
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                
                if (error.isNotEmpty()) {
                    Log.e(TAG, "Shell command error: $error")
                }
                
                output
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute shell command", e)
                ""
            }
        }
    }

    private fun isDeviceReachable(ipAddress: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ipAddress, port), 5000)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

