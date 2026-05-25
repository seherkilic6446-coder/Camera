package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.CallHistory
import com.example.data.CapturedPhoto
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAndCallUi(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Observe flows from ViewModel
    val currentCall by viewModel.currentCallState.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val waveformLevels by viewModel.audioWaveformLevels.collectAsStateWithLifecycle()

    val photos by viewModel.capturedPhotos.collectAsStateWithLifecycle()
    val logs by viewModel.callLogs.collectAsStateWithLifecycle()

    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val isBackCamera by viewModel.isBackCamera.collectAsStateWithLifecycle()
    val flashState by viewModel.flashState.collectAsStateWithLifecycle()
    val selectedPhoto by viewModel.selectedPhoto.collectAsStateWithLifecycle()

    // Screen navigation / overlay state
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSimConfigDialog by remember { mutableStateOf(false) }
    var isCallMinimized by remember { mutableStateOf(false) }

    // New HUD settings and sidebar state
    var isHudHidden by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isSideSheetOpen by remember { mutableStateOf(false) }

    // Simulation fields
    var simName by remember { mutableStateOf("Mom ❤️") }
    var simNumber by remember { mutableStateOf("+1 (555) 019-2834") }
    var simDelaySeconds by remember { mutableStateOf("0") }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- LAYER 1: Core Camera Viewport ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (isHudHidden) {
                        isHudHidden = false
                    }
                }
        ) {
            CameraPreviewView(
                isBackCamera = isBackCamera,
                flashState = flashState,
                selectedMode = selectedMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Invisible / subtle indicator to let user know they can tap to restore HUD
        if (isHudHidden) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "HUD Hidden • Tap Screen to Reveal",
                    color = Color(0xFF00FFCC),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // --- LAYER 2: Camera Controls Overlay ---
        AnimatedVisibility(
            visible = !isHudHidden,
            enter = fadeIn(animationSpec = tween(350)),
            exit = fadeOut(animationSpec = tween(350))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .statusBarsPadding()
            ) {
            // Header: Side Sheet, Flash, Title, Sim, History, Settings Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prepend Side Sheet Open Button (Standard M3 Sidebar)
                    IconButton(
                        onClick = { isSideSheetOpen = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Open Sidebar Side Sheet",
                            tint = Color.White
                        )
                    }

                    // Flash Control
                    IconButton(
                        onClick = { viewModel.toggleFlash() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(44.dp)
                    ) {
                        val icon = when (flashState) {
                            "On" -> Icons.Default.FlashOn
                            "Off" -> Icons.Default.FlashOff
                            else -> Icons.Default.FlashAuto
                        }
                        Icon(icon, contentDescription = "Toggle Flash", tint = Color.White)
                    }
                }

                // Title Banner or Active State Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Rounded.Videocam,
                        contentDescription = "Active Hub",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "CAMCALL HUB",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                // Actions: Simulated dialer, call database logs, and Settings Corner (NO SETTINGS ICON)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call Simulation Configuration Drawer Button
                    IconButton(
                        onClick = { showSimConfigDialog = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.PhoneCallback,
                            contentDescription = "Incoming Call Simulator",
                            tint = Color(0xFF00FFCC)
                        )
                    }

                    // Call logs history
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "View Call History",
                            tint = Color.White
                        )
                    }

                    // HUD Settings Corner (Pill label + Switch dropdown, NO SETTINGS ICON)
                    Box {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .clickable { showMenu = true }
                                .padding(horizontal = 10.dp)
                                .height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FFCC))
                                )
                                Text(
                                    text = "MENU",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Hide HUD View",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = isHudHidden,
                                            onCheckedChange = {
                                                isHudHidden = it
                                                showMenu = false
                                            },
                                            thumbContent = {
                                                Icon(
                                                    imageVector = if (isHudHidden) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                    }
                                },
                                onClick = { }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Zone containing: Mode Selector, Captures Strip, Main capturing actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Horizontal list of modes centered
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modes = listOf("Photo", "Video", "Night", "Portrait")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 30.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(modes) { mode ->
                            val isSelected = selectedMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.35f
                                        ) else Color.Transparent
                                    )
                                    .clickable { viewModel.setMode(mode) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mode.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(
                                        alpha = 0.6f
                                    )
                                )
                            }
                        }
                    }
                }

                // Gallery preview strip & Capture Button Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Gallery thumbnail
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                if (photos.isNotEmpty()) {
                                    viewModel.selectPhoto(photos.first())
                                } else {
                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            "No photos captured yet!",
                                            android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            },
                        color = Color.DarkGray
                    ) {
                        if (photos.isNotEmpty()) {
                            AsyncImage(
                                model = File(photos.first().filePath),
                                contentDescription = "Latest Photo Captured",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary,
                                    contentDescription = "Empty Gallery",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Center: Large Premium Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.capturePhotoAndSave(context) }
                            .drawBehind {
                                // Outer capture guidelines halo ringing
                                drawCircle(
                                    color = Color.White,
                                    radius = size.minDimension / 2f - 4f,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    if (selectedMode == "Video") Color.Red else Color.White
                                )
                        )
                    }

                    // Right: Camera facing rotator
                    IconButton(
                        onClick = { viewModel.toggleCameraFacing() },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Lens Direction",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

        // --- LAYER 3: Minimized Phone Call UI (PIP Widget) ---
        if (currentCall is CallState.Talking && isCallMinimized) {
            MinimizedCallPip(
                talkingState = currentCall as CallState.Talking,
                waveformLevels = waveformLevels,
                onHangUp = { viewModel.hangUpCall() },
                onExpand = { isCallMinimized = false }
            )
        }

        // --- LAYER 4: Fullscreen Incoming Call (Ringing) Alert ---
        AnimatedVisibility(
            visible = currentCall is CallState.Ringing,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(600, easing = EaseOutExpo)
            ),
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(500, easing = EaseInExpo)
            )
        ) {
            if (currentCall is CallState.Ringing) {
                RingingOverlayScreen(
                    ringingState = currentCall as CallState.Ringing,
                    onAnswer = { state ->
                        viewModel.answerCall(state.callerName, state.phoneNumber)
                    },
                    onDecline = { viewModel.hangUpCall() }
                )
            }
        }

        // --- LAYER 5: Fullscreen Active Talking Session Dialog ---
        AnimatedVisibility(
            visible = (currentCall is CallState.Talking && !isCallMinimized),
            enter = scaleIn(initialScale = 0.9f) + fadeIn(),
            exit = scaleOut(targetScale = 0.85f) + fadeOut()
        ) {
            if (currentCall is CallState.Talking) {
                TalkingOverlayScreen(
                    talkingState = currentCall as CallState.Talking,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    waveformLevels = waveformLevels,
                    onMuteToggle = { viewModel.toggleMute() },
                    onSpeakerToggle = { viewModel.toggleSpeaker() },
                    onMinimize = { isCallMinimized = true },
                    onHangUp = { viewModel.hangUpCall() }
                )
            }
        }

        // --- LAYER 6: Photo Detailed Viewer Dialogue ---
        selectedPhoto?.let { photo ->
            PhotoDetailViewer(
                photo = photo,
                onDismiss = { viewModel.selectPhoto(null) },
                onDelete = {
                    viewModel.deletePhoto(photo)
                }
            )
        }

        // --- LAYER 7: Floating Simulator Setup Dialogue ---
        if (showSimConfigDialog) {
            Dialog(onDismissRequest = { showSimConfigDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Call Simulator Config",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Configure parameters to simulate incoming dials perfectly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Name input
                        OutlinedTextField(
                            value = simName,
                            onValueChange = { simName = it },
                            label = { Text("Caller Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Number input
                        OutlinedTextField(
                            value = simNumber,
                            onValueChange = { simNumber = it },
                            label = { Text("Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Delay selector
                        OutlinedTextField(
                            value = simDelaySeconds,
                            onValueChange = { simDelaySeconds = it },
                            label = { Text("Delay (Seconds)") },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Action rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showSimConfigDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    showSimConfigDialog = false
                                    val sec = simDelaySeconds.toIntOrNull() ?: 0
                                    if (sec <= 0) {
                                        viewModel.simulateIncomingCall(simName, simNumber)
                                    } else {
                                        viewModel.scheduleIncomingCall(simName, simNumber, sec)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Incoming call scheduled in $sec seconds",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Default.PhonelinkRing, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Trigger Call")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Or place an immediate outgoing call!
                        Button(
                            onClick = {
                                showSimConfigDialog = false
                                viewModel.placeOutgoingCall(simName, simNumber)
                                isCallMinimized = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FFCC),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Outgoing Call Now")
                        }
                    }
                }
            }
        }

        // --- LAYER 8: Call History and Archive Logs Dialog ---
        if (showHistoryDialog) {
            Dialog(onDismissRequest = { showHistoryDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Call History Logs",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { viewModel.clearCallLogs() }) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = "Clear All Logs",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (logs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PhoneMissed,
                                        contentDescription = "Empty History",
                                        modifier = Modifier.size(72.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No recorded call logs present.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    logs.forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 10.dp)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val (icon, color) = when (log.status) {
                                                    "INCOMING_ANSWERED" -> Icons.Default.CallReceived to Color.Green
                                                    else -> Icons.Default.CallMissed to Color.Red
                                                }
                                                Icon(
                                                    icon,
                                                    contentDescription = null,
                                                    tint = color
                                                )
                                                Column {
                                                    Text(
                                                        text = log.callerName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Text(
                                                        text = log.phoneNumber,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${log.durationSeconds}s duration",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = SimpleDateFormat(
                                                        "HH:mm",
                                                        Locale.getDefault()
                                                    ).format(Date(log.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showHistoryDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss Archive")
                        }
                    }
                }
            }
        }

        // --- LAYER 9: Standard Side Sheet Sidebar ---
        if (isSideSheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isSideSheetOpen = false }
            )
        }

        AnimatedVisibility(
            visible = isSideSheetOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(256.dp)
                    .background(Color(0xFF151025))
                    .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.2f))
                    .shadow(16.dp)
                    .navigationBarsPadding()
                    .statusBarsPadding()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Side Sheet Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AppSettingsAlt,
                                contentDescription = null,
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = context.getString(com.example.R.string.side_sheet_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "LENS PRESET CHANNELS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color(0xFF00FFCC)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render cool custom side sheet presets
                        val sideModes = listOf("Photo", "Video", "Night", "Portrait")
                        sideModes.forEach { mode ->
                            val active = selectedMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setMode(mode)
                                        isSideSheetOpen = false
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        "Video" -> Icons.Default.Videocam
                                        "Night" -> Icons.Default.ModeNight
                                        "Portrait" -> Icons.Default.Face
                                        else -> Icons.Default.CameraAlt
                                    },
                                    contentDescription = null,
                                    tint = if (active) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) Color(0xFF00FFCC) else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "QUICK SIMULATOR DIAL",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isSideSheetOpen = false
                                viewModel.simulateIncomingCall("Mom ❤️", "+1 (555) 019-2834")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8822F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fast Call Test", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Side Sheet Dismiss Button (Mapping standard_side_sheet layout dismiss button action)
                    Button(
                        onClick = { isSideSheetOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE22238)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = context.getString(com.example.R.string.action))
                    }
                }
            }
        }
    }
}

