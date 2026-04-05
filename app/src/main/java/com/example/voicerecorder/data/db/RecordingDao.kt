package com.example.voicerecorder.data.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity)

    @Update
    suspend fun update(recording: RecordingEntity)

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeById(id: String): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: String): RecordingEntity?

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT r.* FROM recordings r
        INNER JOIN recordings_fts fts ON r.rowid = fts.rowid
        WHERE recordings_fts MATCH :query
        ORDER BY r.createdAt DESC
    """)
    fun search(query: String): Flow<List<RecordingEntity>>

    @Query("""
        UPDATE recordings
        SET batchTranscript = :transcript,
            transcriptionStatus = :status,
            language = :language
        WHERE id = :id
    """)
    suspend fun updateBatchResult(
        id: String,
        transcript: String,
        language: String?,
        status: String = TranscriptionStatus.DONE.name
    )

    @Query("""
        UPDATE recordings
        SET liveTranscript = :transcript,
            transcriptionStatus = :status
        WHERE id = :id
    """)
    suspend fun updateLiveTranscript(id: String, transcript: String, status: String)

    @Query("""
        UPDATE recordings
        SET gladiaBatchId = :batchId,
            transcriptionStatus = :status,
            fileSizeBytes = :sizeBytes,
            durationMs = :durationMs
        WHERE id = :id
    """)
    suspend fun updateAfterStop(
        id: String,
        batchId: String,
        sizeBytes: Long,
        durationMs: Long,
        status: String
    )

    @Query("""
        UPDATE recordings
        SET transcriptionStatus = :status
        WHERE id = :id
    """)
    suspend fun updateStatus(id: String, status: String)

    @RawQuery
    suspend fun rebuildFts(query: SupportSQLiteQuery): Int
}
