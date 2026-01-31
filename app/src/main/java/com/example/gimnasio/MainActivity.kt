package com.example.gimnasio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.gimnasio.ui.main.MainScreen
import com.example.gimnasio.ui.navigation.GymNavHost
import com.example.gimnasio.ui.theme.GimnasioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GimnasioTheme {
                GymNavHost()
            }
        }

    }
}
