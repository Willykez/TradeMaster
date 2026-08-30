package com.trademaster.pro.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trademaster.pro.AppMode
import com.trademaster.pro.data.model.*
import com.trademaster.pro.ui.components.*
import com.trademaster.pro.ui.nav.LearnTab
import com.trademaster.pro.ui.theme.*

// Merges Education (courses) and Media Library into one browsing surface --
// a course's supporting PDFs/recordings are the same kind of content a
// member would look for right after finishing a lesson, so keeping them
// apart across two bottom-nav tabs added a seam that didn't need to exist.
@Composable
fun LearnScreen(viewModel: LearnViewModel, mode: AppMode) {
    var tab by remember { mutableStateOf(LearnTab.COURSES) }
    var showForm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)) {
            SectionHeader("Learn", "Courses, guides and files -- everything to master the markets.", Modifier.padding(bottom = 8.dp))
        }
        TabRow(selectedTabIndex = tab.ordinal, containerColor = Bg, contentColor = Gold) {
            LearnTab.entries.forEach { t ->
                Tab(
                    selected = tab == t, onClick = { tab = t; showForm = false },
                    text = { Text(t.label, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Gold, unselectedContentColor = TextMute
                )
            }
        }
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                LearnTab.COURSES -> CoursesTab(viewModel, mode, showForm) { showForm = it }
                LearnTab.MEDIA -> MediaTab(viewModel, mode, showForm) { showForm = it }
            }
        }
    }
}

