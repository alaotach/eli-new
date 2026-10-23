package com.alaotach.eli5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alaotach.eli5.data.local.HistoryEntity
import com.alaotach.eli5.ui.theme.ELI5Blue
import com.alaotach.eli5.ui.theme.ELI5Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    todayCount: Int,
    history: List<HistoryEntity>,
    onDeleteHistory: (HistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    onNavigateToGuide: () -> Unit,
    currentTheme: Int,
    onThemeChange: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ELI5", fontWeight = FontWeight.Bold) },
                actions = {
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System Theme", fontWeight = if (currentTheme == 0) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { 
                                    onThemeChange(0)
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Light Mode", fontWeight = if (currentTheme == 1) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { 
                                    onThemeChange(1)
                                    showMenu = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark Mode", fontWeight = if (currentTheme == 2) FontWeight.Bold else FontWeight.Normal) },
                                onClick = { 
                                    onThemeChange(2)
                                    showMenu = false 
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Clear All History", color = ELI5Red) },
                                onClick = {
                                    showMenu = false
                                    onClearAll()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "$todayCount",
                    subtitle = "Words Explained Today",
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    title = "How to Use",
                    subtitle = "Interactive Guide",
                    onClick = onNavigateToGuide,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Your Recent Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                EmptyHistoryView(onNavigateToGuide)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history) { item ->
                        HistoryItem(item, onDeleteHistory)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ELI5Blue),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun HistoryItem(item: HistoryEntity, onDelete: (HistoryEntity) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (item.type == "Proofread") ELI5Red.copy(alpha = 0.2f) else ELI5Blue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        item.type,
                        color = if (item.type == "Proofread") ELI5Red else ELI5Blue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ELI5Red, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.sourceText, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        }
    }
}

@Composable
fun EmptyHistoryView(onNavigateToGuide: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No history yet!", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Highlight some text anywhere and select ELI5 to get started.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToGuide,
            colors = ButtonDefaults.buttonColors(containerColor = ELI5Blue)
        ) {
            Text("How to Use ELI5")
        }
    }
}
