package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.PersonalRecord
import com.example.data.model.TrainingGoal
import com.example.data.model.WorkoutLog
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "local_workouts")
data class LocalWorkoutEntity(
    @PrimaryKey val id: String,
    val title: String,
    val distanceKm: Double,
    val durationSeconds: Long,
    val avgPaceSecondsPerKm: Long,
    val avgHeartRate: Long,
    val cadenceSpm: Long,
    val elevationGainM: Long,
    val splitsSummary: String,
    val aiCoachingInsight: String,
    val sourceType: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): WorkoutLog = WorkoutLog(
        id = id,
        userId = "offline_local_user",
        title = title,
        distanceKm = distanceKm,
        durationSeconds = durationSeconds,
        avgPaceSecondsPerKm = avgPaceSecondsPerKm,
        avgHeartRate = avgHeartRate,
        cadenceSpm = cadenceSpm,
        elevationGainM = elevationGainM,
        splitsSummary = splitsSummary,
        aiCoachingInsight = aiCoachingInsight,
        sourceType = sourceType,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(createdAtEpochMs))
    )
}

@Entity(tableName = "local_personal_records")
data class LocalPrEntity(
    @PrimaryKey val id: String,
    val distanceCategory: String,
    val distanceMeters: Long,
    val bestTimeSeconds: Long,
    val previousTimeSeconds: Long,
    val paceSecondsPerKm: Long,
    val workoutTitle: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): PersonalRecord = PersonalRecord(
        id = id,
        userId = "offline_local_user",
        distanceCategory = distanceCategory,
        distanceMeters = distanceMeters,
        bestTimeSeconds = bestTimeSeconds,
        previousTimeSeconds = previousTimeSeconds,
        paceSecondsPerKm = paceSecondsPerKm,
        workoutTitle = workoutTitle,
        createdAt = Timestamp(Date(updatedAtEpochMs)),
        updatedAt = Timestamp(Date(updatedAtEpochMs))
    )
}

@Entity(tableName = "local_goals")
data class LocalGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetDistanceCategory: String,
    val targetTimeSeconds: Long,
    val weeklyMileageTargetKm: Long,
    val reminderEnabled: Boolean,
    val reminderHour: Long,
    val reminderMinute: Long,
    val completed: Boolean,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): TrainingGoal = TrainingGoal(
        id = id,
        userId = "offline_local_user",
        title = title,
        targetDistanceCategory = targetDistanceCategory,
        targetTimeSeconds = targetTimeSeconds,
        weeklyMileageTargetKm = weeklyMileageTargetKm,
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        completed = completed,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(createdAtEpochMs))
    )
}

@Entity(tableName = "local_backups")
data class LocalBackupEntity(
    @PrimaryKey val id: String,
    val backupLabel: String,
    val workoutCount: Long,
    val prCount: Long,
    val goalCount: Long,
    val summaryNotes: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): CloudBackupSnapshot = CloudBackupSnapshot(
        id = id,
        userId = "offline_local_user",
        backupLabel = backupLabel,
        workoutCount = workoutCount,
        prCount = prCount,
        goalCount = goalCount,
        summaryNotes = summaryNotes,
        createdAt = Timestamp(Date(createdAtEpochMs)),
        updatedAt = Timestamp(Date(createdAtEpochMs))
    )
}

@Dao
interface LocalTrainingDao {
    @Query("SELECT * FROM local_workouts ORDER BY createdAtEpochMs DESC")
    fun observeWorkouts(): Flow<List<LocalWorkoutEntity>>

    @Query("SELECT COUNT(*) FROM local_workouts")
    suspend fun getWorkoutCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: LocalWorkoutEntity)

    @Query("DELETE FROM local_workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)

    @Query("SELECT * FROM local_personal_records ORDER BY distanceMeters ASC")
    fun observePersonalRecords(): Flow<List<LocalPrEntity>>

    @Query("SELECT * FROM local_personal_records WHERE id = :id LIMIT 1")
    suspend fun getPersonalRecordById(id: String): LocalPrEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(pr: LocalPrEntity)

    @Query("SELECT * FROM local_goals ORDER BY createdAtEpochMs DESC")
    fun observeGoals(): Flow<List<LocalGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: LocalGoalEntity)

    @Query("DELETE FROM local_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: String)

    @Query("SELECT * FROM local_backups ORDER BY createdAtEpochMs DESC")
    fun observeBackups(): Flow<List<LocalBackupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: LocalBackupEntity)
}

@Database(
    entities = [
        LocalWorkoutEntity::class,
        LocalPrEntity::class,
        LocalGoalEntity::class,
        LocalBackupEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StrideApexLocalDatabase : RoomDatabase() {
    abstract fun localTrainingDao(): LocalTrainingDao

    companion object {
        @Volatile
        private var INSTANCE: StrideApexLocalDatabase? = null

        fun getInstance(context: Context): StrideApexLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrideApexLocalDatabase::class.java,
                    "strideapex_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
