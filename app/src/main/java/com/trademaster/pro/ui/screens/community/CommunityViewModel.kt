package com.trademaster.pro.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trademaster.pro.data.model.PollEntity
import com.trademaster.pro.data.model.PollOption
import com.trademaster.pro.data.model.PostEntity
import com.trademaster.pro.data.model.QaEntity
import com.trademaster.pro.data.repo.TradeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Backs the merged "Community" tab: Feed, Polls and Q&A were three separate
// top-level screens in the web app; here they're three lanes of the same
// social space, sharing one ViewModel the way the data naturally clusters.
class CommunityViewModel(private val repo: TradeRepository) : ViewModel() {

    val posts: StateFlow<List<PostEntity>> =
        repo.posts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val polls: StateFlow<List<PollEntity>> =
        repo.polls.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val qaList: StateFlow<List<QaEntity>> =
        repo.qa.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -- Feed --
    fun toggleLike(post: PostEntity) = viewModelScope.launch { repo.toggleLike(post) }

    fun submitPost(text: String, author: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.savePost(
                PostEntity(
                    author = author, avatar = author.take(2).uppercase(), text = text.trim(),
                    tags = emptyList(), likes = 0, liked = false, comments = 0, pinned = false,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun savePost(existing: PostEntity?, text: String, author: String, tagsCsv: String, pinned: Boolean): String? {
        if (text.isBlank()) return "Please write something"
        val tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val entity = existing?.copy(
            text = text.trim(), author = author.ifBlank { "TradeMaster" }, tags = tags, pinned = pinned
        ) ?: PostEntity(
            author = author.ifBlank { "TradeMaster" }, avatar = author.take(2).uppercase().ifBlank { "TM" },
            text = text.trim(), tags = tags, likes = 0, liked = false, comments = 0, pinned = pinned,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repo.savePost(entity) }
        return null
    }

    fun deletePost(post: PostEntity) = viewModelScope.launch { repo.deletePost(post) }

    // -- Polls --
    fun vote(poll: PollEntity, index: Int) = viewModelScope.launch { repo.vote(poll, index) }

    fun savePoll(existing: PollEntity?, question: String, optionLines: String): String? {
        val labels = optionLines.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (question.isBlank() || labels.size < 2) return "Enter a question and at least 2 options"
        val oldByLabel = existing?.options?.associateBy { it.label } ?: emptyMap()
        val options = labels.map { label -> oldByLabel[label]?.let { PollOption(label, it.votes) } ?: PollOption(label, 0) }
        val entity = (existing ?: PollEntity(
            question = question, options = options, active = true, userVoted = false,
            createdAt = System.currentTimeMillis()
        )).let { if (existing != null) it.copy(question = question, options = options) else it }
        viewModelScope.launch { repo.savePoll(entity) }
        return null
    }

    fun deletePoll(poll: PollEntity) = viewModelScope.launch { repo.deletePoll(poll) }
    fun togglePollActive(poll: PollEntity) = viewModelScope.launch { repo.togglePollActive(poll) }

    // -- Q&A --
    fun askQuestion(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            repo.saveQa(QaEntity(question = question.trim(), answer = "", votes = 0, voted = false, createdAt = System.currentTimeMillis()))
        }
    }

    fun markHelpful(item: QaEntity) = viewModelScope.launch { repo.markHelpful(item) }

    fun saveQa(existing: QaEntity?, question: String, answer: String): String? {
        if (question.isBlank() || answer.isBlank()) return "Please fill in both question and answer"
        val entity = existing?.copy(question = question.trim(), answer = answer.trim())
            ?: QaEntity(question = question.trim(), answer = answer.trim(), votes = 0, voted = false, createdAt = System.currentTimeMillis())
        viewModelScope.launch { repo.saveQa(entity) }
        return null
    }

    fun deleteQa(item: QaEntity) = viewModelScope.launch { repo.deleteQa(item) }
}
