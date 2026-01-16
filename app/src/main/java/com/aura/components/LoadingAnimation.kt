package com.aura.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    message: String? = null,
    type: LoadingType = LoadingType.Circular
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            LoadingType.Circular -> CircularLoadingAnimation(message = message)
            LoadingType.Pulse -> PulseLoadingAnimation(message = message)
            LoadingType.Wave -> WaveLoadingAnimation(message = message)
            LoadingType.Aura -> AuraLogoLoadingAnimation(message = message)
            LoadingType.Lottie -> LottieLoadingAnimation(message = message)
        }
    }
}

@Composable
fun CircularLoadingAnimation(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CircularLoading")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        ), label = "Rotation"
    )

    val sweepAngle = infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 330f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1500
                30f at 0
                330f at 750
                30f at 1500
            }
        ), label = "SweepAngle"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation.value)
        ) {
            val strokeWidth = 8.dp.toPx()
            val canvasSize = size.minDimension - strokeWidth

            drawArc(
                color = Color(0xFF6200EE),
                startAngle = -90f,
                sweepAngle = sweepAngle.value,
                useCenter = false,
                topLeft = Offset(
                    (size.width - canvasSize) / 2,
                    (size.height - canvasSize) / 2
                ),
                size = Size(canvasSize, canvasSize),
                style = Stroke(strokeWidth)
            )
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun PulseLoadingAnimation(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseLoading")

    val scale1 = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        ), label = "Scale1"
    )

    val alpha1 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        ), label = "Alpha1"
    )

    val scale2 = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing,
                delayMillis = 500
            )
        ), label = "Scale2"
    )

    val alpha2 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing,
                delayMillis = 500
            )
        ), label = "Alpha2"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer pulse 1
            Box(
                modifier = Modifier
                    .size(80.dp * scale1.value)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha1.value * 0.3f)
                    )
            )

            // Outer pulse 2
            Box(
                modifier = Modifier
                    .size(80.dp * scale2.value)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = alpha2.value * 0.3f)
                    )
            )

            // Center circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun WaveLoadingAnimation(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveLoading")

    val offset1 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                20f at 350
                0f at 700
                0f at 1400
            }
        ), label = "Offset1"
    )

    val offset2 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 350
                20f at 700
                0f at 1050
                0f at 1400
            }
        ), label = "Offset2"
    )

    val offset3 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 700
                20f at 1050
                0f at 1400
            }
        ), label = "Offset3"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            WaveDot(
                offset = offset1.value,
                color = MaterialTheme.colorScheme.primary,
                delay = 0
            )
            WaveDot(
                offset = offset2.value,
                color = MaterialTheme.colorScheme.secondary,
                delay = 350
            )
            WaveDot(
                offset = offset3.value,
                color = MaterialTheme.colorScheme.tertiary,
                delay = 700
            )
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun WaveDot(
    offset: Float,
    color: Color,
    delay: Int
) {
    val size = 20.dp
    val infiniteTransition = rememberInfiniteTransition(label = "WaveDot")

    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.3f at delay
                1f at delay + 350
                0.3f at delay + 700
                0.3f at 1400
            }
        ), label = "Alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .offset(y = (-offset).dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = alpha.value),
                radius = size.toPx() / 2
            )
        }
    }
}

@Composable
fun AuraLogoLoadingAnimation(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraLoading")

    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            )
        ), label = "Rotation"
    )

    val scale = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.8f at 0
                1.2f at 1000
                0.8f at 2000
            }
        ), label = "Scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation.value)
            ) {
                val strokeWidth = 8.dp.toPx()
                val canvasSize = size.minDimension - strokeWidth

                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color(0xFF6200EE).copy(alpha = 0.3f),
                        0.5f to Color(0xFF03DAC6).copy(alpha = 0.5f),
                        1f to Color(0xFFFF4081).copy(alpha = 0.3f)
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(
                        (size.width - canvasSize) / 2,
                        (size.height - canvasSize) / 2
                    ),
                    size = Size(canvasSize, canvasSize),
                    style = Stroke(strokeWidth)
                )
            }

            // Inner logo
            Box(
                modifier = Modifier
                    .size(60.dp * scale.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6200EE),
                                Color(0xFF03DAC6)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "AURA",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
fun LottieLoadingAnimation(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.aura.R.raw.loading_animation)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(120.dp)
        )

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun LoadingScreen(
    message: String = "Loading...",
    type: LoadingType = LoadingType.Aura
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        modifier = Modifier.fillMaxSize()
    ) {
        LoadingAnimation(
            message = message,
            type = type
        )
    }
}

@Composable
fun LoadingButton(
    isLoading: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loadingText: String = "Processing..."
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = loadingText,
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String? = null
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularLoadingAnimation()

                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkeletonLoading(
    type: SkeletonType = SkeletonType.List,
    itemCount: Int = 5
) {
    when (type) {
        SkeletonType.List -> ListSkeleton(itemCount = itemCount)
        SkeletonType.Grid -> GridSkeleton(itemCount = itemCount)
        SkeletonType.Detail -> DetailSkeleton()
        SkeletonType.Profile -> ProfileSkeleton()
    }
}

@Composable
fun ListSkeleton(itemCount: Int = 5) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            CardSkeleton()
        }
    }
}

@Composable
fun CardSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .shimmerEffect()
    )
}

@Composable
fun GridSkeleton(itemCount: Int = 6) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(itemCount / 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun DetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )

        // Title
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        // Content
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
    }
}

@Composable
fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )

        // Name
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        // Info items
        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}

enum class LoadingType {
    Circular,
    Pulse,
    Wave,
    Aura,
    Lottie
}

enum class SkeletonType {
    List,
    Grid,
    Detail,
    Profile
}
