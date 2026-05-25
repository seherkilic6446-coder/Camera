package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val database: AppDatabase) {
    val photoDao = database.photoDao()
    val callDao = database.callDao()

    // --- Photo Operations ---
    val allPhotos: Flow<List<CapturedPhoto>> = photoDao.getAllPhotosFlow()

    suspend fun savePhoto(filePath: String, mode: String, label: String = "") {
        val photo = CapturedPhoto(
            filePath = filePath,
            mode = mode,
            label = label
        )
        photoDao.insertPhoto(photo)
    }

    suspend fun deletePhoto(photo: CapturedPhoto) {
        photoDao.deletePhoto(photo)
    }

    suspend fun clearPhotos() {
        photoDao.clearAllPhotos()
    }

    // --- Call History Operations ---
    val allCallLogs: Flow<List<CallHistory>> = callDao.getAllCallHistoryFlow()

    suspend fun logCall(callerName: String, phoneNumber: String, durationSeconds: Int, status: String) {
        val call = CallHistory(
            callerName = callerName,
            phoneNumber = phoneNumber,
            durationSeconds = durationSeconds,
            status = status
        )
        callDao.insertCallHistory(call)
    }

    suspend fun deleteCallLog(id: Int) {
        callDao.deleteCallHistoryById(id)
    }

    suspend fun clearCallLogs() {
        callDao.clearAllHistory()
    }
}
