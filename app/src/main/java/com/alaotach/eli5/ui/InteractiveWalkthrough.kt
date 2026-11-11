package com.alaotach.eli5.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alaotach.eli5.R
import kotlin.math.roundToInt

class WalkthroughStep(
    val id: String,
    val targetKey: String,
    val titleRes: Int,
    val descRes: Int,
    val actionTextRes: Int? = null,
    val onAction: (() -> Unit)? = null
)

@Stable
class WalkthroughState {
    var isVisible by mutableStateOf(false)
    var currentStepIndex by mutableIntStateOf(0)
    var isCompletionVisible by mutableStateOf(false)
    val targetCoordinates = mutableStateMapOf<String, LayoutCoordinates>()

    fun registerTarget(key: String, coordinates: LayoutCoordinates) {
        targetCoordinates[key] = coordinates
    }

    fun startWalkthrough() {
        currentStepIndex = 0
        isCompletionVisible = false
        isVisible = true
    }

    fun nextStep(totalSteps: Int) {
        if (currentStepIndex + 1 >= totalSteps) {
            isCompletionVisible = true
        } else {
            currentStepIndex++
        }
    }

    fun dismiss() {
        isVisible = false
        isCompletionVisible = false
    }
}

@Composable
fun rememberWalkthroughState(): WalkthroughState {
    return remember { WalkthroughState() }
}

fun Modifier.walkthroughTarget(key: String, state: WalkthroughState): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            state.registerTarget(key, coordinates)
        }
    }
)

