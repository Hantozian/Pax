package com.example.paxik.ui_config.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paxik.data.Space

@Composable
fun SpacesDrawerContent(
    spaces: List<Space>,
    activeSpaceId: Long,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onLongClick: (Space) -> Unit
) {
    ModalDrawerSheet {
        Text(
            text = "Пространства",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(spaces, key = { it.id }) { space ->
                DrawerItem(
                    text = space.name,
                    selected = space.id == activeSpaceId,
                    onClick = { onSelect(space.id) },
                    onLongClick = { onLongClick(space) }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
        DrawerItem(
            text = "+  Новое пространство",
            selected = false,
            onClick = onCreate,
            onLongClick = null
        )
        Text(
            text = "Удерживайте пространство, чтобы изменить",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}