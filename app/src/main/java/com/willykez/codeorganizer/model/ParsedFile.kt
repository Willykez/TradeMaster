package com.willykez.codeorganizer.model

import kotlinx.serialization.Serializable

/**
 * One file the AI extracted from the pasted source dump.
 * [path] is always relative (no leading slash), forward-slash separated,
 * e.g. "app/src/main/java/com/willykez/forexacademy/MainActivity.kt".
 */
@Serializable
data class ParsedFile(
    val path: String,
    val content: String
)

/**
 * The AI is instructed to respond with exactly this shape:
 * { "projectName": "...", "files": [ { "path": "...", "content": "..." }, ... ] }
 *
 * [projectName] is the single root folder everything gets written inside — kept
 * separate from each file's [ParsedFile.path] on purpose, so FileOrganizer can create
 * that one folder itself and nest every file under it deterministically, instead of
 * hoping the AI remembered to prefix every single path with it consistently.
 */
@Serializable
data class ParsedProject(
    val projectName: String = "",
    val files: List<ParsedFile>
)

/** One line of progress/result the UI shows while (or after) writing files. */
data class LogLine(
    val text: String,
    val isError: Boolean = false
)
