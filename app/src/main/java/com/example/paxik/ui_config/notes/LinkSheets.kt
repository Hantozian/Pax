package com.example.paxik.ui_config.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paxik.data.Note

fun noteLabel(note: Note): String =
    note.title.ifBlank {
        note.text.trim().lineSequence().firstOrNull().orEmpty().ifBlank { "Без названия" }
    }

@Composable
fun NoteLinksButton(
    onOpenNote: (Long) -> Unit,
    viewModel: NoteLinksViewModel = viewModel()
) {
    val relations by viewModel.relations.collectAsState()
    var showLinks by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val count = relations.outgoing.size + relations.incoming.size

    TextButton(onClick = { showLinks = true }) {
        Text(if (count > 0) "Связи · $count" else "Связи")
    }

    if (showLinks) {
        NoteLinksSheet(
            relations = relations,
            onOpen = onOpenNote,
            onRemoveOutgoing = viewModel::removeOutgoing,
            onRemoveIncoming = viewModel::removeIncoming,
            onAdd = { showPicker = true },
            onDismiss = { showLinks = false }
        )
    }

    if (showPicker) {
        PickNoteSheet(
            candidates = relations.candidates,
            onPick = viewModel::addLink,
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun NoteLinksSheet(
    relations: Relations,
    onOpen: (Long) -> Unit,
    onRemoveOutgoing: (Long) -> Unit,
    onRemoveIncoming: (Long) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        SheetHeader("Связи")
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            if (relations.outgoing.isEmpty() && relations.incoming.isEmpty()) {
                item {
                    Text(
                        text = "Пока нет связей",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            }
            if (relations.outgoing.isNotEmpty()) {
                item { SectionLabel("Ссылается на") }
                items(relations.outgoing, key = { "out-${it.id}" }) { note ->
                    LinkRow(
                        note = note,
                        onClick = { close { onOpen(note.id) } },
                        onRemove = { onRemoveOutgoing(note.id) }
                    )
                }
            }
            if (relations.incoming.isNotEmpty()) {
                item { SectionLabel("Ссылаются сюда") }
                items(relations.incoming, key = { "in-${it.id}" }) { note ->
                    LinkRow(
                        note = note,
                        onClick = { close { onOpen(note.id) } },
                        onRemove = { onRemoveIncoming(note.id) }
                    )
                }
            }
        }
        ListItem(
            headlineContent = {
                Text("+  Добавить связь", color = MaterialTheme.colorScheme.primary)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable { close { onAdd() } }
        )
    }
}

@Composable
private fun PickNoteSheet(
    candidates: List<Note>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        var query by remember { mutableStateOf("") }
        val filtered = candidates.filter { noteLabel(it).contains(query.trim(), ignoreCase = true) }

        SheetHeader("Связать с заметкой")
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Поиск") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
        if (filtered.isEmpty()) {
            Text(
                text = "Нет подходящих заметок",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(filtered, key = { it.id }) { note ->
                ListItem(
                    headlineContent = {
                        Text(noteLabel(note), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = if (note.text.isNotBlank() && note.title.isNotBlank()) {
                        {
                            Text(
                                text = note.text.trim().lineSequence().first(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { close { onPick(note.id) } }
                )
            }
        }
    }
}

@Composable
private fun LinkRow(note: Note, onClick: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(noteLabel(note), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Убрать связь")
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SheetHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}