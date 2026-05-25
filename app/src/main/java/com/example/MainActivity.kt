package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.CameraAndCallUi
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isReceiverRegistered = false

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
                    viewModel.notifyRealIncomingCall(incomingNumber)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on since it's a camera and dialer system
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var cameraGranted by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    var phoneGranted by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        cameraGranted = permissions[Manifest.permission.CAMERA] ?: cameraGranted
                        phoneGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: phoneGranted
                    }

                    // Register background receiver when permissions are granted
                    LaunchedEffect(phoneGranted) {
                        if (phoneGranted && !isReceiverRegistered) {
                            try {
                                registerReceiver(
                                    phoneStateReceiver,
                                    IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                                )
                                isReceiverRegistered = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (cameraGranted && phoneGranted) {
                            // Render main Application interface
                            CameraAndCallUi(viewModel = viewModel)
                        } else {
                            // Render beautiful Material 3 introductory Dashboard requesting access
                            PermissionIntroScreen(
                                isCameraGranted = cameraGranted,
                                isPhoneGranted = phoneGranted,
                                onRequestAccess = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.READ_PHONE_STATE,
                                            Manifest.permission.ANSWER_PHONE_CALLS,
                                            Manifest.permission.CALL_PHONE
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(phoneStateReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun PermissionIntroScreen(
    isCameraGranted: Boolean,
    isPhoneGranted: Boolean,
    onRequestAccess: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0C20), Color(0xFF151025))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header lock aesthetic
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FFCC).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF00FFCC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CamCall Hub Permissions",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To take snapshots and talk inside the app simultaneously, we require Camera and Phone State accesses.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Checkbox indicators
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionStatusCard(
                    title = "Camera Hardware Access",
                    desc = "Used to render active lens streams and take digital snapshots.",
                    isGranted = isCameraGranted,
                    icon = Icons.Default.CameraAlt
                )

                PermissionStatusCard(
                    title = "Phone Receiver Access",
                    desc = "Used to detect incoming calls so you can pickup and talk directly inside the app.",
                    isGranted = isPhoneGranted,
                    icon = Icons.Default.Phone
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Trigger Request button
            Button(
                onClick = onRequestAccess,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FFCC),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "GRANT ACCESS",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PermissionStatusCard(
    title: String,
    desc: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isGranted) Color(0x5500FFCC) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.6f)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Pending,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.3f)
        )
    }
}
