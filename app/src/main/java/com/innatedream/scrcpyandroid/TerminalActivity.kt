package com.innatedream.scrcpyandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.innatedream.scrcpyandroid.ui.theme.ScrcpyAndroidTheme
import kotlinx.coroutines.launch

class TerminalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrcpyAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TerminalScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen() {
    var commandInput by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf(listOf<TerminalLine>()) }
    var isConnected by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new output is added
    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(terminalOutput.size - 1)
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("终端") },
            actions = {
                IconButton(onClick = { 
                    // TODO: Clear terminal
                    terminalOutput = emptyList()
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "清空")
                }
                IconButton(onClick = { /* TODO: Settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            }
        )
        
        // Connection Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("连接状态")
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isConnected) "已连接" else "未连接")
                }
            }
        }
        
        // Terminal Output
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .padding(8.dp),
            state = listState
        ) {
            items(terminalOutput) { line ->
                Text(
                    text = line.text,
                    color = when (line.type) {
                        TerminalLineType.COMMAND -> Color.Green
                        TerminalLineType.OUTPUT -> Color.White
                        TerminalLineType.ERROR -> Color.Red
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Command Input
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    label = { Text("输入命令") },
                    placeholder = { Text("adb shell ls") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commandInput.isNotBlank()) {
                                executeCommand(commandInput) { output, isError ->
                                    terminalOutput = terminalOutput + TerminalLine(
                                        "$ $commandInput",
                                        TerminalLineType.COMMAND
                                    ) + TerminalLine(
                                        output,
                                        if (isError) TerminalLineType.ERROR else TerminalLineType.OUTPUT
                                    )
                                }
                                commandInput = ""
                            }
                        }
                    ),
                    enabled = isConnected
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            executeCommand(commandInput) { output, isError ->
                                terminalOutput = terminalOutput + TerminalLine(
                                    "$ $commandInput",
                                    TerminalLineType.COMMAND
                                ) + TerminalLine(
                                    output,
                                    if (isError) TerminalLineType.ERROR else TerminalLineType.OUTPUT
                                )
                            }
                            commandInput = ""
                        }
                    },
                    enabled = isConnected && commandInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送")
                }
            }
        }
        
        // Quick Commands
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "快捷命令",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickCommandButton(
                        text = "ls",
                        onClick = { commandInput = "adb shell ls" }
                    )
                    QuickCommandButton(
                        text = "ps",
                        onClick = { commandInput = "adb shell ps" }
                    )
                    QuickCommandButton(
                        text = "top",
                        onClick = { commandInput = "adb shell top" }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickCommandButton(
                        text = "logcat",
                        onClick = { commandInput = "adb logcat" }
                    )
                    QuickCommandButton(
                        text = "devices",
                        onClick = { commandInput = "adb devices" }
                    )
                    QuickCommandButton(
                        text = "reboot",
                        onClick = { commandInput = "adb reboot" }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCommandButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

data class TerminalLine(
    val text: String,
    val type: TerminalLineType
)

enum class TerminalLineType {
    COMMAND,
    OUTPUT,
    ERROR
}

fun executeCommand(command: String, onResult: (String, Boolean) -> Unit) {
    // TODO: Implement actual command execution
    // This is a placeholder implementation
    when {
        command.startsWith("adb devices") -> {
            onResult("List of devices attached\n192.168.1.100:5555\tdevice", false)
        }
        command.startsWith("adb shell ls") -> {
            onResult("bin\ndata\ndev\netc\nproc\nroot\nsbin\nsys\nsystem\nvendor", false)
        }
        command.startsWith("adb shell ps") -> {
            onResult("USER     PID   PPID  VSIZE  RSS     WCHAN    PC        NAME\nroot      1     0     1234   567     c0123456 00000000 S /init", false)
        }
        else -> {
            onResult("Command not implemented yet", true)
        }
    }
}

