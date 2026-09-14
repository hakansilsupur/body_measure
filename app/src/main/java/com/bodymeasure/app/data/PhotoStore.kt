package com.bodymeasure.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Stores progress photos in app-internal storage.
 *
 * Photos picked from the gallery are copied in rather than referenced by their
 * original URI: those URIs are permission-scoped and go stale once the grant is
 * released or the source file moves, which would leave entries pointing at
 * images that can no longer be opened.
 */
object PhotoStore {

    private const val DIR = "photos"

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun newFileName(): String = "photo_${UUID.randomUUID()}.jpg"

    /** Copies [source] into internal storage. Returns the stored name, or null on failure. */
    fun importFrom(context: Context, source: Uri): String? = runCatching {
        val name = newFileName()
        val target = file(context, name)
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        if (target.length() == 0L) {
            target.delete()
            return null
        }
        name
    }.getOrNull()

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { file(context, name).delete() }
    }

    fun exists(context: Context, name: String?): Boolean =
        !name.isNullOrBlank() && file(context, name).exists()

    /**
     * Removes photo files no longer referenced by any entry — images left behind
     * by a cancelled edit or an abandoned draft. Safe only at startup, before any
     * draft can hold a picked-but-unsaved photo.
     */
    fun deleteOrphans(context: Context, referenced: Set<String>) {
        runCatching {
            dir(context).listFiles()?.forEach { f ->
                if (f.name !in referenced) f.delete()
            }
        }
    }
}
