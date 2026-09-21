package com.bodymeasure.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Measurement::class], version = 7, exportSchema = true)
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
                    // Recorded history must survive app updates, so every schema
                    // change ships a real migration (see Migrations.kt). Do not add
                    // fallbackToDestructiveMigration() here — it deletes user data.
                    .addMigrations(*ALL_MIGRATIONS)
                    // A downgrade only happens when sideloading an older build, where
                    // there is no forward schema to migrate into. Rebuilding is the
                    // only safe option in that case.
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
