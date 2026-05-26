package com.shitbox.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shitbox.monitor.ui.Dashboard
import com.shitbox.monitor.ui.theme.ShitboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShitboxTheme { Dashboard() }
        }
    }
}
