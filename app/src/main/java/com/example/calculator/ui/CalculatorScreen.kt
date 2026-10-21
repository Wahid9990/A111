package com.example.calculator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.calculator.ui.components.AboutDialog
import com.example.calculator.ui.components.CalculatorKeypad
import com.example.calculator.ui.components.HistoryBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expressionScrollState = rememberScrollState()

    // Auto-scroll expression to right when typing
    LaunchedEffect(uiState.expression) {
        expressionScrollState.animateScrollTo(expressionScrollState.maxValue)
    }

    val copyToClipboard: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Calculator", text)
            clipboard.setPrimaryClip(clip)
            scope.launch {
                snackbarHostState.showSnackbar("Copied '$text' to clipboard")
            }
        }
    }

    // Handle back button for bottom sheet / dialog
    BackHandler(enabled = uiState.isHistoryOpen || uiState.isAboutOpen) {
        if (uiState.isHistoryOpen) viewModel.setHistoryOpen(false)
        if (uiState.isAboutOpen) viewModel.setAboutOpen(false)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .testTag("version_chip")
                        ) {
                            Text(
                                text = "v${stringResource(R.string.app_version)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setHistoryOpen(true) },
                        modifier = Modifier.testTag("action_history")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.history.isNotEmpty()) {
                                    Badge {
                                        Text(uiState.history.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Calculation History"
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.setAboutOpen(true) },
                        modifier = Modifier.testTag("action_about")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Display Area (Calculation Expression & Result)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                // Error message banner if any
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.errorMessage?.let { error ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }

                // Expression display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(expressionScrollState),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.expression.isEmpty()) "0" else uiState.expression,
                        fontSize = if (uiState.expression.length > 12) 36.sp else 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.expression.isEmpty()) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("display_expression")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Live Result Preview
                Text(
                    text = when {
                        uiState.liveResult.isNotEmpty() -> "= ${uiState.liveResult}"
                        else -> " "
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .alpha(if (uiState.liveResult.isNotEmpty()) 1f else 0f)
                        .testTag("display_live_result")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick actions (Copy, Backspace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val toCopy = uiState.liveResult.ifEmpty { uiState.expression }
                            copyToClipboard(toCopy)
                        },
                        enabled = uiState.expression.isNotEmpty() || uiState.liveResult.isNotEmpty(),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("quick_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy equation or result",
                            tint = if (uiState.expression.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }

                    IconButton(
                        onClick = { viewModel.onBackspace() },
                        enabled = uiState.expression.isNotEmpty(),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("quick_backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete last character",
                            tint = if (uiState.expression.isNotEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            }

            // Divider / separation
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ) {}

            // Keypad
            CalculatorKeypad(
                onDigit = viewModel::onDigit,
                onOperator = viewModel::onOperator,
                onDecimal = viewModel::onDecimal,
                onParenthesis = viewModel::onParenthesis,
                onPercent = viewModel::onPercent,
                onToggleSign = viewModel::onToggleSign,
                onClear = viewModel::onClear,
                onBackspace = viewModel::onBackspace,
                onEquals = viewModel::onEquals,
                hasInput = uiState.expression.isNotEmpty()
            )
        }

        // History Bottom Sheet
        if (uiState.isHistoryOpen) {
            HistoryBottomSheet(
                sheetState = historySheetState,
                history = uiState.history,
                onDismiss = { viewModel.setHistoryOpen(false) },
                onSelectHistoryItem = { item, useResult ->
                    viewModel.onSelectHistoryItem(item, useResult)
                },
                onCopyResult = copyToClipboard,
                onClearHistory = viewModel::onClearHistory
            )
        }

        // About Dialog
        if (uiState.isAboutOpen) {
            AboutDialog(
                onDismiss = { viewModel.setAboutOpen(false) }
            )
        }
    }
}
