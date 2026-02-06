package com.example.gimnasio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.gimnasio.ui.GymApp
import com.example.gimnasio.ui.theme.GimnasioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GimnasioTheme {
                GymApp()
            }
        }
    }
}

