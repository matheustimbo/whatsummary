package com.whatsummary.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsummary.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    val color = if (index <= uiState.currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .then(
                                Modifier
                                    .height(4.dp)
                            )
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = color,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = uiState.currentStep,
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep()
                    1 -> NotificationStep(
                        granted = uiState.notificationPermissionGranted,
                        onRequestPermission = {
                            viewModel.checkNotificationPermission()
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        },
                        onCheckPermission = { viewModel.checkNotificationPermission() }
                    )
                    2 -> ApiKeyStep(
                        apiKey = uiState.apiKey,
                        apiKeyValid = uiState.apiKeyValid,
                        testing = uiState.apiKeyTesting,
                        onApiKeyChanged = viewModel::onApiKeyChanged,
                        onTestKey = viewModel::testApiKey
                    )
                    3 -> ScheduleStep(
                        hour = uiState.summaryHour,
                        minute = uiState.summaryMinute,
                        onTimeChanged = viewModel::onTimeChanged
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.currentStep > 0) {
                    OutlinedButton(onClick = { viewModel.previousStep() }) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (uiState.currentStep < 3) {
                    Button(
                        onClick = { viewModel.nextStep() },
                        enabled = when (uiState.currentStep) {
                            1 -> uiState.notificationPermissionGranted
                            2 -> uiState.apiKeyValid == true
                            else -> true
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    ) {
                        Text(stringResource(R.string.onboarding_finish))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationStep(
    granted: Boolean,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            onCheckPermission()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_notification_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_notification_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_notification_granted),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.onboarding_notification_button))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onCheckPermission) {
                Text("Verificar novamente")
            }
        }
    }
}

@Composable
private fun ApiKeyStep(
    apiKey: String,
    apiKeyValid: Boolean?,
    testing: Boolean,
    onApiKeyChanged: (String) -> Unit,
    onTestKey: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_apikey_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_apikey_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChanged,
            label = { Text(stringResource(R.string.onboarding_apikey_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = {
                when (apiKeyValid) {
                    true -> Text(
                        stringResource(R.string.onboarding_apikey_valid),
                        color = MaterialTheme.colorScheme.primary
                    )
                    false -> Text(
                        stringResource(R.string.onboarding_apikey_invalid),
                        color = MaterialTheme.colorScheme.error
                    )
                    null -> {}
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTestKey,
            enabled = apiKey.isNotBlank() && !testing
        ) {
            if (testing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(stringResource(R.string.onboarding_apikey_test))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleStep(
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        onTimeChanged(timePickerState.hour, timePickerState.minute)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_schedule_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_schedule_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        TimePicker(state = timePickerState)
    }
}
