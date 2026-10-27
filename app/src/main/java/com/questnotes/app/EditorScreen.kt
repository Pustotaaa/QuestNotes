package com.questnotes.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EditorScreen(store: NotesStore, noteId: Long, onClose: () -> Unit) {
    val initial = remember(noteId) { store.find(noteId) ?: Note(id = noteId) }
    val isNew = remember(noteId) { initial.isBlank }

    var title by remember(noteId) { mutableStateOf(initial.title) }
    var text by remember(noteId) { mutableStateOf(initial.text) }
    var isList by remember(noteId) { mutableStateOf(initial.isList) }
    var color by remember(noteId) { mutableStateOf(initial.color) }
    var pinned by remember(noteId) { mutableStateOf(initial.pinned) }
    val items = remember(noteId) { mutableStateListOf<CheckItem>().apply { addAll(initial.items) } }
    var focusId by remember(noteId) { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var lastSaved by remember(noteId) { mutableStateOf(initial) }
    val bodyFocus = remember { FocusRequester() }

    val bg = NoteColors[color.coerceIn(0, NoteColors.lastIndex)]
    val archivedNow = store.find(noteId)?.archived == true

    fun current(): Note = initial.copy(
        title = title,
        text = text,
        isList = isList,
        items = items.toList(),
        color = color,
        pinned = pinned,
    )

    fun finish() {
        val n = current()
        if (n.isBlank) {
            store.deleteForever(n.id)
        } else if (n != lastSaved) {
            store.upsert(n)
        }
        onClose()
    }

    fun trash() {
        val n = current()
        if (n.isBlank) {
            store.deleteForever(n.id)
        } else {
            store.upsert(n)
            store.setTrashed(n.id, true)
            store.toast = Toast("Заметка перемещена в корзину")
        }
        onClose()
    }

    fun archive() {
        val n = current()
        if (n.isBlank) {
            store.deleteForever(n.id)
        } else {
            store.upsert(n)
            store.setArchived(n.id, !archivedNow)
            store.toast = Toast(if (archivedNow) "Заметка возвращена из архива" else "Заметка в архиве")
        }
        onClose()
    }

    fun updateItem(id: Long, change: (CheckItem) -> CheckItem) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) items[i] = change(items[i])
    }

    fun addItem() {
        val item = CheckItem(id = System.nanoTime())
        items.add(item)
        focusId = item.id
    }

    fun toggleMode() {
        if (isList) {
            text = items.joinToString("\n") { it.text }
            items.clear()
            isList = false
        } else {
            val lines = text.split("\n").filter { it.isNotBlank() }
            items.clear()
            lines.forEachIndexed { i, l -> items.add(CheckItem(id = System.nanoTime() + i, text = l)) }
            text = ""
            isList = true
            if (items.isEmpty()) addItem()
        }
    }

    BackHandler { finish() }

    // Автосохранение, как в Keep: через полсекунды после последнего изменения.
    val snapshot = current()
    LaunchedEffect(snapshot) {
        delay(500)
        if (snapshot != lastSaved && !snapshot.isBlank) {
            store.upsert(snapshot)
            lastSaved = snapshot
        }
    }

    // У новой текстовой заметки сразу открываем клавиатуру.
    LaunchedEffect(noteId) {
        if (isNew && !isList) {
            delay(150)
            try {
                bodyFocus.requestFocus()
            } catch (e: Exception) {
                // поле ещё не готово, ничего страшного
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) {} // не пропускаем нажатия на экран под редактором
            .imePadding(),
    ) {
        // ---------- верхняя панель ----------
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { finish() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = KeepText)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { pinned = !pinned }) {
                Text("📌", fontSize = 20.sp, modifier = Modifier.alpha(if (pinned) 1f else 0.4f))
            }
            IconButton(onClick = { trash() }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = KeepText)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Ещё", tint = KeepText)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isList) "Скрыть флажки" else "Показать флажки") },
                        onClick = {
                            menuOpen = false
                            toggleMode()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (archivedNow) "Вернуть из архива" else "В архив") },
                        onClick = {
                            menuOpen = false
                            archive()
                        },
                    )
                }
            }
        }

        // ---------- содержимое ----------
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            PlainField(
                value = title,
                onChange = { title = it },
                hint = "Название",
                style = TextStyle(color = KeepText, fontSize = 22.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (isList) {
                items.forEach { item ->
                    key(item.id) {
                        ChecklistRow(
                            item = item,
                            requestFocus = focusId == item.id,
                            onFocused = { focusId = null },
                            onCheck = { updateItem(item.id) { it.copy(done = !it.done) } },
                            onText = { t -> updateItem(item.id) { it.copy(text = t) } },
                            onQuest = { updateItem(item.id) { it.copy(quest = (it.quest + 1) % 4) } },
                            onDelete = { items.removeAll { it.id == item.id } },
                            onEnter = { addItem() },
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { addItem() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("＋", color = KeepMuted, fontSize = 20.sp, modifier = Modifier.width(40.dp))
                    Text("Добавить пункт", color = KeepMuted, fontSize = 16.sp)
                }
                Text(
                    "⚔ — сделать пункт заданием РПГ. Нажимайте, чтобы выбрать награду: +10, +25 или +50 XP. Опыт даётся один раз, когда пункт отмечен.",
                    color = KeepMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                PlainField(
                    value = text,
                    onChange = { text = it },
                    hint = "Заметка",
                    style = TextStyle(color = KeepText, fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp)
                        .padding(vertical = 8.dp),
                    fieldModifier = Modifier.focusRequester(bodyFocus),
                )
            }
        }

        // ---------- выбор цвета ----------
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteColors.forEachIndexed { i, c ->
                val selected = i == color
                Box(
                    Modifier
                        .padding(end = 10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) KeepAccent else KeepBorder,
                            shape = CircleShape,
                        )
                        .clickable { color = i },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = KeepAccent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: CheckItem,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onCheck: () -> Unit,
    onText: (String) -> Unit,
    onQuest: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            delay(60)
            try {
                focus.requestFocus()
            } catch (e: Exception) {
                // поле ещё не готово
            }
            onFocused()
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clickable(onClick = onCheck),
            contentAlignment = Alignment.Center,
        ) {
            CheckMark(checked = item.done, size = 20.dp)
        }
        PlainField(
            value = item.text,
            onChange = onText,
            hint = "Пункт",
            style = TextStyle(
                color = if (item.done) KeepMuted else KeepText,
                fontSize = 16.sp,
                textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            fieldModifier = Modifier.focusRequester(focus),
            singleLine = true,
            onNext = onEnter,
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onQuest)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = if (item.quest == 0) "⚔" else "⚔ +${Rpg.reward(item.quest)}",
                color = if (item.quest == 0) KeepMuted else Gold,
                fontSize = 13.sp,
                modifier = Modifier.alpha(if (item.quest == 0) 0.6f else 1f),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Удалить пункт", tint = KeepMuted, modifier = Modifier.size(18.dp))
        }
    }
}