@Composable
fun WalkthroughOverlay(
    state: WalkthroughState,
    isDark: Boolean,
    isAssistantEnabled: Boolean,
    onTryGesture: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onWatchVideoTutorial: () -> Unit,
    onFinish: () -> Unit
) {
    if (!state.isVisible) return

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeightDp = config.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    var overlayCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val steps = remember(onTryGesture, onOpenAssistantSettings, isAssistantEnabled) {
        listOf(
            // Step 1: Circle to ELI5 Assistant Setup
            WalkthroughStep(
                id = "circle_to_eli5",
                targetKey = "circle_to_eli5_card",
                titleRes = if (isAssistantEnabled) R.string.walkthrough_step1_enabled_title else R.string.walkthrough_step1_title,
                descRes = if (isAssistantEnabled) R.string.walkthrough_step1_enabled_desc else R.string.walkthrough_step1_desc,
                actionTextRes = if (isAssistantEnabled) R.string.walkthrough_next else R.string.walkthrough_open_settings,
                onAction = if (isAssistantEnabled) null else onOpenAssistantSettings
            ),
            // Step 2: Try Gesture
            WalkthroughStep(
                id = "try_gesture",
                targetKey = "try_gesture_card",
                titleRes = R.string.walkthrough_step2_title,
                descRes = R.string.walkthrough_step2_desc,
                actionTextRes = R.string.walkthrough_try_demo,
                onAction = onTryGesture
            ),
            // Step 3: Direct text & tone mode
            WalkthroughStep(
                id = "input_box",
                targetKey = "input_box_card",
                titleRes = R.string.walkthrough_step3_title,
                descRes = R.string.walkthrough_step3_desc
            ),
            // Step 4: Highlight text everywhere
            WalkthroughStep(
                id = "explain_button",
                targetKey = "explain_button_card",
                titleRes = R.string.walkthrough_step4_title,
                descRes = R.string.walkthrough_step4_desc
            )
        )
    }

    val currentStep = steps.getOrNull(state.currentStepIndex) ?: steps[0]
    val currentTargetCoords = state.targetCoordinates[currentStep.targetKey]

    // Calculate exact pixel position of the target relative to this overlay
    val targetRect: Rect? = if (overlayCoordinates != null && currentTargetCoords != null &&
        overlayCoordinates!!.isAttached && currentTargetCoords.isAttached
    ) {
        try {
            val topLeft = overlayCoordinates!!.localPositionOf(currentTargetCoords, Offset.Zero)
            val size = currentTargetCoords.size
            Rect(topLeft, Size(size.width.toFloat(), size.height.toFloat()))
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }

    // Animated dash stroke phase
    val infiniteTransition = rememberInfiniteTransition(label = "dash")
    val dashPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashPhase"
    )

    BackHandler {
        if (state.isCompletionVisible) {
            onFinish()
        } else if (state.currentStepIndex > 0) {
            state.currentStepIndex--
        } else {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayCoordinates = it }
    ) {
        // Spotlight Scrim Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // 1. Semi-transparent dark overlay
            drawRect(
                color = Color.Black.copy(alpha = if (isDark) 0.82f else 0.74f),
                size = size
            )

            // 2. Cutout spotlight hole with pixel-perfect local positioning
            if (!state.isCompletionVisible && targetRect != null) {
                val pad = with(density) { 5.dp.toPx() }
                val cornerRadiusPx = with(density) { 20.dp.toPx() }

                val cutoutLeft = (targetRect.left - pad).coerceAtLeast(0f)
                val cutoutTop = (targetRect.top - pad).coerceAtLeast(0f)
                val cutoutRight = (targetRect.right + pad).coerceAtMost(size.width)
                val cutoutBottom = (targetRect.bottom + pad).coerceAtMost(size.height)
                val cutoutSize = Size(cutoutRight - cutoutLeft, cutoutBottom - cutoutTop)

                // Punch clear hole
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(cutoutLeft, cutoutTop),
                    size = cutoutSize,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    blendMode = BlendMode.Clear
                )

                // Draw glowing dashed focus border around cutout
                drawRoundRect(
                    color = Color(0xFFC084FC),
                    topLeft = Offset(cutoutLeft, cutoutTop),
                    size = cutoutSize,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(
                        width = with(density) { 2.5.dp.toPx() },
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f), dashPhase)
                    )
                )
            }
        }

        // Click-through interception on spotlight cutout:
        // Tapping the spotlighted card directly triggers the step's primary action!
        if (!state.isCompletionVisible && targetRect != null && currentStep.onAction != null) {
            val pad = with(density) { 5.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (targetRect.left - pad).roundToInt(),
                            (targetRect.top - pad).roundToInt()
                        )
                    }
                    .size(
                        with(density) { (targetRect.width + pad * 2).toDp() },
                        with(density) { (targetRect.height + pad * 2).toDp() }
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        currentStep.onAction.invoke()
                    }
            )
        }

        // Top Header: Skip Button (visible when not in completion screen)
        if (!state.isCompletionVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable {
                        state.isCompletionVisible = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.walkthrough_skip),
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Skip",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Content Area: Either Floating Tooltip or Completion Dialog
        if (state.isCompletionVisible) {
            WalkthroughCompletionCard(
                isDark = isDark,
                onStartUsing = {
                    onFinish()
                },
                onReplay = {
                    state.startWalkthrough()
                },
                onWatchVideo = {
                    onFinish()
                    onWatchVideoTutorial()
                }
            )
        } else {
            // Position tooltip dynamically either above or below the target with zero overlap
            val overlayHeightPx = overlayCoordinates?.size?.height?.toFloat() ?: screenHeightPx
            val tooltipEstimatedHeightPx = with(density) { 220.dp.toPx() }
            val spaceBelowPx = if (targetRect != null) overlayHeightPx - targetRect.bottom else overlayHeightPx * 0.5f
            val spaceAbovePx = if (targetRect != null) targetRect.top else overlayHeightPx * 0.5f

            val placeBelow = if (targetRect == null) {
                true
            } else if (spaceBelowPx >= tooltipEstimatedHeightPx) {
                true
            } else if (spaceAbovePx >= tooltipEstimatedHeightPx) {
                false
            } else {
                spaceBelowPx >= spaceAbovePx
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = if (placeBelow) Alignment.TopCenter else Alignment.BottomCenter
            ) {
                val verticalOffset = if (targetRect != null) {
                    if (placeBelow) {
                        with(density) { (targetRect.bottom + 14.dp.toPx()).toDp() }.coerceAtLeast(16.dp)
                    } else {
                        with(density) { (overlayHeightPx - targetRect.top + 14.dp.toPx()).toDp() }.coerceAtLeast(16.dp)
                    }
                } else {
                    140.dp
                }

                Box(
                    modifier = if (placeBelow) {
                        Modifier.padding(top = verticalOffset)
                    } else {
                        Modifier.padding(bottom = verticalOffset)
                    }
                ) {
                    WalkthroughTooltipCard(
                        stepIndex = state.currentStepIndex + 1,
                        totalSteps = steps.size,
                        title = stringResource(currentStep.titleRes),
                        description = stringResource(currentStep.descRes),
                        actionText = currentStep.actionTextRes?.let { stringResource(it) },
                        onAction = currentStep.onAction,
                        isLastStep = state.currentStepIndex == steps.size - 1,
                        isDark = isDark,
                        onNext = {
                            state.nextStep(steps.size)
                        },
                        onSkip = {
                            state.isCompletionVisible = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WalkthroughTooltipCard(
    stepIndex: Int,
    totalSteps: Int,
    title: String,
    description: String,
    actionText: String?,
    onAction: (() -> Unit)?,
    isLastStep: Boolean,
    isDark: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E1829) else Color.White
    val cardBorder = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.4f)
    val textPrimary = if (isDark) Color(0xFFF3EEFF) else Color(0xFF1E1430)
    val textSecondary = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5B4D73)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(cardBg)
            .border(BorderStroke(1.5.dp, cardBorder), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Step Badge & Counter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
            ) {
                Text(
                    text = stringResource(R.string.walkthrough_step_counter, stepIndex, totalSteps),
                    color = Color(0xFFA855F7),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = stringResource(R.string.walkthrough_skip),
                color = textSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onSkip() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = title,
            color = textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 23.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Description
        Text(
            text = description,
            color = textSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (actionText != null && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = actionText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onNext,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF8B5CF6)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text(
                        text = if (isLastStep) stringResource(R.string.walkthrough_finish) else stringResource(R.string.walkthrough_next),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WalkthroughCompletionCard(
    isDark: Boolean,
    onStartUsing: () -> Unit,
    onReplay: () -> Unit,
    onWatchVideo: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E1829) else Color.White
    val cardBorder = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.4f)
    val textPrimary = if (isDark) Color(0xFFF3EEFF) else Color(0xFF1E1430)
    val textSecondary = if (isDark) Color(0xFFC4B5FD) else Color(0xFF5B4D73)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(cardBg)
                .border(BorderStroke(1.5.dp, cardBorder), RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Icon Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFF6A0DAD))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Completion Title
            Text(
                text = stringResource(R.string.walkthrough_complete_title),
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Completion Subtitle
            Text(
                text = stringResource(R.string.walkthrough_complete_subtitle),
                color = textSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Option 1: Start Using ELI5 (Primary)
            Button(
                onClick = onStartUsing,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C3AED)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.walkthrough_action_start),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Watch Full App Tutorial Video
            OutlinedButton(
                onClick = onWatchVideo,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF8B5CF6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.walkthrough_action_video),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 3: Replay Walkthrough
            TextButton(
                onClick = onReplay,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Replay,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.walkthrough_action_replay),
                    color = textSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
