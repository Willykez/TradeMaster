package com.trademaster.pro.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.trademaster.pro.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private object Collections {
    const val SIGNALS = "signals"
    const val POSTS = "posts"
    const val POLLS = "polls"
    const val QA = "qa"
    const val COURSES = "courses"
    const val MEDIA = "media"
}

// Every user's app opens the same Firestore collections and listens live --
// that's what makes admin content "reach everyone": there's exactly one
// signals collection in the cloud, and every device's Room cache is just a
// local mirror of whatever's in it right now. Firestore's SDK already
// queues writes and replays them when connectivity returns, so this also
// gives us offline support for free rather than something we have to build.
class FirestoreDataSource(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun observeSignals(): Flow<List<SignalEntity>> = observeCollection(Collections.SIGNALS) { it.toSignalEntity() }
    fun observePosts(): Flow<List<PostEntity>> = observeCollection(Collections.POSTS) { it.toPostEntity() }
    fun observePolls(): Flow<List<PollEntity>> = observeCollection(Collections.POLLS) { it.toPollEntity() }
    fun observeQa(): Flow<List<QaEntity>> = observeCollection(Collections.QA) { it.toQaEntity() }
    fun observeCourses(): Flow<List<CourseEntity>> = observeCollection(Collections.COURSES) { it.toCourseEntity() }
    fun observeMedia(): Flow<List<MediaEntity>> = observeCollection(Collections.MEDIA) { it.toMediaEntity() }

    suspend fun upsertSignal(e: SignalEntity) = set(Collections.SIGNALS, e.id, e.toFirestoreMap())
    suspend fun deleteSignal(id: String) = delete(Collections.SIGNALS, id)

    suspend fun upsertPost(e: PostEntity) = set(Collections.POSTS, e.id, e.toFirestoreMap())
    suspend fun deletePost(id: String) = delete(Collections.POSTS, id)

    suspend fun upsertPoll(e: PollEntity) = set(Collections.POLLS, e.id, e.toFirestoreMap())
    suspend fun deletePoll(id: String) = delete(Collections.POLLS, id)

    suspend fun upsertQa(e: QaEntity) = set(Collections.QA, e.id, e.toFirestoreMap())
    suspend fun deleteQa(id: String) = delete(Collections.QA, id)

    suspend fun upsertCourse(e: CourseEntity) = set(Collections.COURSES, e.id, e.toFirestoreMap())
    suspend fun deleteCourse(id: String) = delete(Collections.COURSES, id)

    suspend fun upsertMedia(e: MediaEntity) = set(Collections.MEDIA, e.id, e.toFirestoreMap())
    suspend fun deleteMedia(id: String) = delete(Collections.MEDIA, id)

    private suspend fun set(collection: String, id: String, data: Map<String, Any?>) {
        db.collection(collection).document(id).set(data).await()
    }

    private suspend fun delete(collection: String, id: String) {
        db.collection(collection).document(id).delete().await()
    }

    private fun <T> observeCollection(collection: String, mapper: (Map<String, Any?>) -> T?): Flow<List<T>> =
        callbackFlow {
            // includeMetadataChanges=false: we don't care whether a snapshot
            // came from cache or server, only that it's the current known
            // state -- that's exactly what we want mirrored into Room.
            val registration = db.collection(collection).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Don't close the flow on a transient error (e.g. a
                    // momentary permission hiccup on sign-in) -- just skip
                    // this tick and wait for the next snapshot.
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc -> doc.data?.let(mapper) } ?: emptyList()
                trySend(items)
            }
            awaitClose { registration.remove() }
        }
}
