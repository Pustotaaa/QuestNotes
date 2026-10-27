package com.questnotes.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Пункт чеклиста. quest: 0 — обычный пункт, 1..3 — задание РПГ с уровнем сложности. */
data class CheckItem(
    val id: Long,
    val text: String = "",
    val done: Boolean = false,
    val quest: Int = 0,
    val rewarded: Boolean = false,
)

data class Note(
    val id: Long,
    val title: String = "",
    val text: String = "",
    val isList: Boolean = false,
    val items: List<CheckItem> = emptyList(),
    val color: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    /** Служебная заметка «Квесты», куда попадают задания, созданные с экрана РПГ. */
    val board: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isBlank: Boolean
        get() = title.isBlank() && text.isBlank() && items.all { it.text.isBlank() }
}

fun Note.matches(q: String): Boolean =
    title.contains(q, ignoreCase = true) ||
        text.contains(q, ignoreCase = true) ||
        items.any { it.text.contains(q, ignoreCase = true) }

enum class Section(val label: String) {
    Notes("Заметки"),
    Archive("Архив"),
    Trash("Корзина"),
}

class LevelInfo(val level: Int, val into: Int, val need: Int)

object Rpg {
    fun reward(difficulty: Int): Int = when (difficulty) {
        1 -> 10
        2 -> 25
        else -> 50
    }

    fun difficultyName(difficulty: Int): String = when (difficulty) {
        1 -> "Лёгкое"
        2 -> "Среднее"
        else -> "Сложное"
    }

    /** Сколько опыта нужно, чтобы с уровня [level] перейти на следующий. */
    fun need(level: Int): Int = 50 + 25 * level

    fun levelOf(totalXp: Int): LevelInfo {
        var level = 1
        var rest = totalXp
        while (rest >= need(level)) {
            rest -= need(level)
            level++
        }
        return LevelInfo(level, rest, need(level))
    }

    fun title(level: Int): String = when {
        level < 3 -> "Новичок"
        level < 5 -> "Ученик"
        level < 8 -> "Воин"
        level < 12 -> "Герой"
        else -> "Легенда"
    }

    fun avatar(level: Int): String = when {
        level < 3 -> "🗡️"
        level < 5 -> "📖"
        level < 8 -> "⚔️"
        level < 12 -> "🛡️"
        else -> "👑"
    }
}

/** Одноразовое сообщение для снекбара. Новый экземпляр = новое сообщение. */
class Toast(val text: String)

class NotesStore(app: Application) : AndroidViewModel(app) {

    val notes = mutableStateListOf<Note>()

    var totalXp by mutableIntStateOf(0)
        private set
    var questsDone by mutableIntStateOf(0)
        private set
    var toast by mutableStateOf<Toast?>(null)

    private val file = File(app.filesDir, "data.json")

    init {
        load()
    }

    fun find(id: Long): Note? = notes.firstOrNull { it.id == id }

    /** Сохраняет заметку и начисляет опыт за новые выполненные задания. */
    fun upsert(note: Note) {
        val idx = notes.indexOfFirst { it.id == note.id }
        val old = if (idx >= 0) notes[idx] else null
        var gained = 0
        var completed = 0
        val items = note.items.map { item ->
            val wasRewarded = old?.items?.firstOrNull { it.id == item.id }?.rewarded == true
            val award = item.quest > 0 && item.done && !wasRewarded
            if (award) {
                gained += Rpg.reward(item.quest)
                completed++
            }
            item.copy(rewarded = wasRewarded || award)
        }
        val fixed = note.copy(items = items, updatedAt = System.currentTimeMillis())
        if (idx >= 0) notes[idx] = fixed else notes.add(0, fixed)
        if (gained > 0) addXp(gained, completed)
        save()
    }

    fun setTrashed(id: Long, trashed: Boolean) = update(id) {
        it.copy(trashed = trashed, pinned = if (trashed) false else it.pinned)
    }

