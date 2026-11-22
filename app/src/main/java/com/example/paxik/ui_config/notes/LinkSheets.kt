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

private enum class LinksStage { LIST, PICK }

@Composable
fun NodeLinksButton(
    node: NodeRef,
    onOpenNode: (NodeRef) -> Unit,
    viewModel: LinksViewModel = viewModel()
) {
    val graph by viewModel.graph.collectAsState()
    var show by remember { mutableStateOf(false) }
    val count = remember(graph, node) { graph.links.count { it.from == node || it.to == node } }

    TextButton(onClick = { show = true }) {
        Text(if (count > 0) "Связи · $count" else "Связи")
    }

    if (show) {
        NodeLinksSheets(
            node = node,
            onOpenNode = onOpenNode,
            onDismiss = { show = false }
        )
    }
}

@Composable
fun NodeLinksSheets(
    node: NodeRef,
    onOpenNode: (NodeRef) -> Unit,
    onDismiss: () -> Unit,
    viewModel: LinksViewModel = viewModel()
) {
    val graph by viewModel.graph.collectAsState()
    val relations = remember(graph, node) { graph.relationsOf(node) }
    val title = graph.byRef[node]?.displayTitle.orEmpty()
    var stage by remember { mutableStateOf(LinksStage.LIST) }

    when (stage) {
        LinksStage.LIST -> NodeLinksSheet(
            title = title,
            relations = relations,
            onOpen = onOpenNode,
            onRemoveOutgoing = { viewModel.removeLink(node, it) },
            onRemoveIncoming = { viewModel.removeLink(it, node) },
            onAdd = { stage = LinksStage.PICK },
            onDismiss = onDismiss
        )

        LinksStage.PICK -> PickNodeSheet(
            candidates = relations.candidates,
            onPick = { viewModel.addLink(node, it) },
            onDismiss = { stage = LinksStage.LIST }
        )
    }
}

@Composable
private fun NodeLinksSheet(
    title: String,
    relations: Relations,
    onOpen: (NodeRef) -> Unit,
    onRemoveOutgoing: (NodeRef) -> Unit,
    onRemoveIncoming: (NodeRef) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        SheetHeader(if (title.isBlank()) "Связи" else "Связи · $title")
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
                items(relations.outgoing, key = { "out-${it.ref.type}-${it.ref.id}" }) { node ->
                    LinkRow(
                        node = node,
                        onClick = { close { onOpen(node.ref) } },
                        onRemove = { onRemoveOutgoing(node.ref) }
                    )
                }
            }
            if (relations.incoming.isNotEmpty()) {
                item { SectionLabel("Ссылаются сюда") }
                items(relations.incoming, key = { "in-${it.ref.type}-${it.ref.id}" }) { node ->
                    LinkRow(
                        node = node,
                        onClick = { close { onOpen(node.ref) } },
                        onRemove = { onRemoveIncoming(node.ref) }
                    )
                }
            }
        }
        ListItem(
            headlineContent = {
                Text("+  Добавить связь", color = MaterialTheme.colorScheme.primary)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.clickable(onClick = onAdd)
        )
    }
}

@Composable
private fun PickNodeSheet(
    candidates: List<GraphNode>,
    onPick: (NodeRef) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss) { close ->
        var query by remember { mutableStateOf("") }
        val filtered = candidates.filter { it.title.contains(query.trim(), ignoreCase = true) }

        SheetHeader("Связать с…")
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
                text = "Ничего не найдено",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(filtered, key = { "${it.ref.type}-${it.ref.id}" }) { node ->
                ListItem(
                    headlineContent = {
                        Text(node.displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    supportingContent = {
                        Text(node.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { close { onPick(node.ref) } }
                )
            }
        }
    }
}

@Composable
private fun LinkRow(node: GraphNode, onClick: () -> Unit, onRemove: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(node.displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(node.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
