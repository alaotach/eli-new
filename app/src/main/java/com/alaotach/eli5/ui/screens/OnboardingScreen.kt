package com.alaotach.eli5.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alaotach.eli5.ui.theme.ELI5Blue

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onGrantPermission: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Indicators
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (index == currentStep) ELI5Blue else Color.DarkGray,
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Content Area
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (currentStep) {
                0 -> WelcomeStep()
                1 -> PermissionStep(onGrantPermission)
                2 -> ExplanationStep()
                3 -> FinalStep(onFinish)
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentStep > 0) {
                TextButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Back", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = {
                    if (currentStep < totalSteps - 1) currentStep++
                    else onFinish()
                },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ELI5Blue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (currentStep == totalSteps - 1) "Done ✓" else "Next →")
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Placeholder for the "ELI5" logo/text image from screenshot
        Text("ELI 5", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(32.dp))
        Text("Understand anything.\nInstantly.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Highlight any word in any app.\nWe explain it like you're 5.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun PermissionStep(onGrantPermission: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(12.dp), color = ELI5Blue.copy(alpha = 0.2f)) {
            // Icon placeholder
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("One permission.\nUnlimited power.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("ELI5 needs to display over other apps so the explanation card can appear on top of Chrome, WhatsApp, and everywhere else.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onGrantPermission, colors = ButtonDefaults.buttonColors(containerColor = ELI5Blue)) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun ExplanationStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Highlight. Tap. Understand.", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Works in WhatsApp, Chrome, Twitter, PDFs — everywhere.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        // Image placeholder showing text selection
    }
}

@Composable
fun FinalStep(onFinish: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("You're all set", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Enjoy understanding the world effortlessly.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = ELI5Blue)) {
            Text("Start using ELI5")
        }
    }
}