    fun setArchived(id: Long, archived: Boolean) = update(id) {
        it.copy(archived = archived, pinned = if (archived) false else it.pinned)
    }

    fun deleteForever(id: Long) {
        notes.removeAll { it.id == id }
        save()
    }

    fun emptyTrash() {
        notes.removeAll { it.trashed }
        save()
    }

    fun toggleQuestItem(noteId: Long, itemId: Long) {
        val n = find(noteId) ?: return
        upsert(n.copy(items = n.items.map { if (it.id == itemId) it.copy(done = !it.done) else it }))
    }

    fun addQuest(title: String, difficulty: Int) {
        val item = CheckItem(id = System.nanoTime(), text = title.trim(), quest = difficulty)
        val board = notes.firstOrNull { it.board && !it.trashed }
        if (board == null) {
            upsert(
                Note(
                    id = System.currentTimeMillis(),
                    title = "⚔ Квесты",
                    isList = true,
                    board = true,
                    items = listOf(item),
                ),
            )
        } else {
            upsert(board.copy(items = board.items + item))
        }
    }

    private fun addXp(xp: Int, quests: Int) {
        val before = Rpg.levelOf(totalXp).level
        totalXp += xp
        questsDone += quests
        val after = Rpg.levelOf(totalXp).level
        toast = Toast(if (after > before) "+$xp XP · Новый уровень: $after!" else "+$xp XP")
    }

    private fun update(id: Long, change: (Note) -> Note) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx < 0) return
        notes[idx] = change(notes[idx])
        save()
    }

    // ---------- хранение в JSON-файле ----------

    private fun load() {
        try {
            if (!file.exists()) return
            val root = JSONObject(file.readText())
            totalXp = root.optInt("xp", 0)
            questsDone = root.optInt("questsDone", 0)
            val arr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                notes.add(noteFromJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // повреждённый файл: начинаем с пустого списка
        }
    }

    private fun save() {
        try {
            val root = JSONObject()
            root.put("xp", totalXp)
            root.put("questsDone", questsDone)
            val arr = JSONArray()
            notes.forEach { arr.put(noteToJson(it)) }
            root.put("notes", arr)
            val tmp = File(file.parentFile, "data.json.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(file)) file.writeText(root.toString())
        } catch (e: Exception) {
            // не удалось записать: данные останутся в памяти до закрытия
        }
    }

    private fun noteToJson(n: Note): JSONObject {
        val o = JSONObject()
        o.put("id", n.id)
        o.put("title", n.title)
        o.put("text", n.text)
        o.put("isList", n.isList)
        o.put("color", n.color)
        o.put("pinned", n.pinned)
        o.put("archived", n.archived)
        o.put("trashed", n.trashed)
        o.put("board", n.board)
        o.put("updatedAt", n.updatedAt)
        val arr = JSONArray()
        n.items.forEach { i ->
            val j = JSONObject()
            j.put("id", i.id)
            j.put("text", i.text)
            j.put("done", i.done)
            j.put("quest", i.quest)
            j.put("rewarded", i.rewarded)
            arr.put(j)
        }
        o.put("items", arr)
        return o
    }

    private fun noteFromJson(o: JSONObject): Note {
        val items = ArrayList<CheckItem>()
        val arr = o.optJSONArray("items")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val j = arr.getJSONObject(i)
                items.add(
                    CheckItem(
                        id = j.optLong("id"),
                        text = j.optString("text"),
                        done = j.optBoolean("done"),
                        quest = j.optInt("quest"),
                        rewarded = j.optBoolean("rewarded"),
                    ),
                )
            }
        }
        return Note(
            id = o.getLong("id"),
            title = o.optString("title"),
            text = o.optString("text"),
            isList = o.optBoolean("isList"),
            items = items,
            color = o.optInt("color"),
            pinned = o.optBoolean("pinned"),
            archived = o.optBoolean("archived"),
            trashed = o.optBoolean("trashed"),
            board = o.optBoolean("board"),
            updatedAt = o.optLong("updatedAt"),
        )
    }
}
