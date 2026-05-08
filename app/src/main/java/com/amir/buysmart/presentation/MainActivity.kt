package com.amir.buysmart.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amir.buysmart.presentation.navigation.AppNavGraph
import com.amir.buysmart.presentation.theme.BuySmartTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuySmartTheme {
                AppNavGraph()
            }
        }
    }
}
