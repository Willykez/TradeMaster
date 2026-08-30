package com.trademaster.pro.ui.screens.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trademaster.pro.data.model.*
import com.trademaster.pro.data.repo.TradeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Backs the merged "Learn" tab: courses and their supporting files (PDFs,
// recordings, charts) are one browsing experience for a member, so Courses
// and Media Library share this ViewModel and one screen with two tabs.
class LearnViewModel(private val repo: TradeRepository) : ViewModel() {

    val courses: StateFlow<List<CourseEntity>> =
        repo.courses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val media: StateFlow<List<MediaEntity>> =
        repo.media.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun enroll(course: CourseEntity) = viewModelScope.launch { repo.enroll(course) }
    fun download(media: MediaEntity) = viewModelScope.launch { repo.recordDownload(media) }

    fun saveCourse(
        existing: CourseEntity?, title: String, desc: String, duration: String,
        lessons: Int, type: CourseType, category: CourseCategory
    ): String? {
        if (title.isBlank() || desc.isBlank()) return "Please fill in title and description"
        val entity = existing?.copy(
            title = title, desc = desc, duration = duration.ifBlank { "N/A" }, lessons = lessons,
            type = type, category = category
        ) ?: CourseEntity(
            title = title, desc = desc, duration = duration.ifBlank { "N/A" }, lessons = lessons,
            type = type, category = category, enrolled = 0, createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { repo.saveCourse(entity) }
        return null
    }

    fun deleteCourse(course: CourseEntity) = viewModelScope.launch { repo.deleteCourse(course) }

    fun saveMedia(name: String, type: MediaType, sizeLabel: String): String? {
        if (name.isBlank()) return "Please enter a file name"
        viewModelScope.launch {
            repo.saveMedia(
                MediaEntity(
                    name = name, type = type, sizeLabel = sizeLabel.ifBlank { "-- " },
                    dateLabel = "Just now", downloads = 0, createdAt = System.currentTimeMillis()
                )
            )
        }
        return null
    }

    fun deleteMedia(media: MediaEntity) = viewModelScope.launch { repo.deleteMedia(media) }
}
