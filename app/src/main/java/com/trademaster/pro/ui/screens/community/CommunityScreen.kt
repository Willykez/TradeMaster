package com.trademaster.pro.ui.screens.community

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
import com.trademaster.pro.data.model.PollEntity
import com.trademaster.pro.data.model.PostEntity
import com.trademaster.pro.data.model.QaEntity
import com.trademaster.pro.ui.components.*
import com.trademaster.pro.ui.nav.CommunityTab
import com.trademaster.pro.ui.theme.*

// One screen, three lanes: Feed / Polls / Q&A. In the web app these were
// three separate top-level pages that all did the same thing conceptually
// -- members posting and reacting to community content -- so here they're
// tabs inside a single Community destination instead of competing for a
// bottom-nav slot each.
@Composable
fun CommunityScreen(viewModel: CommunityViewModel, mode: AppMode) {
    var tab by remember { mutableStateOf(CommunityTab.FEED) }
    var showForm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)) {
            SectionHeader("Community", "Feed, polls and Q&A -- everything the community is doing, in one place.", Modifier.padding(bottom = 8.dp))
        }
        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = Bg,
            contentColor = Gold
        ) {
            CommunityTab.entries.forEach { t ->
                Tab(
                    selected = tab == t,
                    onClick = { tab = t; showForm = false },
                    text = { Text(t.label, fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = Gold,
                    unselectedContentColor = TextMute
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (tab) {
                CommunityTab.FEED -> FeedTab(viewModel, mode, showForm, onFormToggle = { showForm = it })
                CommunityTab.POLLS -> PollsTab(viewModel, mode, showForm, onFormToggle = { showForm = it })
                CommunityTab.QA -> QaTab(viewModel, mode, showForm, onFormToggle = { showForm = it })
            }
        }
    }
}

// ---------------- FEED ----------------

@Composable
private fun FeedTab(vm: CommunityViewModel, mode: AppMode, showForm: Boolean, onFormToggle: (Boolean) -> Unit) {
    val posts by vm.posts.collectAsState()
    var editing by remember { mutableStateOf<PostEntity?>(null) }
    var composerText by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode == AppMode.CLIENT) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            OutlinedTextField(
                                value = composerText, onValueChange = { composerText = it },
                                placeholder = { Text("Share a trade idea or insight…") },
                                modifier = Modifier.fillMaxWidth(), minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold, unfocusedBorderColor = Border,
                                    focusedContainerColor = Bg, unfocusedContainerColor = Bg
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { vm.submitPost(composerText, "You"); composerText = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)
                                ) { Text("Post") }
                            }
                        }
                    }
                }
            }
            if (mode == AppMode.ADMIN && showForm) {
                item {
                    PostForm(
                        existing = editing,
                        onCancel = { onFormToggle(false); editing = null },
                        onSave = { text, author, tags, pinned ->
                            val err = vm.savePost(editing, text, author, tags, pinned)
                            if (err == null) { onFormToggle(false); editing = null }
                            err
                        },
                        onDelete = editing?.let { p -> { vm.deletePost(p); onFormToggle(false); editing = null } }
                    )
                }
            }
            if (posts.isEmpty()) {
                item { EmptyState("💬", "No posts yet", "Be the first to share something.") }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(post, mode, onLike = { vm.toggleLike(post) }, onEdit = {
                        editing = post; onFormToggle(true)
                    }, onDelete = { vm.deletePost(post) })
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
private fun PostCard(post: PostEntity, mode: AppMode, onLike: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(post.author + if (post.pinned) "  📌 Pinned" else "", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(post.text, color = TextDim, fontSize = 13.sp, lineHeight = 19.sp)
            if (post.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { post.tags.forEach { TagChip(it) } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("${if (post.liked) "❤️" else "🤍"} ${post.likes}", fontSize = 12.sp, color = TextMute, modifier = Modifier.clickable(onClick = onLike))
                Text("💬 ${post.comments}", fontSize = 12.sp, color = TextMute)
            }
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

@Composable
private fun PostForm(
    existing: PostEntity?,
    onCancel: () -> Unit,
    onSave: (String, String, String, Boolean) -> String?,
    onDelete: (() -> Unit)?
) {
    var text by remember(existing) { mutableStateOf(existing?.text ?: "") }
    var author by remember(existing) { mutableStateOf(existing?.author ?: "TradeMaster") }
    var tags by remember(existing) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var pinned by remember(existing) { mutableStateOf(existing?.pinned ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (existing == null) "Create New Post" else "Edit Post", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FormField("Content *", text, { text = it }, singleLine = false, minLines = 3)
            Spacer(Modifier.height(10.dp))
            FormField("Author", author, { author = it })
            Spacer(Modifier.height(10.dp))
            FormField("Tags (comma separated)", tags, { tags = it })
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = pinned, onCheckedChange = { pinned = it }, colors = CheckboxDefaults.colors(checkedColor = Gold))
                Text("Pin to top", color = TextDim, fontSize = 13.sp)
            }
            if (error != null) { Spacer(Modifier.height(6.dp)); Text(error!!, color = Red, fontSize = 12.sp) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { error = onSave(text, author, tags, pinned) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)) {
                    Text(if (existing == null) "Publish" else "Update")
                }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                if (onDelete != null) OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
            }
        }
    }
}

// ---------------- POLLS ----------------

@Composable
private fun PollsTab(vm: CommunityViewModel, mode: AppMode, showForm: Boolean, onFormToggle: (Boolean) -> Unit) {
    val polls by vm.polls.collectAsState()
    var editing by remember { mutableStateOf<PollEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode == AppMode.ADMIN && showForm) {
                item {
                    PollForm(
                        existing = editing,
                        onCancel = { onFormToggle(false); editing = null },
                        onSave = { q, opts ->
                            val err = vm.savePoll(editing, q, opts)
                            if (err == null) { onFormToggle(false); editing = null }
                            err
                        },
                        onDelete = editing?.let { p -> { vm.deletePoll(p); onFormToggle(false); editing = null } },
                        onToggleActive = editing?.let { p -> { vm.togglePollActive(p) } }
                    )
                }
            }
            if (polls.isEmpty()) {
                item { EmptyState("📊", "No polls yet", "Check back soon for community polls.") }
            } else {
                items(polls, key = { it.id }) { poll ->
                    PollCard(poll, mode, onVote = { idx -> vm.vote(poll, idx) }, onEdit = { editing = poll; onFormToggle(true) })
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
private fun PollCard(poll: PollEntity, mode: AppMode, onVote: (Int) -> Unit, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("📊 ${poll.question}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
            Text("${poll.total} votes • ${if (poll.active) "Open" else "Closed"}", fontSize = 11.sp, color = TextMute)
            Spacer(Modifier.height(10.dp))
            poll.options.forEachIndexed { idx, opt ->
                val pct = if (poll.total == 0) 0 else (opt.votes * 100 / poll.total)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Bg, RoundedCornerShape(10.dp))
                        .clickable(enabled = !poll.userVoted && poll.active) { onVote(idx) }
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(pct / 100f)
                            .height(38.dp)
                            .background(Gold.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(38.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
                    }
                }
            }
            if (mode == AppMode.ADMIN) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun PollForm(
    existing: PollEntity?,
    onCancel: () -> Unit,
    onSave: (String, String) -> String?,
    onDelete: (() -> Unit)?,
    onToggleActive: (() -> Unit)?
) {
    var question by remember(existing) { mutableStateOf(existing?.question ?: "") }
    var options by remember(existing) { mutableStateOf(existing?.options?.joinToString("\n") { it.label } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (existing == null) "Create Poll" else "Edit Poll", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FormField("Question *", question, { question = it })
            Spacer(Modifier.height(10.dp))
            FormField("Options, one per line (min 2) *", options, { options = it }, singleLine = false, minLines = 3)
            if (error != null) { Spacer(Modifier.height(6.dp)); Text(error!!, color = Red, fontSize = 12.sp) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { error = onSave(question, options) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)) {
                    Text(if (existing == null) "Create" else "Update")
                }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                if (onDelete != null) OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
                if (onToggleActive != null) OutlinedButton(onClick = onToggleActive) { Text(if (existing?.active == true) "Close" else "Reopen") }
            }
        }
    }
}

// ---------------- Q&A ----------------

@Composable
private fun QaTab(vm: CommunityViewModel, mode: AppMode, showForm: Boolean, onFormToggle: (Boolean) -> Unit) {
    val items by vm.qaList.collectAsState()
    var editing by remember { mutableStateOf<QaEntity?>(null) }
    var askText by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode == AppMode.CLIENT) {
                item {
                    OutlinedTextField(
                        value = askText, onValueChange = { askText = it },
                        placeholder = { Text("Ask a new question…") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { vm.askQuestion(askText); askText = "" }) { Text("Ask", color = Gold) }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, unfocusedBorderColor = Border,
                            focusedContainerColor = CardBg, unfocusedContainerColor = CardBg
                        )
                    )
                }
            }
            if (mode == AppMode.ADMIN && showForm) {
                item {
                    QaForm(
                        existing = editing,
                        onCancel = { onFormToggle(false); editing = null },
                        onSave = { q, a ->
                            val err = vm.saveQa(editing, q, a)
                            if (err == null) { onFormToggle(false); editing = null }
                            err
                        },
                        onDelete = editing?.let { item -> { vm.deleteQa(item); onFormToggle(false); editing = null } }
                    )
                }
            }
            if (items.isEmpty()) {
                item { EmptyState("❓", "No questions yet", "Ask the first question above.") }
            } else {
                items(items, key = { it.id }) { qa ->
                    QaCard(qa, mode, onHelpful = { vm.markHelpful(qa) }, onEdit = { editing = qa; onFormToggle(true) }, onDelete = { vm.deleteQa(qa) })
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
private fun QaCard(qa: QaEntity, mode: AppMode, onHelpful: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Box(Modifier.size(28.dp).background(Gold, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("Q", fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.Black, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text(qa.question, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.size(28.dp).background(Green, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("A", fontWeight = FontWeight.Black, color = androidx.compose.ui.graphics.Color.Black, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (qa.answer.isBlank()) "Awaiting an expert answer." else qa.answer,
                        color = if (qa.answer.isBlank()) TextMute else TextDim, fontSize = 13.sp, lineHeight = 19.sp
                    )
                    if (qa.answer.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "👍 Helpful (${qa.votes})",
                            fontSize = 11.sp, color = if (qa.voted) Gold else TextMute,
                            modifier = Modifier.clickable(enabled = !qa.voted, onClick = onHelpful)
                        )
                    }
                }
            }
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

@Composable
private fun QaForm(existing: QaEntity?, onCancel: () -> Unit, onSave: (String, String) -> String?, onDelete: (() -> Unit)?) {
    var q by remember(existing) { mutableStateOf(existing?.question ?: "") }
    var a by remember(existing) { mutableStateOf(existing?.answer ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(if (existing == null) "Add Q&A" else "Edit Q&A", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            FormField("Question *", q, { q = it })
            Spacer(Modifier.height(10.dp))
            FormField("Answer *", a, { a = it }, singleLine = false, minLines = 3)
            if (error != null) { Spacer(Modifier.height(6.dp)); Text(error!!, color = Red, fontSize = 12.sp) }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { error = onSave(q, a) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = androidx.compose.ui.graphics.Color.Black)) {
                    Text(if (existing == null) "Add" else "Update")
                }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                if (onDelete != null) OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = Red)) { Text("Delete") }
            }
        }
    }
}
