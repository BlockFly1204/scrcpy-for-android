package com.innatedream.scrcpyandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.innatedream.scrcpyandroid.ui.theme.ScrcpyAndroidTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var adbManager: AdbManager
    private lateinit var scrcpyManager: ScrcpyManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        adbManager = AdbManager(this)
        scrcpyManager = ScrcpyManager(this)
        
        setContent {
            ScrcpyAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScrcpyMainScreen(
                        adbManager = adbManager,
                        scrcpyManager = scrcpyManager,
                        onNavigateToFileTransfer = {
                            startActivity(Intent(this@MainActivity, FileTransferActivity::class.java))
                        },
                        onNavigateToTerminal = {
                            startActivity(Intent(this@MainActivity, TerminalActivity::class.java))
                        },
                        onNavigateToFloatingControls = {
                            startActivity(Intent(this@MainActivity, FloatingControlsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrcpyMainScreen(
    adbManager: AdbManager? = null,
    scrcpyManager: ScrcpyManager? = null,
    onNavigateToFileTransfer: () -> Unit = {},
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToFloatingControls: () -> Unit = {}
) {
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }
    var selectedResolution by remember { mutableStateOf("原始分辨率") }
    var selectedBitrate by remember { mutableStateOf("8M") }
    var selectedAspectRatio by remember { mutableStateOf("保持原始比例") }
    var turnScreenOff by remember { mutableStateOf(false) }
    var keepAwake by remember { mutableStateOf(false) }
    var enableFileTransfer by remember { mutableStateOf(false) }
    var enableTerminal by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("未连接") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val resolutionOptions = listOf("原始分辨率", "自动适配", "1920x1080", "1280x720", "854x480")
    val bitrateOptions = listOf("2M", "4M", "8M", "16M", "32M")
    val aspectRatioOptions = listOf("保持原始比例", "拉伸屏幕")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "scrcpy for Android",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        // Connection Status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "连接状态: $connectionStatus",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // IP Address and Port
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "连接设置",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP地址") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isConnecting
                )
                
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口") },
                    placeholder = { Text("5555") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isConnecting
                )
            }
        }
        
        // Video Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "视频设置",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Resolution Dropdown
                var resolutionExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = resolutionExpanded,
                    onExpandedChange = { resolutionExpanded = !resolutionExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedResolution,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分辨率") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isConnecting
                    )
                    ExposedDropdownMenu(
                        expanded = resolutionExpanded,
                        onDismissRequest = { resolutionExpanded = false }
                    ) {
                        resolutionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedResolution = option
                                    resolutionExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Bitrate Dropdown
                var bitrateExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = bitrateExpanded,
                    onExpandedChange = { bitrateExpanded = !bitrateExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBitrate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("码率") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bitrateExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isConnecting
                    )
                    ExposedDropdownMenu(
                        expanded = bitrateExpanded,
                        onDismissRequest = { bitrateExpanded = false }
                    ) {
                        bitrateOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedBitrate = option
                                    bitrateExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Aspect Ratio Dropdown
                var aspectRatioExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = aspectRatioExpanded,
                    onExpandedChange = { aspectRatioExpanded = !aspectRatioExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAspectRatio,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("画面比例") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aspectRatioExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isConnecting
                    )
                    ExposedDropdownMenu(
                        expanded = aspectRatioExpanded,
                        onDismissRequest = { aspectRatioExpanded = false }
                    ) {
                        aspectRatioOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedAspectRatio = option
                                    aspectRatioExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Control Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "控制设置",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("熄屏控制")
                    Switch(
                        checked = turnScreenOff,
                        onCheckedChange = { turnScreenOff = it },
                        enabled = !isConnecting
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("后台保活")
                    Switch(
                        checked = keepAwake,
                        onCheckedChange = { keepAwake = it },
                        enabled = !isConnecting
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("文件传输")
                    Switch(
                        checked = enableFileTransfer,
                        onCheckedChange = { enableFileTransfer = it },
                        enabled = !isConnecting
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("终端功能")
                    Switch(
                        checked = enableTerminal,
                        onCheckedChange = { enableTerminal = it },
                        enabled = !isConnecting
                    )
                }
            }
        }
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Connect Button
            Button(
                onClick = {
                    if (adbManager != null && scrcpyManager != null) {
                        isConnecting = true
                        connectionStatus = "连接中..."
                        
                        coroutineScope.launch {
                            try {
                                val connected = adbManager.connectToDevice(ipAddress, port.toIntOrNull() ?: 5555)
                                if (connected) {
                                    connectionStatus = "已连接"
                                    
                                    val options = ScrcpyOptions(
                                        resolution = if (selectedResolution == "原始分辨率") null else parseResolution(selectedResolution),
                                        bitRate = parseBitrate(selectedBitrate),
                                        turnScreenOff = turnScreenOff,
                                        stayAwake = keepAwake
                                    )
                                    
                                    val deviceId = "$ipAddress:${port.toIntOrNull() ?: 5555}"
                                    scrcpyManager.startScrcpy(deviceId, options)
                                    
                                    onNavigateToFloatingControls()
                                } else {
                                    connectionStatus = "连接失败"
                                }
                            } catch (e: Exception) {
                                connectionStatus = "连接错误: ${e.message}"
                            } finally {
                                isConnecting = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = ipAddress.isNotBlank() && !isConnecting
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "开始连接",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // Feature Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToFileTransfer,
                modifier = Modifier.weight(1f),
                enabled = enableFileTransfer
            ) {
                Text("文件传输")
            }
            
            OutlinedButton(
                onClick = onNavigateToTerminal,
                modifier = Modifier.weight(1f),
                enabled = enableTerminal
            ) {
                Text("终端")
            }
        }
    }
}

private fun parseResolution(resolution: String): Int? {
    return when (resolution) {
        "1920x1080" -> 1080
        "1280x720" -> 720
        "854x480" -> 480
        else -> null
    }
}

private fun parseBitrate(bitrate: String): Int {
    return bitrate.replace("M", "").toIntOrNull() ?: 8
}

@Preview(showBackground = true)
@Composable
fun ScrcpyMainScreenPreview() {
    ScrcpyAndroidTheme {
        ScrcpyMainScreen()
    }
}

