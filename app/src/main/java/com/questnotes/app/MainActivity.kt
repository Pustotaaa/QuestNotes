package com.questnotes.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            QuestTheme {
                App()
            }
        }
    }
}

@Composable
fun App(store: NotesStore = viewModel()) {
    // -1 — редактор закрыт, иначе id открытой заметки
    var editingId by rememberSaveable { mutableStateOf(-1L) }
    var showRpg by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(store.toast) {
        val t = store.toast
        if (t != null) {
            snackbar.showSnackbar(t.text, duration = SnackbarDuration.Short)
            if (store.toast === t) store.toast = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KeepBg),
    ) {
        // Список заметок всегда остаётся под остальными экранами,
        // поэтому поиск и прокрутка сохраняются после возврата.
        NotesScreen(
            store = store,
            onOpen = { editingId = it },
            onNew = { editingId = System.currentTimeMillis() },
            onRpg = { showRpg = true },
        )

        if (showRpg) {
            RpgScreen(store = store, onBack = { showRpg = false })
        }

        if (editingId != -1L) {
            EditorScreen(store = store, noteId = editingId, onClose = { editingId = -1L })
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}
