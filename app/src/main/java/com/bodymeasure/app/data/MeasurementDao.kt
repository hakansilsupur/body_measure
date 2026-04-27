package com.bodymeasure.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<Measurement?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: Measurement): Long

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
