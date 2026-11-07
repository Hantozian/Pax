package com.example.paxik.ui_config.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paxik.data.Note
import com.example.paxik.data.Space
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: (after: () -> Unit) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { after ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp)
        ) {
            content(close)
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SheetAction(
    text: String,
    onClick: () -> Unit,
    color: Color = Color.Unspecified
) {
    ListItem(
        headlineContent = { Text(text = text, color = color) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun NoteActionsSheet(
    note: Note,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        SheetTitle(note.title.ifBlank { note.text.ifBlank { "Без названия" } })
        SheetAction(text = "Переместить в папку", onClick = { close { onMove() } })
        SheetAction(
            text = "Удалить",
            color = MaterialTheme.colorScheme.error,
            onClick = { close { onDelete() } }
        )
    }
}

@Composable
fun SpaceActionsSheet(
    space: Space,
    canDelete: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        SheetTitle(space.name)
        SheetAction(text = "Переименовать", onClick = { close { onRename() } })
        if (canDelete) {
            SheetAction(
                text = "Удалить",
                color = MaterialTheme.colorScheme.error,
                onClick = { close { onDelete() } }
            )
        }
    }
}

@Composable
fun MoveNoteSheet(
    options: List<FolderOption>,
    currentFolderId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        SheetTitle("Переместить в")
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(options, key = { it.id ?: -1L }) { option ->
                val isCurrent = option.id == currentFolderId
                ListItem(
                    headlineContent = {
                        Text(
                            text = option.name,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Unspecified
                            }
                        )
                    },
                    supportingContent = if (option.parents.isNotEmpty()) {
                        { Text(option.parents) }
                    } else {
                        null
                    },
                    trailingContent = if (isCurrent) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Текущая папка",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        null
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(enabled = !isCurrent) {
                        close { onSelect(option.id) }
                    }
                )
            }
        }
    }
}

@Composable
fun NameSheet(
    title: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = ""
) {
    AppBottomSheet(onDismiss) { close ->
        var value by remember {
            mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
        }
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        SheetTitle(title)
        TextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            placeholder = { Text("Название") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (value.text.isNotBlank()) close { onConfirm(value.text) } }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .focusRequester(focusRequester)
        )
        Button(
            onClick = { close { onConfirm(value.text) } },
            enabled = value.text.isNotBlank(),
            modifier = Modifier
                .align(Alignment.End)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(confirmText)
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Удалить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}