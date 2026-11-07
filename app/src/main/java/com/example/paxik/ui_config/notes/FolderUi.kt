package com.example.paxik.ui_config.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paxik.data.Folder

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(folder: Folder, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Text(
            text = "📁 ${folder.name}",
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}