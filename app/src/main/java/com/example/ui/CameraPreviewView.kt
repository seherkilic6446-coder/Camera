package com.example.ui

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.Purple80
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutionException

@Composable
fun CameraPreviewView(
    isBackCamera: Boolean,
    flashState: String,
    selectedMode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraHardware by remember { mutableStateOf(true) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Attempt to bind real CameraX preview lifecycle
    LaunchedEffect(isBackCamera, flashState, selectedMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                
                // Select camera lens selector
                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                // Create camera preview configuration
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Clear any previous bindings
                provider.unbindAll()

                // Register camera lifecycle to owner, and bind preview usecase
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                hasCameraHardware = true
            } catch (e: Exception) {
                Log.e("CameraPreviewView", "Camera binding failed: ${e.message}")
                hasCameraHardware = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraHardware) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High fidelity simulated active viewfinder if camera fails or inside emulator
            CameraFallbackSimulator(
                selectedMode = selectedMode,
                isBackCamera = isBackCamera,
                flashState = flashState
            )
        }

        // Overlay artistic Material 3 Viewfinder parameters
        ViewfinderOverlay(selectedMode = selectedMode)
    }
}

@Composable
fun CameraFallbackSimulator(
    selectedMode: String,
    isBackCamera: Boolean,
    flashState: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simulated_feed")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    val scaleIntensity by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    // Draw ambient digital mesh to represent focus and visual scanning space
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Background dark radial visual tone
                drawRect(Color(0xFF0F0F13))

                // Beautiful interactive glowing mesh grid lines
                val gridCountX = 12
                val gridCountY = 20
                val gridWidth = size.width / gridCountX
                val gridHeight = size.height / gridCountY

                // Vertical lines
                for (i in 0..gridCountX) {
                    val x = i * gridWidth
                    drawLine(
                        color = Color(0x334433FF),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                }

                // Horizontal lines
                for (j in 0..gridCountY) {
                    val y = j * gridHeight
                    drawLine(
                        color = Color(0x334433FF),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // High-fidelity focal scanning rings
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                drawCircle(
                    color = Color(0x886A22F0),
                    radius = 220f * scaleIntensity,
                    center = Offset(centerX, centerY),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                    )
                )

                drawCircle(
                    color = Color(0xFF00FFCC),
                    radius = 70f * (2f - scaleIntensity),
                    center = Offset(centerX, centerY),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), sweepAngle)
                    )
                )
            }
    ) {
        // Mode badge indicator on top left
        Row(
            modifier = Modifier
                .safeContentPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selectedMode == "Video") Color.Red else Color.Green)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Feed: SIMULATOR // ${selectedMode.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }

        // Lens Info/Status Indicators on Top Right
        Column(
            modifier = Modifier
                .safeContentPadding()
                .padding(16.dp)
                .align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = if (isBackCamera) "REAR_LENS: WIDE 24MM" else "FRONT_LENS: SEGMENT 12MM",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "FLASH: ${flashState.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ISO 240 / F1.8 / 1/120S",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF00FFCC)
            )
        }
    }
}

@Composable
fun ViewfinderOverlay(selectedMode: String) {
    // Elegant crosshair, frame bounds, and active overlay to deliver highly stylized Material 3 experience
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 3f
        val lineLength = 40f
        val padding = 60f

        // 1. Draw outer focus corner brackets
        // Top Left
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(padding, padding),
            end = Offset(padding + lineLength, padding),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(padding, padding),
            end = Offset(padding, padding + lineLength),
            strokeWidth = strokeWidth
        )

        // Top Right
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(size.width - padding, padding),
            end = Offset(size.width - padding - lineLength, padding),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(size.width - padding, padding),
            end = Offset(size.width - padding, padding + lineLength),
            strokeWidth = strokeWidth
        )

        // Bottom Left
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(padding, size.height - padding),
            end = Offset(padding + lineLength, size.height - padding),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(padding, size.height - padding),
            end = Offset(padding, size.height - padding - lineLength),
            strokeWidth = strokeWidth
        )

        // Bottom Right
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(size.width - padding, size.height - padding),
            end = Offset(size.width - padding - lineLength, size.height - padding),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(size.width - padding, size.height - padding),
            end = Offset(size.width - padding, size.height - padding - lineLength),
            strokeWidth = strokeWidth
        )

        // 2. Center Crosshairs
        val cx = size.width / 2f
        val cy = size.height / 2f
        val crossLength = 16f
        
        // Horizontal crosshair line
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(cx - crossLength, cy),
            end = Offset(cx + crossLength, cy),
            strokeWidth = 2f
        )
        // Vertical crosshair line
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = Offset(cx, cy - crossLength),
            end = Offset(cx, cy + crossLength),
            strokeWidth = 2f
        )
    }
}
