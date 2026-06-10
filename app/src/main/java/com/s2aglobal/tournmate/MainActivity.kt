package com.s2aglobal.tournmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.s2aglobal.tournmate.ui.navigation.TournMateNavHost
import com.s2aglobal.tournmate.ui.theme.TournMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TournMateTheme {
                TournMateNavHost()
            }
        }
    }
}
