package com.trademaster.pro.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

// Five top-level destinations -- down from the original eight. Feed, Polls
// and Q&A are all "things the community posts", so they live together under
// Community with internal tabs. Education content and the files that back it
// (PDFs, charts, recordings) are one browsing experience, not two, so Learn
// merges Courses + Media Library the same way.
sealed class Destination(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    data object Dashboard : Destination("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Signals : Destination("signals", "Signals", Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    data object Setups : Destination("setups", "Setups", Icons.Filled.Widgets, Icons.Outlined.Widgets)
    data object Community : Destination("community", "Community", Icons.Filled.Forum, Icons.Outlined.Forum)
    data object Learn : Destination("learn", "Learn", Icons.Filled.School, Icons.Outlined.School)

    companion object {
        val bottomNavItems = listOf(Dashboard, Signals, Setups, Community, Learn)
    }
}

enum class CommunityTab(val label: String) { FEED("Feed"), POLLS("Polls"), QA("Q&A") }
enum class LearnTab(val label: String) { COURSES("Courses"), MEDIA("Media") }
