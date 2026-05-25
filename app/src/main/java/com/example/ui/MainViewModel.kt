package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CallHistory
import com.example.data.CapturedPhoto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface CallState {
    object Idle : CallState
    data class Ringing(val callerName: String, val phoneNumber: String) : CallState
    data class Talking(val callerName: String, val phoneNumber: String, val durationSeconds: Int) : CallState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
    }

    // --- Database Streams ---
    val capturedPhotos: StateFlow<List<CapturedPhoto>> = repository.allPhotos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val callLogs: StateFlow<List<CallHistory>> = repository.allCallLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- UI/Camera States ---
    private val _selectedMode = MutableStateFlow("Photo") // Photo, Video, Night, Portrait
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _isBackCamera = MutableStateFlow(true)
    val isBackCamera: StateFlow<Boolean> = _isBackCamera.asStateFlow()

    private val _flashState = MutableStateFlow("Auto") // Auto, On, Off
    val flashState: StateFlow<String> = _flashState.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<String> = MutableStateFlow("1.0x").asStateFlow() // display text fallback

    private val _selectedPhoto = MutableStateFlow<CapturedPhoto?>(null)
    val selectedPhoto: StateFlow<CapturedPhoto?> = _selectedPhoto.asStateFlow()

    // --- Call States & Simulation ---
    private val _currentCallState = MutableStateFlow<CallState>(CallState.Idle)
    val currentCallState: StateFlow<CallState> = _currentCallState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    // Waveform simulation coefficients
    private val _audioWaveformLevels = MutableStateFlow(List(16) { 0.2f })
    val audioWaveformLevels: StateFlow<List<Float>> = _audioWaveformLevels.asStateFlow()

    private var callTimerJob: Job? = null
    private var waveformJob: Job? = null

    // Setters
    fun setMode(mode: String) {
        _selectedMode.value = mode
    }

    fun toggleCameraFacing() {
        _isBackCamera.value = !_isBackCamera.value
    }

    fun toggleFlash() {
        val next = when (_flashState.value) {
            "Auto" -> "On"
            "On" -> "Off"
            else -> "Auto"
        }
        _flashState.value = next
    }

    fun selectPhoto(photo: CapturedPhoto?) {
        _selectedPhoto.value = photo
    }

    fun deletePhoto(photo: CapturedPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
            // If the deleted photo is selected, clear selection
            if (_selectedPhoto.value?.id == photo.id) {
                _selectedPhoto.value = null
            }
            // Delete the file from local storage as well to preserve storage limit
            try {
                val file = File(photo.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Capture Picture Processing ---
    fun capturePhotoAndSave(context: Context) {
        viewModelScope.launch {
            try {
                // Generate simulated premium image and write to sandboxed file if no hardware Camera can write to it
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFileName = "IMG_${timeStamp}.jpg"
                val storageDir = context.getExternalFilesDir("Captured") ?: context.filesDir
                val imageFile = File(storageDir, imageFileName)

                // Write dummy or real premium looking canvas data to show high fidelity
                val out = FileOutputStream(imageFile)
                val dummyBmp = createMockBitmapData(timeStamp)
                out.write(dummyBmp)
                out.flush()
                out.close()

                repository.savePhoto(
                    filePath = imageFile.absolutePath,
                    mode = _selectedMode.value,
                    label = "Beautiful detail in ${_selectedMode.value} mode"
                )

                Toast.makeText(context, "Captured: $imageFileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Capture error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Provide preloaded dummy bytes representation so image loaders can display it elegantly
    private fun createMockBitmapData(timeStamp: String): ByteArray {
        // Just structured placeholder file headers or colors, coil-loader will display placeholder fallback
        // or we will use beautiful gradient shapes inside our Composable to display existing captured content!
        return ByteArray(128)
    }

    // --- Call Processing Functions ---

    /**
     * Instantly triggers an Incoming Call transition
     */
    fun simulateIncomingCall(callerName: String, phoneNumber: String) {
        // Cancel existing call timers/jobs if any
        endActiveCallTimers()
        _currentCallState.value = CallState.Ringing(callerName, phoneNumber)
    }

    /**
     * Schedules an Incoming Call to arrive in a designated delay
     */
    fun scheduleIncomingCall(callerName: String, phoneNumber: String, delaySeconds: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            simulateIncomingCall(callerName, phoneNumber)
        }, delaySeconds * 1000L)
    }

    /**
     * Receives/notifies from real TelephonyManager incoming phone state
     */
    fun notifyRealIncomingCall(number: String) {
        val callerName = when (number) {
            "Unknown", "" -> "Incoming Caller"
            else -> "Call via Phone State"
        }
        simulateIncomingCall(callerName, number)
    }

    /**
     * Simulates placing an Outgoing Call (Talk directly inside the app)
     */
    fun placeOutgoingCall(callerName: String, phoneNumber: String) {
        endActiveCallTimers()
        answerCall(callerName, phoneNumber)
    }

    /**
     * Answers/Picks Up an Incoming Call and begins active conversation state
     */
    fun answerCall(callerName: String, phoneNumber: String) {
        _currentCallState.value = CallState.Talking(callerName, phoneNumber, 0)
        _isMuted.value = false
        _isSpeakerOn.value = false
        startCallTimers(callerName, phoneNumber)
    }

    /**
     * Rejects or Ends an Active Call
     */
    fun hangUpCall() {
        val lastState = _currentCallState.value
        endActiveCallTimers()
        _currentCallState.value = CallState.Idle

        if (lastState is CallState.Talking) {
            // Save to call logs with duration
            viewModelScope.launch {
                repository.logCall(
                    callerName = lastState.callerName,
                    phoneNumber = lastState.phoneNumber,
                    durationSeconds = lastState.durationSeconds,
                    status = "INCOMING_ANSWERED"
                )
            }
        } else if (lastState is CallState.Ringing) {
            // Log as missed
            viewModelScope.launch {
                repository.logCall(
                    callerName = lastState.callerName,
                    phoneNumber = lastState.phoneNumber,
                    durationSeconds = 0,
                    status = "INCOMING_MISSED"
                )
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    private fun startCallTimers(callerName: String, phoneNumber: String) {
        // Job tracking timer values in Talking State
        callTimerJob = viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(1000L)
                counter++
                _currentCallState.value = CallState.Talking(callerName, phoneNumber, counter)
            }
        }

        // Animated sound waves visualizer job
        waveformJob = viewModelScope.launch {
            while (true) {
                delay(150L)
                // Randomly generate visual equalizer values
                _audioWaveformLevels.value = List(16) {
                    val base = if (_isMuted.value) 0.05f else 0.15f
                    val multiplier = if (_isMuted.value) 0.05f else 0.85f
                    base + (Math.random().toFloat() * multiplier)
                }
            }
        }
    }

    private fun endActiveCallTimers() {
        callTimerJob?.cancel()
        callTimerJob = null
        waveformJob?.cancel()
        waveformJob = null
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        endActiveCallTimers()
    }
}
