package com.bodymeasure.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialises recorded history to and from a plain JSON document, so it can be
 * saved outside the app and restored after an uninstall or on another phone.
 *
 * The format is intentionally simple and self-describing. [FORMAT_VERSION] is
 * written into every export so a future schema change can migrate old backup
 * files instead of rejecting them.
 */
object MeasurementBackup {

    const val FORMAT_VERSION = 1
    const val MIME_TYPE = "application/json"

    private const val KEY_FORMAT = "formatVersion"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_ENTRIES = "entries"

    class InvalidBackupException(message: String) : Exception(message)

    fun encode(entries: List<Measurement>, exportedAt: Long = System.currentTimeMillis()): String {
        val array = JSONArray()
        entries.forEach { m ->
            array.put(
                JSONObject().apply {
                    put("timestamp", m.timestamp)
                    put("sex", m.sex)
                    putOrNull("ageYears", m.ageYears)
                    put("weightKg", m.weightKg)
                    put("heightCm", m.heightCm)
                    putOrNull("waistCm", m.waistCm)
                    putOrNull("armCm", m.armCm)
                    putOrNull("chestCm", m.chestCm)
                    putOrNull("hipCm", m.hipCm)
                    putOrNull("thighCm", m.thighCm)
                    putOrNull("calfCm", m.calfCm)
                    putOrNull("wristCm", m.wristCm)
                    putOrNull("neckCm", m.neckCm)
                    put("bmi", m.bmi)
                    putOrNull("bodyFatPct", m.bodyFatPct)
                    putOrNull("bmrKcal", m.bmrKcal)
                    putOrNull("activityFactor", m.activityFactor)
                    putOrNull("photoFileName", m.photoFileName)
                }
            )
        }
        val root = JSONObject().apply {
            put(KEY_FORMAT, FORMAT_VERSION)
            put(KEY_EXPORTED_AT, exportedAt)
            put(KEY_ENTRIES, array)
        }
        return root.toString(2)
    }

    /**
     * Parses a backup document. Entries are returned with `id = 0` so the caller
     * can insert them as new rows rather than clobbering existing ids.
     *
     * @throws InvalidBackupException if the file isn't a backup this build understands.
     */
    fun decode(text: String): List<Measurement> {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw InvalidBackupException("Not a valid backup file")
        }

        if (!root.has(KEY_ENTRIES)) {
            throw InvalidBackupException("Not a Body Measure backup file")
        }
        val version = root.optInt(KEY_FORMAT, 0)
        if (version > FORMAT_VERSION) {
            throw InvalidBackupException(
                "This backup was made by a newer version of the app (format $version)"
            )
        }

        val array = root.optJSONArray(KEY_ENTRIES)
            ?: throw InvalidBackupException("Backup file has no entries")

        val out = ArrayList<Measurement>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            // weight/height are required; anything without them can't produce a BMI.
            val weight = o.doubleOrNull("weightKg") ?: continue
            val height = o.doubleOrNull("heightCm") ?: continue
            if (weight <= 0 || height <= 0) continue

            out += Measurement(
                id = 0,
                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                sex = o.optString("sex", "Male").ifBlank { "Male" },
                ageYears = o.intOrNull("ageYears"),
                weightKg = weight,
                heightCm = height,
                waistCm = o.doubleOrNull("waistCm"),
                armCm = o.doubleOrNull("armCm"),
                chestCm = o.doubleOrNull("chestCm"),
                hipCm = o.doubleOrNull("hipCm"),
                thighCm = o.doubleOrNull("thighCm"),
                calfCm = o.doubleOrNull("calfCm"),
                wristCm = o.doubleOrNull("wristCm"),
                neckCm = o.doubleOrNull("neckCm"),
                bmi = o.doubleOrNull("bmi") ?: (weight / ((height / 100.0) * (height / 100.0))),
                bodyFatPct = o.doubleOrNull("bodyFatPct"),
                bmrKcal = o.doubleOrNull("bmrKcal"),
                activityFactor = o.doubleOrNull("activityFactor"),
                // Photo bytes are not in the JSON; the name only resolves if the
                // app's photo directory still holds that file.
                photoFileName = if (o.isNull("photoFileName")) null
                                else o.optString("photoFileName").ifBlank { null }
            )
        }
        if (out.isEmpty()) throw InvalidBackupException("Backup file contains no usable entries")
        return out
    }

    /** Suggested filename, e.g. body-measure-backup-2026-09-14.json */
    fun suggestedFileName(dateStamp: String): String = "body-measure-backup-$dateStamp.json"
}

private fun JSONObject.putOrNull(name: String, value: Any?) {
    if (value == null) put(name, JSONObject.NULL) else put(name, value)
}

private fun JSONObject.doubleOrNull(name: String): Double? =
    if (isNull(name)) null else optDouble(name).takeIf { !it.isNaN() }

private fun JSONObject.intOrNull(name: String): Int? =
    if (isNull(name)) null else if (has(name)) optInt(name) else null
