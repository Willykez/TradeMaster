package com.willykez.codeorganizer.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.willykez.codeorganizer.model.LogLine
import com.willykez.codeorganizer.model.ParsedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

/**
 * Turns a flat list of (path, content) into real directories/files under a
 * user-picked SAF tree — creating what's missing and overwriting what's there,
 * which is what makes it able to "fix" an already-broken structure.
 *
 * Every write is guaranteed to land inside its own picked-folder/<projectName>/
 * subfolder rather than loose in whatever folder the user picked — see writeProject.
 */
class FileOrganizer(private val context: Context) {

    /**
     * mimeType is deliberately generic. Standard local-storage document providers don't
     * append an extra extension to the filename when the mime type is
     * "application/octet-stream" — using a text/* mime for an unrecognized extension
     * like .kt or .gradle.kts can otherwise cause some providers to tack on ".txt".
     */
    private val genericMime = "application/octet-stream"

    suspend fun writeProject(
        treeUri: Uri,
        projectName: String,
        files: List<ParsedFile>
    ): List<LogLine> = withContext(Dispatchers.IO) {
        val log = mutableListOf<LogLine>()
        val pickedRoot = DocumentFile.fromTreeUri(context, treeUri)
        if (pickedRoot == null || !pickedRoot.isDirectory) {
            log.add(LogLine("Selected folder is not accessible. Pick it again in Settings.", isError = true))
            return@withContext log
        }

        // Every write lands inside picked-folder/<projectName>/... — never loose in the
        // picked folder itself — regardless of whether individual file paths happen to
        // repeat the project name.
        val existingProjectDir = pickedRoot.findFile(projectName)
        val projectDir = if (existingProjectDir != null && existingProjectDir.isDirectory) {
            existingProjectDir
        } else {
            existingProjectDir?.delete() // stray file blocking the project folder
            pickedRoot.createDirectory(projectName)
        }
        if (projectDir == null) {
            log.add(LogLine("Couldn't create project folder '$projectName'.", isError = true))
            return@withContext log
        }
        log.add(LogLine("Project folder: $projectName/"))

        // Cache resolved directories so a project with many files in the same
        // folder doesn't re-walk/re-query the tree for every single file.
        val dirCache = HashMap<String, DocumentFile>()
        dirCache[""] = projectDir

        for (file in files) {
            val cleanPath = file.path.trim().trimStart('/').replace('\\', '/')
            if (cleanPath.isBlank() || cleanPath.contains("..")) {
                log.add(LogLine("Skipped unsafe path: '${file.path}'", isError = true))
                continue
            }

            val segments = cleanPath.split("/").filter { it.isNotBlank() }
                .let { if (it.firstOrNull()?.equals(projectName, ignoreCase = true) == true) it.drop(1) else it }
            if (segments.isEmpty()) {
                log.add(LogLine("Skipped empty path.", isError = true))
                continue
            }
            val fileName = segments.last()
            val dirSegments = segments.dropLast(1)

            try {
                val parentDir = resolveDirectory(dirCache, dirSegments)
                val existing = parentDir.findFile(fileName)
                val target = if (existing != null && existing.isFile) {
                    existing
                } else {
                    existing?.delete() // stray folder/file with the same name blocking us
                    parentDir.createFile(genericMime, fileName)
                }

                if (target == null) {
                    log.add(LogLine("Failed to create $cleanPath", isError = true))
                    continue
                }

                context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                    OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                        writer.write(file.content)
                    }
                } ?: run {
                    log.add(LogLine("Couldn't open output stream for $cleanPath", isError = true))
                    continue
                }

                log.add(LogLine("Wrote $cleanPath"))
            } catch (e: Exception) {
                log.add(LogLine("Error writing $cleanPath: ${e.message}", isError = true))
            }
        }

        log
    }

    /** Walks/creates each directory segment under the cached root, memoizing as it goes. */
    private fun resolveDirectory(
        cache: HashMap<String, DocumentFile>,
        segments: List<String>
    ): DocumentFile {
        var pathKey = ""
        var current = cache[""]!!
        for (segment in segments) {
            pathKey = if (pathKey.isEmpty()) segment else "$pathKey/$segment"
            val cached = cache[pathKey]
            current = if (cached != null) {
                cached
            } else {
                val existingDir = current.findFile(segment)
                val dir = if (existingDir != null && existingDir.isDirectory) {
                    existingDir
                } else {
                    existingDir?.delete() // stray file blocking a needed folder
                    current.createDirectory(segment)
                        ?: throw IllegalStateException("Could not create folder '$segment'")
                }
                cache[pathKey] = dir
                dir
            }
        }
        return current
    }
}
