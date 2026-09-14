package com.bodymeasure.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<Measurement?>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getById(id: Long): Measurement?

    /** One-shot read of everything, for export. */
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Measurement>

    /** Used to skip entries already present when importing. */
    @Query("SELECT timestamp FROM measurements")
    suspend fun allTimestamps(): List<Long>

    /** Every referenced photo, for cleaning up files no entry points at. */
    @Query("SELECT photoFileName FROM measurements WHERE photoFileName IS NOT NULL")
    suspend fun allPhotoNames(): List<String>

    @Insert
    suspend fun insertAll(measurements: List<Measurement>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: Measurement): Long

    @Update
    suspend fun update(measurement: Measurement)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
