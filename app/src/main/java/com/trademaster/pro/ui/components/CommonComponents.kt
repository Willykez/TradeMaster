package com.trademaster.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trademaster.pro.ui.theme.*

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextDim)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, deltaText: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMute)
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Spacer(Modifier.height(4.dp))
            Text("▲ $deltaText", style = MaterialTheme.typography.bodySmall, color = Green, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .background(Blue.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(icon: String, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = TextDim, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(message, color = TextMute, fontSize = 13.sp)
    }
}

@Composable
fun FilterTabRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = options.indexOf(selected).coerceAtLeast(0),
        containerColor = Color.Transparent,
        edgePadding = 0.dp,
        contentColor = Gold,
        divider = {}
    ) {
        options.forEach { opt ->
            Tab(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                text = { Text(opt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                selectedContentColor = Gold,
                unselectedContentColor = TextMute
            )
        }
    }
}

@Composable
fun AdminFab(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (expanded) CardBg else Gold,
        contentColor = if (expanded) TextPrimary else Color.Black,
        icon = { Icon(if (expanded) Icons.Filled.Close else Icons.Filled.Add, contentDescription = null) },
        text = { Text(if (expanded) "Close form" else "New") }
    )
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextDim)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            isError = isError,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = Border,
                focusedContainerColor = Bg,
                unfocusedContainerColor = Bg,
                cursorColor = Gold
            )
        )
        if (isError && errorText != null) {
            Text(errorText, color = Red, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
