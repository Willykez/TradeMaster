package com.trademaster.pro.data.remote

import com.trademaster.pro.data.model.*

// Deliberately manual (not Firestore's automatic POJO reflection). Automatic
// mapping is convenient right up until an enum, a nested list of data
// classes, or a Kotlin default value does something the reflection mapper
// doesn't expect -- and that failure shows up silently as a missing field at
// runtime, not a compile error. Writing the two directions out explicitly
// means every field is accounted for and the compiler catches drift if a
// model changes.

fun SignalEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "pair" to pair, "type" to type.name, "entry" to entry, "tp" to tp, "sl" to sl,
    "status" to status.name, "pips" to pips, "notes" to notes, "createdAt" to createdAt
)

fun Map<String, Any?>.toSignalEntity(): SignalEntity? = try {
    SignalEntity(
        id = this["id"] as String,
        pair = this["pair"] as String,
        type = SignalType.valueOf(this["type"] as String),
        entry = this["entry"] as String,
        tp = this["tp"] as String,
        sl = this["sl"] as String,
        status = SignalStatus.valueOf(this["status"] as String),
        pips = this["pips"] as String,
        notes = this["notes"] as? String ?: "",
        createdAt = (this["createdAt"] as Number).toLong()
    )
} catch (e: Exception) { null }

fun PostEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "author" to author, "avatar" to avatar, "text" to text, "tags" to tags,
    "likes" to likes, "liked" to liked, "comments" to comments, "pinned" to pinned, "createdAt" to createdAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toPostEntity(): PostEntity? = try {
    PostEntity(
        id = this["id"] as String,
        author = this["author"] as String,
        avatar = this["avatar"] as? String ?: "",
        text = this["text"] as String,
        tags = (this["tags"] as? List<String>) ?: emptyList(),
        likes = (this["likes"] as Number).toInt(),
        liked = this["liked"] as? Boolean ?: false,
        comments = (this["comments"] as Number).toInt(),
        pinned = this["pinned"] as? Boolean ?: false,
        createdAt = (this["createdAt"] as Number).toLong()
    )
} catch (e: Exception) { null }

fun PollEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "question" to question,
    "options" to options.map { mapOf("label" to it.label, "votes" to it.votes) },
    "active" to active, "userVoted" to userVoted, "createdAt" to createdAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toPollEntity(): PollEntity? = try {
    val rawOptions = this["options"] as? List<Map<String, Any?>> ?: emptyList()
    PollEntity(
        id = this["id"] as String,
        question = this["question"] as String,
        options = rawOptions.map { PollOption(it["label"] as String, (it["votes"] as Number).toInt()) },
        active = this["active"] as? Boolean ?: true,
        userVoted = this["userVoted"] as? Boolean ?: false,
        createdAt = (this["createdAt"] as Number).toLong()
    )
} catch (e: Exception) { null }

fun QaEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "question" to question, "answer" to answer, "votes" to votes, "voted" to voted, "createdAt" to createdAt
)

fun Map<String, Any?>.toQaEntity(): QaEntity? = try {
    QaEntity(
        id = this["id"] as String,
        question = this["question"] as String,
        answer = this["answer"] as? String ?: "",
        votes = (this["votes"] as Number).toInt(),
        voted = this["voted"] as? Boolean ?: false,
        createdAt = (this["createdAt"] as Number).toLong()
    )
} catch (e: Exception) { null }

fun CourseEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "title" to title, "desc" to desc, "duration" to duration, "lessons" to lessons,
    "type" to type.name, "category" to category.name, "enrolled" to enrolled, "createdAt" to createdAt
)

fun Map<String, Any?>.toCourseEntity(): CourseEntity? = try {
    CourseEntity(
        id = this["id"] as String,
        title = this["title"] as String,
        desc = this["desc"] as String,
        duration = this["duration"] as? String ?: "N/A",
        lessons = (this["lessons"] as Number).toInt(),
        type = CourseType.valueOf(this["type"] as String),
        category = CourseCategory.valueOf(this["category"] as String),
        enrolled = (this["enrolled"] as Number).toInt(),
        createdAt = (this["createdAt"] as Number).toLong()
    )
} catch (e: Exception) { null }

fun MediaEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "type" to type.name, "sizeLabel" to sizeLabel,
    "dateLabel" to dateLabel, "downloads" to downloads, "createdAt" to createdAt
)

fun Map<String, Any?>.toMediaEntity(): MediaEntity? = try {
    MediaEntity(
        id = this["id"] as String,
        name = this["name"] as String,
        type = MediaType.valueOf(this["type"] as String),
        sizeLabel = this["sizeLabel"] as? String ?: "--",
        dateLabel = this["dateLabel"] as? String ?: "",
        downloads = (this["downloads"] as Number).toInt(),
        createdAt = (this["createdAt"] as Number).toLong()
    )
} catch (e: Exception) { null }
