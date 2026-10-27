package com.questnotes.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    store: NotesStore,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onRpg: () -> Unit,
) {
    var section by rememberSaveable { mutableStateOf(Section.Notes) }
    var query by rememberSaveable { mutableStateOf("") }
    var trashNote by remember { mutableStateOf<Note?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val q = query.trim()

    BackHandler(enabled = q.isNotEmpty() || section != Section.Notes) {
        query = ""
        section = Section.Notes
    }
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val visible = store.notes
        .filter { n ->
            val inSection = when {
                q.isNotEmpty() -> !n.trashed
                section == Section.Notes -> !n.trashed && !n.archived
                section == Section.Archive -> n.archived && !n.trashed
                else -> n.trashed
            }
            inSection && (q.isEmpty() || n.matches(q))
        }
        .sortedByDescending { it.updatedAt }
    val pinned = visible.filter { it.pinned }
    val others = visible.filter { !it.pinned }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = KeepBg) {
                Text(
                    "Квест-заметки",
                    color = KeepText,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 28.dp, top = 28.dp, bottom = 16.dp),
                )
                val icons = listOf("💡", "📥", "🗑")
                Section.values().forEachIndexed { i, s ->
                    NavigationDrawerItem(
                        label = { Text("${icons[i]}  ${s.label}") },
                        selected = s == section,
                        onClick = {
                            section = s
                            query = ""
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GoldDark,
                            unselectedContainerColor = Color.Transparent,
                            selectedTextColor = Gold,
                            unselectedTextColor = KeepText,
                        ),
                    )
                }
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(KeepBg),
        ) {
            Column(Modifier.fillMaxSize()) {
                TopSearchBar(
                    query = query,
                    level = Rpg.levelOf(store.totalXp).level,
                    onQuery = { query = it },
                    onMenu = { scope.launch { drawerState.open() } },
                    onRpg = onRpg,
                )

                if (section != Section.Notes && q.isEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(section.label, color = KeepText, fontSize = 20.sp)
                        Spacer(Modifier.weight(1f))
                        if (section == Section.Trash && visible.isNotEmpty()) {
                            TextButton(onClick = { store.emptyTrash() }) {
                                Text("Очистить корзину", color = KeepAccent)
                            }
                        }
                    }
                }

                if (visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                q.isNotEmpty() -> "Ничего не найдено"
                                section == Section.Notes -> "Здесь появятся ваши заметки"
                                section == Section.Archive -> "В архиве пусто"
                                else -> "Корзина пуста"
                            },
                            color = KeepMuted,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (pinned.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) { SectionLabel("ЗАКРЕПЛЁННЫЕ") }
                            items(pinned, key = { it.id }) { n ->
                                NoteCard(n) {
                                    if (n.trashed) {
                                        trashNote = n
                                    } else {
                                        onOpen(n.id)
                                    }
                                }
                            }
                            if (others.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) { SectionLabel("ДРУГИЕ") }
                            }
                        }
                        items(others, key = { it.id }) { n ->
                            NoteCard(n) {
                                if (n.trashed) {
                                    trashNote = n
                                } else {
                                    onOpen(n.id)
                                }
                            }
                        }
                    }
                }
            }

            if (section == Section.Notes && q.isEmpty()) {
                FloatingActionButton(
                    onClick = onNew,
                    containerColor = KeepSearch,
                    contentColor = Gold,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(20.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Новая заметка")
                }
            }
        }
    }

    val tn = trashNote
    if (tn != null) {
        AlertDialog(
            onDismissRequest = { trashNote = null },
            containerColor = KeepSearch,
            title = { Text("Заметка в корзине", color = KeepText) },
            text = { Text(tn.title.ifBlank { "Без названия" }, color = KeepMuted) },
            confirmButton = {
                TextButton(onClick = {
                    store.setTrashed(tn.id, false)
                    trashNote = null
                }) { Text("Восстановить", color = KeepAccent) }
            },
            dismissButton = {
                TextButton(onClick = {
                    store.deleteForever(tn.id)
                    trashNote = null
                }) { Text("Удалить навсегда", color = KeepAccent) }
            },
        )
    }
}

@Composable
private fun TopSearchBar(
    query: String,
    level: Int,
    onQuery: (String) -> Unit,
    onMenu: () -> Unit,
    onRpg: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        color = KeepSearch,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Меню", tint = KeepText)
            }
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Поиск", color = KeepMuted, fontSize = 16.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = KeepText, fontSize = 16.sp),
                    cursorBrush = SolidColor(KeepAccent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Очистить", tint = KeepText)
                }
            }
            Box(
                Modifier
                    .padding(end = 8.dp, start = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GoldDark)
                    .clickable(onClick = onRpg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚔ $level", color = Gold, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = KeepMuted,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    val bg = NoteColors[note.color.coerceIn(0, NoteColors.lastIndex)]
    val borderColor = if (note.color == 0) KeepBorder else bg
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        if (note.title.isNotBlank()) {
            Text(
                note.title,
                color = KeepText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
        }
        if (note.isList) {
            val pending = note.items.filter { !it.done && it.text.isNotBlank() }
            val doneCount = note.items.count { it.done && it.text.isNotBlank() }
            pending.take(6).forEach { item ->
                Row(
                    Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    CheckMark(checked = false, size = 14.dp)
                    Spacer(Modifier.width(8.dp))
                    if (item.quest > 0) {
                        Text("⚔ ", color = Gold, fontSize = 13.sp)
                    }
                    Text(
                        item.text,
                        color = KeepText,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (pending.size > 6) {
                Text("… ещё ${pending.size - 6}", color = KeepMuted, fontSize = 12.sp)
            }
            if (doneCount > 0) {
                Text("✓ Выполнено: $doneCount", color = KeepMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        } else if (note.text.isNotBlank()) {
            Text(
                note.text,
                color = KeepText,
                fontSize = 14.sp,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
