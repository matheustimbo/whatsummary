package com.whatsummary.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whatsummary.R
import com.whatsummary.data.db.entity.CapturedMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMessagesScreen(
    onBack: () -> Unit,
    viewModel: GroupMessagesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val justQueued by viewModel.justQueuedSummary.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(justQueued) {
        if (justQueued) {
            snackbarHostState.showSnackbar(
                message = "Resumo enfileirado. Você receberá uma notificação quando estiver pronto."
            )
            viewModel.consumeQueuedFlag()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.groupName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.messages_total_count, uiState.messages.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onboarding_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.messages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.summarizeNow() },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    text = { Text(stringResource(R.string.messages_summarize_now)) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.messages.isEmpty() -> EmptyState(modifier = Modifier.padding(padding))
            else -> MessageList(
                messages = uiState.messages,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.messages_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

@Composable
private fun MessageList(
    messages: List<CapturedMessage>,
    modifier: Modifier = Modifier
) {
    // Group messages by day (using formatted date string as the key).
    val grouped = remember(messages) {
        messages.groupBy { dateFormat.format(Date(it.timestamp)) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        grouped.forEach { (dayLabel, dayMessages) ->
            item(key = "header-$dayLabel") {
                DayHeader(dayLabel)
            }
            items(dayMessages, key = { it.id }) { message ->
                MessageRow(message)
            }
        }
        // Bottom padding so the FAB doesn't cover the last item
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun DayHeader(day: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MessageRow(message: CapturedMessage) {
    val displayText = when (message.messageType) {
        "image" -> "\uD83D\uDCF7 Foto"
        "video" -> "\uD83D\uDCF9 Vídeo"
        "audio" -> "\uD83C\uDFA4 Áudio"
        "document" -> "\uD83D\uDCC4 Documento"
        "sticker" -> "Figurinha"
        "gif" -> "GIF"
        "location" -> "\uD83D\uDCCD Localização"
        "contact" -> "Cartão de contato"
        else -> message.text
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = "[${timeFormat.format(Date(message.timestamp))}]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.author,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.messageType == "text")
                    MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
