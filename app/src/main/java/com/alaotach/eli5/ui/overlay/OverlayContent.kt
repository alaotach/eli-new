package com.alaotach.eli5.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alaotach.eli5.ui.theme.ELI5Blue
import com.alaotach.eli5.ui.theme.ELI5Red

@Composable
fun OverlayContent(
    sourceText: String,
    isReadOnly: Boolean,
    initialMode: String = "ELI5",
    viewModel: OverlayViewModel,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(initialMode) }
    val states by viewModel.states.collectAsState()
    val uiState = states[selectedMode] ?: OverlayUiState.Idle

    LaunchedEffect(selectedMode) {
        viewModel.getExplanation(sourceText, selectedMode)
    }

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.LightGray, RoundedCornerShape(2.dp))
                )
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    sourceText.take(20) + if (sourceText.length > 20) "..." else "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row {
                    TextButton(onClick = { /* Copy */ }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector
            val modes = listOf("ELI5", "Direct", "Detailed")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    modes.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(
                                    if (isSelected) ELI5Blue else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedMode = mode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            when (val state = uiState) {
                is OverlayUiState.Idle, is OverlayUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ELI5Blue)
                    }
                }
                is OverlayUiState.Success -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                state.result,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                is OverlayUiState.Error -> {
                    Text(
                        state.message,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = ELI5Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Actions
            if (selectedMode == "Proofread" || !isReadOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (uiState is OverlayUiState.Success) {
                                onAccept((uiState as OverlayUiState.Success).result)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ELI5Blue),
                        enabled = uiState is OverlayUiState.Success
                    ) {
                        Text("Accept ✓")
                    }
                }
            }
        }
    }
}
