package com.roomcheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.roomcheck.app.ui.AppNavigation
import com.roomcheck.app.ui.theme.RoomCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoomCheckTheme {
                AppNavigation()
            }
        }
    }
}
