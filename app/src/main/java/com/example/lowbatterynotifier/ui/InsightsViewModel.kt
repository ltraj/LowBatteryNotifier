package com.example.lowbatterynotifier.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lowbatterynotifier.R
import com.example.lowbatterynotifier.analytics.BatteryAnalyticsRepository
import com.example.lowbatterynotifier.analytics.BatteryAnalyticsTracker
import com.example.lowbatterynotifier.analytics.InsightsCalculator
import com.example.lowbatterynotifier.analytics.BatteryInsights
import com.example.lowbatterynotifier.battery.readBatteryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.concurrent.TimeUnit

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: BatteryInsights? = null,
    val errorMessage: String? = null,
)

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BatteryAnalyticsRepository(application)
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private val dateTimeFormat: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = InsightsUiState(isLoading = true)
            try {
                val app = getApplication<Application>()
                val battery = app.readBatteryInfo()
                BatteryAnalyticsTracker.getInstance(app).processBatteryInfo(
                    battery,
                    System.currentTimeMillis(),
                )

                val data = repository.loadInsightsData()
                val notEnough = app.getString(R.string.insights_not_enough_data)
                val insights = InsightsCalculator.compute(
                    currentBatteryPercent = data.currentBatteryPercent,
                    drainSamples = data.drainSamples,
                    chargingSessions = data.chargingSessions,
                    formatDuration = ::formatDuration,
                    formatDateTime = { dateTimeFormat.format(it) },
                    notEnoughData = notEnough,
                )
                _uiState.value = InsightsUiState(isLoading = false, insights = insights)
            } catch (e: Exception) {
                _uiState.value = InsightsUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Unable to load insights",
                )
            }
        }
    }

    private fun formatDuration(durationMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        return when {
            minutes < 1 -> "< 1 min"
            minutes < 60 -> "$minutes min"
            else -> {
                val hours = minutes / 60
                val rem = minutes % 60
                if (rem == 0L) "$hours h" else "$hours h $rem min"
            }
        }
    }
}
