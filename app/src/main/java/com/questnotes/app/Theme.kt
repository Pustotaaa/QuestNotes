package com.questnotes.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KeepBg = Color(0xFF202124)
val KeepText = Color(0xFFE8EAED)
val KeepMuted = Color(0xFF9AA0A6)
val KeepBorder = Color(0xFF5F6368)
val KeepSearch = Color(0xFF3C4043)
val KeepAccent = Color(0xFF8AB4F8)
val Gold = Color(0xFFFBBC04)
val GoldDark = Color(0xFF41331C)

/** Тёмная палитра карточек, как в Google Keep. Первый цвет — по умолчанию. */
val NoteColors = listOf(
    Color(0xFF202124),
    Color(0xFF5C2B29),
    Color(0xFF614A19),
    Color(0xFF635D19),
    Color(0xFF345920),
    Color(0xFF16504B),
    Color(0xFF2D555E),
    Color(0xFF1E3A5F),
    Color(0xFF42275E),
    Color(0xFF5B2245),
    Color(0xFF442F19),
    Color(0xFF3C3F43),
)

@Composable
fun QuestTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = KeepAccent,
        onPrimary = KeepBg,
        background = KeepBg,
        onBackground = KeepText,
        surface = KeepBg,
        onSurface = KeepText,
        surfaceVariant = KeepSearch,
        onSurfaceVariant = KeepMuted,
        outline = KeepBorder,
    )
    // Шрифт по умолчанию — системный Roboto (Google Sans нельзя свободно встраивать).
    MaterialTheme(colorScheme = scheme, content = content)
}
