package com.innatedream.scrcpyandroid

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScrcpyManager(private val context: Context) {
    companion object {
        private const val TAG = "ScrcpyManager"
        private const val SCRCPY_SERVER_FILENAME = "scrcpy-server"
    }

    private var scrcpyServerPath: String? = null
    private var scrcpyProcess: Process? = null

    init {
        extractScrcpyServer()
    }

    private fun extractScrcpyServer() {
        val scrcpyServerFile = File(context.filesDir, SCRCPY_SERVER_FILENAME)
        if (!scrcpyServerFile.exists()) {
            try {
                context.assets.open(SCRCPY_SERVER_FILENAME).use { input ->
                    scrcpyServerFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                scrcpyServerFile.setExecutable(true)
                scrcpyServerPath = scrcpyServerFile.absolutePath
                Log.d(TAG, "Scrcpy server extracted to: $scrcpyServerPath")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract Scrcpy server", e)
            }
        } else {
            scrcpyServerPath = scrcpyServerFile.absolutePath
        }
    }

    suspend fun startScrcpy(deviceId: String, options: ScrcpyOptions): Boolean {
        return withContext(Dispatchers.IO) {
            if (scrcpyServerPath == null) {
                Log.e(TAG, "Scrcpy server binary not found.")
                return@withContext false
            }

            try {
                val command = mutableListOf<String>()
                command.add(scrcpyServerPath!!)
                command.add("-s")
                command.add(deviceId)

                options.apply {
                    if (resolution != null) {
                        command.add("-m")
                        command.add(resolution.toString())
                    }
                    if (bitRate != null) {
                        command.add("-b")
                        command.add("${bitRate}M")
                    }
                    if (crop != null) {
                        command.add("--crop")
                        command.add(crop)
                    }
                    if (turnScreenOff) {
                        command.add("--turn-screen-off")
                    }
                    if (stayAwake) {
                        command.add("--stay-awake")
                    }
                    // Add other options as needed
                }

                Log.d(TAG, "Starting Scrcpy with command: ${command.joinToString(" ")}")
                scrcpyProcess = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()

                val reader = scrcpyProcess!!.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    Log.d(TAG, "Scrcpy output: $line")
                }

                val exitCode = scrcpyProcess!!.waitFor()
                Log.d(TAG, "Scrcpy process exited with code: $exitCode")

                exitCode == 0
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Scrcpy", e)
                false
            }
        }
    }

    fun stopScrcpy() {
        scrcpyProcess?.destroy()
        scrcpyProcess = null
        Log.d(TAG, "Scrcpy process stopped.")
    }
}

data class ScrcpyOptions(
    val resolution: Int? = null,
    val bitRate: Int? = null,
    val crop: String? = null,
    val turnScreenOff: Boolean = false,
    val stayAwake: Boolean = false
    // Add other scrcpy options here
)


