package com.innatedream.scrcpyandroid

import android.app.Activity
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.innatedream.scrcpyandroid.ui.theme.ScrcpyAndroidTheme

class FloatingControlsActivity : ComponentActivity() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = true
    
    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        } else {
            createFloatingControls()
        }
        
        setContent {
            ScrcpyAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FloatingControlsScreen()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                createFloatingControls()
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能使用此功能", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun createFloatingControls() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = 0
        layoutParams.y = 100
        
        floatingView = createFloatingControlsView()
        windowManager?.addView(floatingView, layoutParams)
    }

    private fun createFloatingControlsView(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0x80000000.toInt())
            setPadding(8, 8, 8, 8)
        }
        
        // Toggle button (expand/collapse)
        val toggleButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT.hashCode())
            setOnClickListener {
                toggleControls()
            }
        }
        layout.addView(toggleButton)
        
        // Control buttons (initially visible)
        val controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            id = View.generateViewId()
        }
        
        // Navigation buttons
        val backButton = createControlButton(android.R.drawable.ic_media_rew) {
            // TODO: Send back key event
        }
        val homeButton = createControlButton(android.R.drawable.ic_menu_home) {
            // TODO: Send home key event
        }
        val recentButton = createControlButton(android.R.drawable.ic_menu_recent_history) {
            // TODO: Send recent apps key event
        }
        
        // Volume buttons
        val volumeUpButton = createControlButton(android.R.drawable.ic_media_ff) {
            // TODO: Send volume up key event
        }
        val volumeDownButton = createControlButton(android.R.drawable.ic_media_rew) {
            // TODO: Send volume down key event
        }
        val powerButton = createControlButton(android.R.drawable.ic_lock_power_off) {
            // TODO: Send power key event
        }
        
        // Audio toggle and exit buttons
        val audioToggleButton = createControlButton(android.R.drawable.ic_media_play) {
            // TODO: Toggle remote audio
        }
        val exitButton = createControlButton(android.R.drawable.ic_menu_close_clear_cancel) {
            finish()
        }
        
        controlsLayout.addView(backButton)
        controlsLayout.addView(homeButton)
        controlsLayout.addView(recentButton)
        controlsLayout.addView(volumeUpButton)
        controlsLayout.addView(volumeDownButton)
        controlsLayout.addView(powerButton)
        controlsLayout.addView(audioToggleButton)
        controlsLayout.addView(exitButton)
        
        layout.addView(controlsLayout)
        
        // Make the floating view draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        
        layout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val layoutParams = floatingView?.layoutParams as WindowManager.LayoutParams
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val layoutParams = floatingView?.layoutParams as WindowManager.LayoutParams
                    layoutParams.x = initialX + (initialTouchX - event.rawX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, layoutParams)
                    true
                }
                else -> false
            }
        }
        
        return layout
    }

    private fun createControlButton(iconRes: Int, onClick: () -> Unit): ImageButton {
        return ImageButton(this).apply {
            setImageResource(iconRes)
            setBackgroundColor(0x40FFFFFF)
            setPadding(16, 16, 16, 16)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 4, 4, 4)
            }
        }
    }

    private fun toggleControls() {
        val controlsLayout = floatingView?.findViewById<LinearLayout>(View.generateViewId())
        if (isExpanded) {
            controlsLayout?.visibility = View.GONE
            isExpanded = false
        } else {
            controlsLayout?.visibility = View.VISIBLE
            isExpanded = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager?.removeView(it) }
    }
}

@Composable
fun FloatingControlsScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "悬浮控制已启动",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "悬浮控制栏已在屏幕右上角显示",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                (context as Activity).finish()
            }
        ) {
            Text("返回主界面")
        }
    }
}

@Composable
fun FloatingControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

