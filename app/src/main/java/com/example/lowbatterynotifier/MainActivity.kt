package com.example.lowbatterynotifier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lowbatterynotifier.notification.NotificationHelper
import com.example.lowbatterynotifier.ui.InsightsScreen
import com.example.lowbatterynotifier.ui.MainScreen
import com.example.lowbatterynotifier.ui.MainViewModel
import com.example.lowbatterynotifier.ui.SettingsScreen
import com.example.lowbatterynotifier.ui.theme.LowBatteryNotifierTheme

class MainActivity : ComponentActivity() {

    private enum class AppDestination { Home, Insights, Settings }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)
        enableEdgeToEdge()
        setContent {
            LowBatteryNotifierTheme {
                val viewModel: MainViewModel = viewModel()
                var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }

                when (destination) {
                    AppDestination.Home -> MainScreen(
                        viewModel = viewModel,
                        onOpenInsights = { destination = AppDestination.Insights },
                    ) { destination = AppDestination.Settings }
                    AppDestination.Insights -> InsightsScreen(
                        onBack = { destination = AppDestination.Home },
                    )
                    AppDestination.Settings -> SettingsScreen(
                        onBack = { destination = AppDestination.Home },
                    )
                }
            }
        }
    }
}
