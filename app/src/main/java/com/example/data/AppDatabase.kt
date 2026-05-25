package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "captured_photos")
data class CapturedPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "Photo",
    val label: String = ""
)

@Entity(tableName = "call_history")
data class CallHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val status: String // "INCOMING_ANSWERED", "INCOMING_MISSED", "OUTGOING"
)

// --- DAOs ---

@Dao
interface PhotoDao {
    @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
    fun getAllPhotosFlow(): Flow<List<CapturedPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: CapturedPhoto)

    @Delete
    suspend fun deletePhoto(photo: CapturedPhoto)

    @Query("DELETE FROM captured_photos")
    suspend fun clearAllPhotos()
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getAllCallHistoryFlow(): Flow<List<CallHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallHistory(history: CallHistory)

    @Query("DELETE FROM call_history WHERE id = :id")
    suspend fun deleteCallHistoryById(id: Int)

    @Query("DELETE FROM call_history")
    suspend fun clearAllHistory()
}

// --- Database Holder ---

@Database(entities = [CapturedPhoto::class, CallHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun callDao(): CallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "camcall_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
