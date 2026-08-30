package com.trademaster.pro.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

// IDs are client-generated UUID strings (not Room autoGenerate longs) on
// purpose: the same ID has to mean the same document in both Room (local
// cache) and Firestore (source of truth for shared content). Generating it
// client-side means a new signal/post/etc. is usable immediately -- offline
// or online -- with no round trip needed to learn its own ID.
private fun newId() = UUID.randomUUID().toString()

enum class SignalType { BUY, SELL, PENDING }
enum class SignalStatus { ACTIVE, PENDING, CLOSED }

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey val id: String = newId(),
    val pair: String,
    val type: SignalType,
    val entry: String,
    val tp: String,
    val sl: String,
    val status: SignalStatus,
    val pips: String,
    val notes: String,
    val createdAt: Long
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String = newId(),
    val author: String,
    val avatar: String,
    val text: String,
    val tags: List<String>,
    val likes: Int,
    val liked: Boolean,
    val comments: Int,
    val pinned: Boolean,
    val createdAt: Long
)

data class PollOption(
    val label: String,
    val votes: Int
)

@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey val id: String = newId(),
    val question: String,
    val options: List<PollOption>,
    val active: Boolean,
    val userVoted: Boolean,
    val createdAt: Long
) {
    @Ignore
    val total: Int = options.sumOf { it.votes }
}

@Entity(tableName = "qa")
data class QaEntity(
    @PrimaryKey val id: String = newId(),
    val question: String,
    val answer: String,
    val votes: Int,
    val voted: Boolean,
    val createdAt: Long
)

enum class CourseType { VIDEO, PDF }
enum class CourseCategory { BEGINNER, INTERMEDIATE, ADVANCED }

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String = newId(),
    val title: String,
    val desc: String,
    val duration: String,
    val lessons: Int,
    val type: CourseType,
    val category: CourseCategory,
    val enrolled: Int,
    val createdAt: Long
)

enum class MediaType { IMAGE, VIDEO, PDF, FILE }

@Entity(tableName = "media_files")
data class MediaEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val type: MediaType,
    val sizeLabel: String,
    val dateLabel: String,
    val downloads: Int,
    val createdAt: Long
)

// Not synced through Firestore -- this is public market data, refreshed
// from the live quote API (see MarketDataRepository), not admin content.
@Entity(tableName = "ticker")
data class TickerEntity(
    @PrimaryKey val pair: String,
    val price: Double,
    val changePct: Double,
    val up: Boolean
)

data class PlatformStats(
    val activeSignals: Int,
    val winRate: Double,
    val totalPips: Int,
    val members: Int,
    val newSignalsToday: Int,
    val winDelta: Double,
    val pipsDelta: Int,
    val memberDelta: Int
)
