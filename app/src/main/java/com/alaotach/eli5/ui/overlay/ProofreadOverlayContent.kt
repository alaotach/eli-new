package com.alaotach.eli5.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alaotach.eli5.ui.theme.ELI5Blue

@Composable
fun ProofreadOverlayContent(
    sourceText: String,
    viewModel: OverlayViewModel,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit
) {
    val states by viewModel.states.collectAsState()
    val uiState = states["Proofread"] ?: OverlayUiState.Idle

    LaunchedEffect(Unit) {
        viewModel.getExplanation(sourceText, "Proofread")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("✨", fontSize = 16.sp)

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is OverlayUiState.Idle, is OverlayUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ELI5Blue,
                            strokeWidth = 2.dp
                        )
                    }
                    is OverlayUiState.Success -> {
                        Text(
                            text = "\"${state.result}\"",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    }
                    is OverlayUiState.Error -> {
                        Text(
                            text = "Error",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (uiState is OverlayUiState.Success) {
                Button(
                    onClick = { onAccept((uiState as OverlayUiState.Success).result) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ELI5Blue)
                ) {
                    Text("Accept ✓", fontSize = 13.sp)
                }
            }

            TextButton(
                onClick = { /* Adjust */ },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Adjust", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Cancel", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
