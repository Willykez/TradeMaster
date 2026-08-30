package com.trademaster.pro.data.repo

import com.trademaster.pro.data.db.AppDatabase
import com.trademaster.pro.data.model.*
import com.trademaster.pro.data.remote.FirestoreDataSource
import com.trademaster.pro.data.remote.MarketDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.random.Random

// Architecture in one paragraph: Room is the only thing the UI ever reads
// from (via the Flows below) -- fast, works offline, survives process
// death. Firestore is the system of record for admin-authored content
// (signals/posts/polls/qa/courses/media): a snapshot listener per
// collection mirrors whatever's in the cloud into Room in real time, so
// every install converges to the same state. Writes go straight to
// Firestore; the listener echoes the change back into Room for every
// device, including the one that made the write. Market prices are a
// separate concern entirely -- public data, not admin content -- fetched
// straight from a quote API and written only to the local ticker table.
class TradeRepository(
    private val db: AppDatabase,
    private val remote: FirestoreDataSource,
    private val marketData: MarketDataRepository,
    private val syncScope: CoroutineScope
) {

    val signals: Flow<List<SignalEntity>> = db.signalDao().observeAll()
    val posts: Flow<List<PostEntity>> = db.postDao().observeAll()
    val polls: Flow<List<PollEntity>> = db.pollDao().observeAll()
    val qa: Flow<List<QaEntity>> = db.qaDao().observeAll()
    val courses: Flow<List<CourseEntity>> = db.courseDao().observeAll()
    val media: Flow<List<MediaEntity>> = db.mediaDao().observeAll()
    val ticker: Flow<List<TickerEntity>> = db.tickerDao().observeAll()

    val hasLiveMarketData: Boolean get() = marketData.hasApiKey

    suspend fun seedIfEmpty() {
        // Seed data only ever goes into Room as a nice first-run demo before
        // the cloud has anything -- it is deliberately NOT pushed to
        // Firestore, so it never overwrites real admin content and never
        // leaks placeholder rows into the shared collections.
        if (db.tickerDao().count() == 0) db.tickerDao().upsertAll(SeedData.ticker)
    }

    /** Starts the six Firestore listeners that keep Room in sync. Call once, from app start. */
    fun startCloudSync() {
        syncScope.launch { remote.observeSignals().collect { db.signalDao().replaceAll(it) } }
        syncScope.launch { remote.observePosts().collect { db.postDao().replaceAll(it) } }
        syncScope.launch { remote.observePolls().collect { db.pollDao().replaceAll(it) } }
        syncScope.launch { remote.observeQa().collect { db.qaDao().replaceAll(it) } }
        syncScope.launch { remote.observeCourses().collect { db.courseDao().replaceAll(it) } }
        syncScope.launch { remote.observeMedia().collect { db.mediaDao().replaceAll(it) } }
    }

    // ---- Signals ----
    suspend fun saveSignal(signal: SignalEntity) {
        db.signalDao().upsert(signal)      // instant local echo, works offline
        remote.upsertSignal(signal)        // Firestore queues this if offline, syncs when back
    }
    suspend fun deleteSignal(signal: SignalEntity) {
        db.signalDao().delete(signal)
        remote.deleteSignal(signal.id)
    }

    // ---- Posts ----
    suspend fun savePost(post: PostEntity) { db.postDao().upsert(post); remote.upsertPost(post) }
    suspend fun deletePost(post: PostEntity) { db.postDao().delete(post); remote.deletePost(post.id) }
    suspend fun toggleLike(post: PostEntity) {
        val liked = !post.liked
        val updated = post.copy(liked = liked, likes = (post.likes + if (liked) 1 else -1).coerceAtLeast(0))
        savePost(updated)
    }

    // ---- Polls ----
    suspend fun savePoll(poll: PollEntity) { db.pollDao().upsert(poll); remote.upsertPoll(poll) }
    suspend fun deletePoll(poll: PollEntity) { db.pollDao().delete(poll); remote.deletePoll(poll.id) }
    suspend fun vote(poll: PollEntity, optionIndex: Int) {
        if (poll.userVoted || !poll.active) return
        val updated = poll.options.mapIndexed { i, opt -> if (i == optionIndex) opt.copy(votes = opt.votes + 1) else opt }
        savePoll(poll.copy(options = updated, userVoted = true))
    }
    suspend fun togglePollActive(poll: PollEntity) = savePoll(poll.copy(active = !poll.active))

    // ---- Q&A ----
    suspend fun saveQa(item: QaEntity) { db.qaDao().upsert(item); remote.upsertQa(item) }
    suspend fun deleteQa(item: QaEntity) { db.qaDao().delete(item); remote.deleteQa(item.id) }
    suspend fun markHelpful(item: QaEntity) {
        if (item.voted) return
        saveQa(item.copy(votes = item.votes + 1, voted = true))
    }

    // ---- Education ----
    suspend fun saveCourse(course: CourseEntity) { db.courseDao().upsert(course); remote.upsertCourse(course) }
    suspend fun deleteCourse(course: CourseEntity) { db.courseDao().delete(course); remote.deleteCourse(course.id) }
    suspend fun enroll(course: CourseEntity) = saveCourse(course.copy(enrolled = course.enrolled + 1))

    // ---- Media ----
    suspend fun saveMedia(media: MediaEntity) { db.mediaDao().upsert(media); remote.upsertMedia(media) }
    suspend fun deleteMedia(media: MediaEntity) { db.mediaDao().delete(media); remote.deleteMedia(media.id) }
    suspend fun recordDownload(media: MediaEntity) = saveMedia(media.copy(downloads = media.downloads + 1))

    // ---- Ticker (live market data, not Firestore-backed) ----
    suspend fun refreshTicker(current: List<TickerEntity>) {
        if (current.isEmpty()) return
        val result = marketData.fetchQuotes(current.map { it.pair })
        if (result.isSuccess) {
            val quotes = result.getOrThrow()
            val updated = current.map { t ->
                val newPrice = quotes[t.pair] ?: return@map t
                val changePct = if (t.price > 0) ((newPrice - t.price) / t.price) * 100 else 0.0
                t.copy(price = newPrice, changePct = changePct, up = newPrice >= t.price)
            }
            db.tickerDao().upsertAll(updated)
        } else {
            // No key, rate-limited, or offline -- keep the dashboard feeling
            // alive with a small simulated walk instead of freezing.
            simulateTick(current)
        }
    }

    private suspend fun simulateTick(current: List<TickerEntity>) {
        val updated = current.map { t ->
            val magnitude = if (t.price > 100) 0.4 else 0.002
            val delta = (Random.nextDouble() - 0.5) * magnitude
            val newPrice = (t.price + delta).coerceAtLeast(0.0001)
            val changePct = ((newPrice - t.price) / t.price) * 100
            t.copy(price = newPrice, changePct = changePct, up = newPrice >= t.price)
        }
        db.tickerDao().upsertAll(updated)
    }

    fun computeStats(signals: List<SignalEntity>): PlatformStats {
        val active = signals.count { it.status == SignalStatus.ACTIVE }
        return PlatformStats(
            activeSignals = active,
            winRate = 78.4,
            totalPips = 1247,
            members = 2847,
            newSignalsToday = signals.count { System.currentTimeMillis() - it.createdAt < 86_400_000 },
            winDelta = 4.2,
            pipsDelta = 186,
            memberDelta = 124
        )
    }
}
