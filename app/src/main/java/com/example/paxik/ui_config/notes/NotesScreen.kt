package com.example.paxik.ui_config.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paxik.data.Note

@Composable
fun NotesScreen(
    notes: List<Note>,
    onAddNote: (String, String) -> Unit,
    onEditNote: (Long, String, String) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { noteToEdit = note },
                    onLongClick = { noteToDelete = note }
                )
            }
        }
    }

    if (showAddDialog) {
        NoteDialog(
            dialogTitle = "Новая заметка",
            confirmText = "Добавить",
            onDismiss = { showAddDialog = false },
            onConfirm = { title, text ->
                onAddNote(title, text)
                showAddDialog = false
            }
        )
    }

    noteToEdit?.let { note ->
        NoteDialog(
            dialogTitle = "Редактирование",
            confirmText = "Сохранить",
            initialTitle = note.title,
            initialText = note.text,
            onDismiss = { noteToEdit = null },
            onConfirm = { title, text ->
                onEditNote(note.id, title, text)
                noteToEdit = null
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Удалить заметку?") },
            text = {
                Text(
                    text = note.title.ifBlank { note.text },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNote(note.id)
                    noteToDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Отмена") }
            }
        )
    }
}