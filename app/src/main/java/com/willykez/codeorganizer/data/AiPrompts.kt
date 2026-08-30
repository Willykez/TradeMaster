package com.willykez.codeorganizer.data

import com.willykez.codeorganizer.model.ParsedProject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object AiPrompts {
    val SYSTEM = """
        You turn a pasted dump of Android/Kotlin (or any) project source code into a
        precise file listing so it can be written to disk.

        The input may look like any of these, or a mix:
        - Code with "// path/to/File.kt"-style header comments above each file's content
        - A block-separated dump using "// ====" divider lines with a path comment inside
        - An ASCII directory tree (e.g. using ├──, └──, │ characters) followed by, or mixed
          with, the actual file contents
        - Partially broken / inconsistent structure (missing separators, wrong nesting,
          duplicate paths, stray text) — infer and repair the most sensible structure
        - A plain description of files with contents but no explicit markers — infer paths
          from package declarations, class names, and context

        Rules:
        - First, determine ONE project root folder name. Prefer, in order: the label at
          the top of an ASCII tree (e.g. "ForexAcademy/"), a "rootProject.name" value
          found in a settings.gradle(.kts) in the paste, an applicationId/namespace's
          last segment, or — only if nothing in the input suggests a name — "project".
          Use only safe folder-name characters (letters, digits, dot, dash, underscore).
        - Every file path is relative to THAT root and must NOT repeat the root folder
          name itself — e.g. if the root is "ForexAcademy", a path is
          "app/build.gradle.kts", never "ForexAcademy/app/build.gradle.kts". The root is
          returned once, separately, as "projectName" — see the response shape below.
        - Output ONLY the file contents that were actually present in the input. Never
          invent, summarize, or omit code. Preserve it exactly (including comments,
          whitespace-sensitive syntax, and blank lines) except for stripping the outer
          "// path" header comment / divider lines / tree diagram that are structural
          markup rather than real file content.
        - Every path must be relative (no leading slash, no drive letters), forward-slash
          separated, and include the file name with extension.
        - If the same path appears more than once, keep only the most complete /
          most recent version.
        - If a fragment clearly belongs inside a file but has no explicit path, infer the
          most likely path from surrounding context (package name + class name for
          Kotlin/Java, etc.). If you truly cannot determine a path for some content,
          omit that content rather than guessing wildly.
        - Do not include build output, .gitignore-style boilerplate, or commentary that
          isn't part of a real source file, unless it was clearly meant to be written as
          its own file (e.g. an actual .gitignore).

        Respond with ONLY minified JSON, nothing else — no markdown fences, no preamble,
        no explanation. Exact shape:
        {"projectName":"RootFolderName","files":[{"path":"relative/path/File.ext","content":"full file content"}]}
    """.trimIndent()
}

/** Every provider eventually hands us one big text blob; this turns it into a ParsedProject. */
object AiJson {
    val json = Json { ignoreUnknownKeys = true }

    fun parseFiles(rawText: String): ParsedProject {
        val cleaned = stripCodeFences(rawText)
        return json.decodeFromString(ParsedProject.serializer(), cleaned)
    }

    private fun stripCodeFences(text: String): String {
        var t = text.trim()
        if (t.startsWith("```")) {
            t = t.substringAfter("\n")
            if (t.endsWith("```")) t = t.substringBeforeLast("```")
        }
        return t.trim()
    }
}

/** Wraps the parse step so every client reports the same kind of failure message. */
fun runCatchingParse(rawText: String?): AiResult {
    if (rawText.isNullOrBlank()) return AiResult.Failure("Empty response from API.")
    return try {
        val project = AiJson.parseFiles(rawText)
        if (project.files.isEmpty()) {
            AiResult.Failure("The model didn't find any files in that paste.")
        } else {
            AiResult.Success(project.copy(projectName = sanitizeProjectName(project.projectName)))
        }
    } catch (e: SerializationException) {
        AiResult.Failure("Couldn't parse the model's response as JSON: ${e.message}")
    }
}

/**
 * Belt-and-suspenders: never trust a model-provided folder name blindly. Strips
 * anything that isn't a safe filename character and falls back to "project" if that
 * leaves nothing usable, so a blank/weird name from the model can never turn into a
 * write straight into the picked root folder (which the old un-rooted behavior did).
 */
private fun sanitizeProjectName(raw: String): String {
    val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_', '.', '-')
    return cleaned.ifBlank { "project" }
}