@Composable
private fun CoursesTab(vm: LearnViewModel, mode: AppMode, showForm: Boolean, onFormToggle: (Boolean) -> Unit) {
    val courses by vm.courses.collectAsState()
    var editing by remember { mutableStateOf<CourseEntity?>(null) }
    var filter by remember { mutableStateOf("All") }

    val filtered = when (filter) {
        "Video" -> courses.filter { it.type == CourseType.VIDEO }
        "PDF" -> courses.filter { it.type == CourseType.PDF }
        "Beginner" -> courses.filter { it.category == CourseCategory.BEGINNER }
        "Advanced" -> courses.filter { it.category == CourseCategory.ADVANCED }
        else -> courses
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { FilterTabRow(listOf("All", "Video", "PDF", "Beginner", "Advanced"), filter) { filter = it } }
            if (mode == AppMode.ADMIN && showForm) {
                item {
                    CourseForm(
                        existing = editing,
                        onCancel = { onFormToggle(false); editing = null },
                        onSave = { title, desc, dur, lessons, type, cat ->
                            val err = vm.saveCourse(editing, title, desc, dur, lessons, type, cat)
                            if (err == null) { onFormToggle(false); editing = null }
                            err
                        },
                        onDelete = editing?.let { c -> { vm.deleteCourse(c); onFormToggle(false); editing = null } }
                    )
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyState("📚", "No courses found", "Try a different filter.") }
            } else {
                items(filtered, key = { it.id }) { course ->
                    CourseCard(course, mode, onEnroll = { vm.enroll(course) }, onEdit = { editing = course; onFormToggle(true) }, onDelete = { vm.deleteCourse(course) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (mode == AppMode.ADMIN) {
            AdminFab(showForm, onClick = {
                if (showForm) { onFormToggle(false); editing = null } else { editing = null; onFormToggle(true) }
            }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun CourseCard(course: CourseEntity, mode: AppMode, onEnroll: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onEnroll)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(90.dp).background(Bg),
                contentAlignment = Alignment.Center
            ) {
                Text(if (course.type == CourseType.VIDEO) "📹" else "📄", fontSize = 32.sp)
            }
            Column(Modifier.padding(14.dp)) {
                Text(course.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(course.desc, color = TextDim, fontSize = 12.sp, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("📚 ${course.lessons} lessons • ${course.duration}", fontSize = 11.sp, color = TextMute)
                    Text(course.category.name, fontSize = 11.sp, color = Gold, fontWeight = FontWeight.SemiBold)
                }
                Text("${course.enrolled} members enrolled", fontSize = 11.sp, color = TextMute)
                if (mode == AppMode.ADMIN) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEdit) { Text("Edit") }
                        OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseForm(
    existing: CourseEntity?,
    onCancel: () -> Unit,
    onSave: (String, String, String, Int, CourseType, CourseCategory) -> String?,
    onDelete: (() -> Unit)?
) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var desc by remember(existing) { mutableStateOf(existing?.desc ?: "") }
    var duration by remember(existing) { mutableStateOf(existing?.duration ?: "") }
    var lessons by remember(existing) { mutableStateOf(existing?.lessons?.toString() ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: CourseType.VIDEO) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: CourseCategory.BEGINNER) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (existing == null) "Add Course" else "Edit Course", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FormField("Title *", title, { title = it })
            Spacer(Modifier.height(10.dp))
            FormField("Description *", desc, { desc = it }, singleLine = false, minLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField("Duration", duration, { duration = it }, Modifier.weight(1f))
                FormField("Lessons", lessons, { lessons = it.filter(Char::isDigit) }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            ChipRow("Type", CourseType.entries, type) { type = it }
            Spacer(Modifier.height(10.dp))
            ChipRow("Category", CourseCategory.entries, category) { category = it }
            if (error != null) { Spacer(Modifier.height(6.dp)); Text(error!!, color = Red, fontSize = 12.sp) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { error = onSave(title, desc, duration, lessons.toIntOrNull() ?: 0, type, category) },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) { Text(if (existing == null) "Add" else "Update") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                if (onDelete != null) OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun MediaTab(vm: LearnViewModel, mode: AppMode, showForm: Boolean, onFormToggle: (Boolean) -> Unit) {
    val media by vm.media.collectAsState()
    var filter by remember { mutableStateOf("All") }
    val filtered = when (filter) {
        "Images" -> media.filter { it.type == MediaType.IMAGE }
        "Videos" -> media.filter { it.type == MediaType.VIDEO }
        "PDFs" -> media.filter { it.type == MediaType.PDF }
        else -> media
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { FilterTabRow(listOf("All", "Images", "Videos", "PDFs"), filter) { filter = it } }
            if (mode == AppMode.ADMIN && showForm) {
                item { MediaForm(onCancel = { onFormToggle(false) }, onSave = { name, type, size ->
                    val err = vm.saveMedia(name, type, size)
                    if (err == null) onFormToggle(false)
                    err
                }) }
            }
            if (filtered.isEmpty()) {
                item { EmptyState("📁", "No files here", "Try a different filter or upload a file.") }
            } else {
                items(filtered, key = { it.id }) { m ->
                    MediaRow(m, mode, onDownload = { vm.download(m) }, onDelete = { vm.deleteMedia(m) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        if (mode == AppMode.ADMIN) {
            AdminFab(showForm, onClick = { onFormToggle(!showForm) }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun MediaRow(m: MediaEntity, mode: AppMode, onDownload: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (m.type) { MediaType.IMAGE -> "🖼️"; MediaType.VIDEO -> "🎬"; MediaType.PDF -> "📄"; MediaType.FILE -> "📎" }
            Box(Modifier.size(40.dp).background(Bg, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
                Text("${m.sizeLabel} • ${m.dateLabel} • ${m.downloads} downloads", fontSize = 11.sp, color = TextMute)
            }
            IconButton(onClick = onDownload) { Text("⬇️") }
            if (mode == AppMode.ADMIN) {
                IconButton(onClick = onDelete) { Text("🗑️") }
            }
        }
    }
}

@Composable
private fun MediaForm(onCancel: () -> Unit, onSave: (String, MediaType, String) -> String?) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MediaType.PDF) }
    var size by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Add File", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FormField("File name *", name, { name = it })
            Spacer(Modifier.height(10.dp))
            ChipRow("Type", MediaType.entries, type) { type = it }
            Spacer(Modifier.height(10.dp))
            FormField("Size (e.g. 2.4 MB)", size, { size = it })
            if (error != null) { Spacer(Modifier.height(6.dp)); Text(error!!, color = Red, fontSize = 12.sp) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { error = onSave(name, type, size) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)) { Text("Add") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> ChipRow(label: String, options: List<T>, selected: T, onSelect: (T) -> Unit) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextDim)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold, selectedLabelColor = androidx.compose.ui.graphics.Color.Black)
                )
            }
        }
    }
}
