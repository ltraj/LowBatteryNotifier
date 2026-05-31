package com.example.lowbatterynotifier.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lowbatterynotifier.R
import com.example.lowbatterynotifier.analytics.BatteryInsights
import com.example.lowbatterynotifier.analytics.ChargingSessionSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage!!,
                    modifier = Modifier.padding(padding).padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            uiState.insights != null -> {
                InsightsContent(
                    insights = uiState.insights!!,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun InsightsContent(insights: BatteryInsights, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InsightHighlightCard(
            title = stringResource(R.string.insights_shutdown_estimate),
            value = insights.estimatedShutdownText,
            sourceLabel = insights.sourceLabel,
        )

        InsightCard(stringResource(R.string.insights_avg_drain)) {
            Text(
                text = insights.averageDrainPerHourText
                    ?: stringResource(R.string.insights_not_enough_data),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        InsightCard(stringResource(R.string.insights_charging_stats)) {
            MetricRow(
                stringResource(R.string.insights_total_sessions),
                insights.totalChargingSessions.toString(),
            )
            MetricRow(
                stringResource(R.string.insights_avg_charge_duration),
                insights.averageChargingDurationText ?: stringResource(R.string.insights_not_enough_data),
            )
            MetricRow(
                stringResource(R.string.insights_longest_charge),
                insights.longestChargingText ?: stringResource(R.string.insights_not_enough_data),
            )
            MetricRow(
                stringResource(R.string.insights_shortest_charge),
                insights.shortestChargingText ?: stringResource(R.string.insights_not_enough_data),
            )
            MetricRow(
                stringResource(R.string.insights_avg_plug_in),
                insights.averagePlugInPercentText ?: stringResource(R.string.insights_not_enough_data),
            )
            MetricRow(
                stringResource(R.string.insights_avg_unplug),
                insights.averageUnplugPercentText ?: stringResource(R.string.insights_not_enough_data),
            )
        }

        InsightCard(stringResource(R.string.insights_period_summary)) {
            MetricRow(
                stringResource(R.string.insights_last_24h),
                insights.summary24hText ?: stringResource(R.string.insights_not_enough_data),
            )
            MetricRow(
                stringResource(R.string.insights_last_7d),
                insights.summary7dText ?: stringResource(R.string.insights_not_enough_data),
            )
            MetricRow(
                "Overnight drain (11 PM - 7 AM)",
                insights.overnightDrainText ?: stringResource(R.string.insights_not_enough_data),
            )
        }

        if (insights.recentSessions.isNotEmpty()) {
            InsightCard(stringResource(R.string.insights_session_history)) {
                insights.recentSessions.forEach { session ->
                    SessionRow(session)
                }
            }
        }

        Text(
            text = stringResource(R.string.insights_footer_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun InsightHighlightCard(title: String, value: String, sourceLabel: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (sourceLabel != null) {
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SessionRow(session: ChargingSessionSummary) {
    val levelText = if (session.endPercent != null) {
        "${session.startPercent}% → ${session.endPercent}%"
    } else {
        "${session.startPercent}% → …"
    }
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = levelText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(
            text = "${session.durationText} · ${session.whenText}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
