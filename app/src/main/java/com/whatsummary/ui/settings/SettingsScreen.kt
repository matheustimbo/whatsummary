package com.whatsummary.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.whatsummary.R
import com.whatsummary.data.preferences.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check notification service status when returning from system settings
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshState()
        }
    }

    // Handle export: share JSON via intent
    LaunchedEffect(uiState.exportedJson) {
        val json = uiState.exportedJson ?: return@LaunchedEffect
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, json)
            type = "application/json"
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
        viewModel.clearExportedJson()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.onboarding_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification service status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.notificationServiceActive)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.notificationServiceActive)
                            Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (uiState.notificationServiceActive)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_notification_status),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (uiState.notificationServiceActive)
                                stringResource(R.string.settings_notification_active)
                            else stringResource(R.string.settings_notification_inactive),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary time
            SettingsSection(title = stringResource(R.string.settings_summary_time)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.showTimePicker() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = String.format("%02d:%02d", uiState.summaryHour, uiState.summaryMinute),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // API Key
            SettingsSection(title = stringResource(R.string.settings_api_key)) {
                Text(
                    text = if (uiState.hasApiKey) stringResource(R.string.settings_api_key_configured)
                    else stringResource(R.string.settings_api_key_not_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.hasApiKey) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            // Model selection
            SettingsSection(title = stringResource(R.string.settings_model)) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateModel(UserPreferences.MODEL_HAIKU) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.llmModel == UserPreferences.MODEL_HAIKU,
                            onClick = { viewModel.updateModel(UserPreferences.MODEL_HAIKU) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_model_haiku),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateModel(UserPreferences.MODEL_SONNET) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.llmModel == UserPreferences.MODEL_SONNET,
                            onClick = { viewModel.updateModel(UserPreferences.MODEL_SONNET) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_model_sonnet),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Data retention
            SettingsSection(title = stringResource(R.string.settings_retention)) {
                Column {
                    Text(
                        text = stringResource(R.string.settings_retention_days, uiState.retentionDays),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = uiState.retentionDays.toFloat(),
                        onValueChange = { viewModel.updateRetentionDays(it.toInt()) },
                        valueRange = 7f..90f,
                        steps = 11
                    )
                }
            }

            HorizontalDivider()

            // Export data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.exportData() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_export),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_export_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Delete all data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showDeleteConfirmation() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_delete_all),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // About
            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(R.string.settings_version, "1.0.0"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Time picker dialog
    if (uiState.showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.summaryHour,
            initialMinute = uiState.summaryMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { viewModel.dismissTimePicker() },
            title = { Text(stringResource(R.string.settings_summary_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSummaryTime(timePickerState.hour, timePickerState.minute)
                    }
                ) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTimePicker() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirmation() },
            title = { Text(stringResource(R.string.settings_delete_all)) },
            text = { Text(stringResource(R.string.settings_delete_all_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAllData() }
                ) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirmation() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}
