package com.bodymeasure.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema history for [MeasurementDatabase].
 *
 * Every version bump must ship a migration here, otherwise Room throws on open
 * for anyone upgrading. Never replace these with destructive fallback — that
 * silently deletes the user's recorded history.
 *
 * v1 -> v2  added sex, bodyFatPct
 * v2 -> v3  added ageYears, bmrKcal
 * v3 -> v4  added activityFactor
 *
 * Note: `sex` is NOT NULL, so SQLite requires a DEFAULT on the ALTER. The
 * entity deliberately does not declare @ColumnInfo(defaultValue = ...), which
 * means Room does not validate the default and databases created fresh from
 * the entity stay compatible with ones migrated up through these steps.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE measurements ADD COLUMN sex TEXT NOT NULL DEFAULT 'Male'")
        db.execSQL("ALTER TABLE measurements ADD COLUMN bodyFatPct REAL")
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE measurements ADD COLUMN ageYears INTEGER")
        db.execSQL("ALTER TABLE measurements ADD COLUMN bmrKcal REAL")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE measurements ADD COLUMN activityFactor REAL")
    }
}

internal val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4
)
