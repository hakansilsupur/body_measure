package com.bodymeasure.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Measurement::class], version = 2, exportSchema = false)
abstract class MeasurementDatabase : RoomDatabase() {

    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile private var instance: MeasurementDatabase? = null

        fun get(context: Context): MeasurementDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeasurementDatabase::class.java,
                    "body_measure.db"
                )
                    // Pre-release: schema additions (sex, bodyFatPct) wipe the DB.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