// --- Dynamic Ringing Mode Overlay ---
@Composable
fun RingingOverlayScreen(
    ringingState: CallState.Ringing,
    onAnswer: (CallState.Ringing) -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_avatar")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF320E4E), Color(0xFF0F0E17)),
                    radius = 1200f
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Pulse Ringing Icon/Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .drawBehind {
                        // Drawing glowing custom circles matching pulses
                        drawCircle(
                            color = Color(0xFFE222F0).copy(alpha = 0.2f),
                            radius = (size.minDimension / 2f) * avatarScale
                        )
                        drawCircle(
                            color = Color(0xFFE222F0).copy(alpha = 0.1f),
                            radius = (size.minDimension / 2f) * (avatarScale + 0.25f)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8822F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ringingState.callerName.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 42.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle status pulse
            Text(
                text = "INCOMING PHONE CALL",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.5.sp),
                color = Color(0xFF00FFCC),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Caller name
            Text(
                text = ringingState.callerName,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Phone details
            Text(
                text = ringingState.phoneNumber,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Answers Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject Action
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE22238))
                    ) {
                        Icon(
                            Icons.Filled.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Decline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Accept Action
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { onAnswer(ringingState) },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C248))
                    ) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Answer Call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pick up",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// --- Active Talk Screen Mode ---
@Composable
fun TalkingOverlayScreen(
    talkingState: CallState.Talking,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    waveformLevels: List<Float>,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onMinimize: () -> Unit,
    onHangUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F1E24), Color(0xFF0C1014))
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header actions: Minimize
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.PictureInPictureAlt,
                        contentDescription = "Minimize talking window",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Small glowing avatar represent talk session active
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                    .border(2.dp, Color(0xFF00FFCC), CircleShape)
            ) {
                Icon(
                    Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Caller name
            Text(
                text = talkingState.callerName,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Duration timer ticking
            val minutes = talkingState.durationSeconds / 60
            val seconds = talkingState.durationSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.5.sp),
                color = Color(0xFF00FFCC),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Waveform equalizer indicating simulated active talk voice signals
            Text(
                text = if (isMuted) "Microphone Muted" else "Talking Inside App • Live Waveform",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                waveformLevels.forEach { amp ->
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(amp.coerceIn(0.1f, 1f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isMuted) Color.White.copy(alpha = 0.3f) else Color(0xFF00FFCC)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Options: Mute, Speaker, Dialer pad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMuteToggle,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMuted) Color.White else Color.White.copy(alpha = 0.1f)
                            )
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute mic",
                            tint = if (isMuted) Color.Black else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mute",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Hang Up Call Ende
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onHangUp,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE22238))
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Hang up call",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "End Call",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Speaker phone route
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onSpeakerToggle,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.1f)
                            )
                    ) {
                        Icon(
                            if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Speaker Toggle",
                            tint = if (isSpeakerOn) Color.Black else Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Speaker",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- Minimized PIP Overlay Widget ---
@Composable
fun MinimizedCallPip(
    talkingState: CallState.Talking,
    waveformLevels: List<Float>,
    onHangUp: () -> Unit,
    onExpand: () -> Unit
) {
    // Beautiful floating micro bubble
    var offsetX by remember { mutableStateOf(20f) }
    var offsetY by remember { mutableStateOf(100f) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xDF0C1014))
            .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clickable { onExpand() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFCC))
                    )
                    Text(
                        text = talkingState.callerName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(100.dp)
                    )
                }

                // Minimized time
                val minutes = talkingState.durationSeconds / 60
                val seconds = talkingState.durationSeconds % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00FFCC)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Micro animated waveform indicator
                Row(
                    modifier = Modifier.width(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    waveformLevels.take(8).forEach { amp ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp * amp.coerceIn(0.15f, 1.0f))
                                .clip(RoundedCornerShape(1.dp))
                                .background(Color(0xFF00FFCC))
                        )
                    }
                }

                // Hangup icon button
                IconButton(
                    onClick = onHangUp,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// --- Photo detail viewer dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailViewer(
    photo: CapturedPhoto,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewing captured file",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Photo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // High fidelity visual loader
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(photo.filePath),
                        contentDescription = "Photo Captured Detail",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Details Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Metadata Info",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mode: ${photo.mode.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(photo.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
