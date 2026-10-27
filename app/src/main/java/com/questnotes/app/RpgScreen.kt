package com.questnotes.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class QuestRef(val noteId: Long, val noteTitle: String, val item: CheckItem)

private class Achievement(val title: String, val desc: String, val unlocked: Boolean)

@Composable
fun RpgScreen(store: NotesStore, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var showAdd by remember { mutableStateOf(false) }
    val info = Rpg.levelOf(store.totalXp)

    val all = store.notes
        .filter { !it.trashed }
        .flatMap { n ->
            n.items
                .filter { it.quest > 0 && it.text.isNotBlank() }
                .map { QuestRef(n.id, n.title, it) }
        }
    val active = all.filter { !it.item.done }
    val done = all.filter { it.item.done }
    val liveNotes = store.notes.count { !it.trashed }

    val achievements = listOf(
        Achievement("Первый шаг", "Выполнить первое задание", store.questsDone >= 1),
        Achievement("Охотник за квестами", "Выполнить 10 заданий", store.questsDone >= 10),
        Achievement("Ветеран", "Выполнить 50 заданий", store.questsDone >= 50),
        Achievement("Пятый уровень", "Достичь 5 уровня", info.level >= 5),
        Achievement("Летописец", "Создать 10 заметок", liveNotes >= 10),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(KeepBg)
            .pointerInput(Unit) {}, // не пропускаем нажатия на экран под РПГ
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KeepText)
                    }
                    Text("Приключение", color = KeepText, fontSize = 22.sp)
                }
            }

            item { HeroCard(info = info, totalXp = store.totalXp, questsDone = store.questsDone) }

            item { SectionTitle("Задания · ${active.size}") }
            if (active.isEmpty()) {
                item {
                    Text(
                        "Пока нет заданий. Нажмите «Задание» ниже или нажмите ⚔ у пункта в любой заметке со списком.",
                        color = KeepMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(active, key = { it.item.id }) { q ->
                QuestRow(q) { store.toggleQuestItem(q.noteId, q.item.id) }
            }

            if (done.isNotEmpty()) {
                item { SectionTitle("Выполнено · ${done.size}") }
                items(done, key = { it.item.id }) { q ->
                    QuestRow(q) { store.toggleQuestItem(q.noteId, q.item.id) }
                }
            }

            item { SectionTitle("Достижения") }
            items(achievements) { a -> AchievementRow(a) }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            containerColor = Gold,
            contentColor = KeepBg,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Задание", fontWeight = FontWeight.Medium)
        }
    }

    if (showAdd) {
        AddQuestDialog(
            onDismiss = { showAdd = false },
            onAdd = { title, difficulty ->
                store.addQuest(title, difficulty)
                showAdd = false
            },
        )
    }
}

@Composable
private fun HeroCard(info: LevelInfo, totalXp: Int, questsDone: Int) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF2B2A1E), shape)
            .border(1.dp, Color(0xFF5A4A1A), shape)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GoldDark)
                    .border(2.dp, Gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(Rpg.avatar(info.level), fontSize = 30.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(Rpg.title(info.level), color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Уровень ${info.level}", color = KeepText, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { info.into.toFloat() / info.need.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = Gold,
            trackColor = KeepSearch,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${info.into} / ${info.need} XP до следующего уровня",
            color = KeepMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Stat("Заданий выполнено", questsDone.toString(), Modifier.weight(1f))
            Stat("Всего опыта", "$totalXp XP", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, color = KeepText, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(label, color = KeepMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = KeepMuted,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun QuestRow(q: QuestRef, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val done = q.item.done
    Row(
        Modifier
            .fillMaxWidth()
            .background(KeepSearch, shape)
            .clip(shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckMark(checked = done, size = 22.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                q.item.text,
                style = TextStyle(
                    color = if (done) KeepMuted else KeepText,
                    fontSize = 16.sp,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
            if (q.noteTitle.isNotBlank()) {
                Text(
                    q.noteTitle,
                    color = KeepMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (q.item.rewarded) "✓ ${Rpg.reward(q.item.quest)} XP" else "+${Rpg.reward(q.item.quest)} XP",
            color = Gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.alpha(if (q.item.rewarded) 0.5f else 1f),
        )
    }
}

@Composable
private fun AchievementRow(a: Achievement) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (a.unlocked) 1f else 0.45f)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (a.unlocked) "🏆" else "🔒", fontSize = 24.sp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(a.title, color = KeepText, fontSize = 16.sp)
            Text(a.desc, color = KeepMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AddQuestDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var difficulty by remember { mutableIntStateOf(2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KeepSearch,
        title = { Text("Новое задание", color = KeepText) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, KeepBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    PlainField(
                        value = title,
                        onChange = { title = it },
                        hint = "Что нужно сделать?",
                        style = TextStyle(color = KeepText, fontSize = 16.sp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (d in 1..3) {
                        val selected = d == difficulty
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) GoldDark else KeepBg)
                                .border(1.dp, if (selected) Gold else KeepBorder, RoundedCornerShape(10.dp))
                                .clickable { difficulty = d }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(Rpg.difficultyName(d), color = if (selected) Gold else KeepText, fontSize = 13.sp)
                            Text("+${Rpg.reward(d)} XP", color = if (selected) Gold else KeepMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onAdd(title, difficulty) },
                enabled = title.isNotBlank(),
            ) { Text("Добавить", color = if (title.isNotBlank()) KeepAccent else KeepMuted) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = KeepAccent) }
        },
    )
}
