package com.example.paxik.ui_config.notes

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.paxik.data.Space
import kotlinx.coroutines.launch

@Composable
fun NotesRoute(
    viewModel: NotesViewModel,
    onOpenNote: (Long) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val path by viewModel.path.collectAsState()
    val folderOptions by viewModel.folderOptions.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val spaces by viewModel.spaces.collectAsState()
    val activeSpaceId by viewModel.activeSpaceId.collectAsState()

    val spaceName = spaces.firstOrNull { it.id == activeSpaceId }?.name ?: ""
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showNewSpace by remember { mutableStateOf(false) }
    var spaceForActions by remember { mutableStateOf<Space?>(null) }
    var spaceToRename by remember { mutableStateOf<Space?>(null) }
    var spaceToDelete by remember { mutableStateOf<Space?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SpacesDrawerContent(
                spaces = spaces,
                activeSpaceId = activeSpaceId,
                onSelect = { id ->
                    viewModel.selectSpace(id)
                    scope.launch { drawerState.close() }
                },
                onCreate = { showNewSpace = true },
                onLongClick = { space -> spaceForActions = space }
            )
        }
    ) {
        NotesScreen(
            spaceName = spaceName,
            notes = notes,
            folders = folders,
            path = path,
            folderOptions = folderOptions,
            currentFolderId = currentFolderId,
            onOpenMenu = { scope.launch { drawerState.open() } },
            onOpenFolder = viewModel::openFolder,
            onCreateNote = { viewModel.createNote(onOpenNote) },
            onOpenNote = onOpenNote,
            onDeleteNote = viewModel::deleteNote,
            onMoveNote = viewModel::moveNote,
            onAddFolder = viewModel::addFolder,
            onDeleteFolder = viewModel::deleteFolder
        )
    }

    if (showNewSpace) {
        NameSheet(
            title = "Новое пространство",
            confirmText = "Создать",
            onConfirm = { name ->
                viewModel.addSpace(name)
                scope.launch { drawerState.close() }
            },
            onDismiss = { showNewSpace = false }
        )
    }

    spaceForActions?.let { space ->
        SpaceActionsSheet(
            space = space,
            canDelete = spaces.size > 1,
            onRename = { spaceToRename = space },
            onDelete = { spaceToDelete = space },
            onDismiss = { spaceForActions = null }
        )
    }

    spaceToRename?.let { space ->
        NameSheet(
            title = "Переименовать",
            confirmText = "Сохранить",
            initial = space.name,
            onConfirm = { name -> viewModel.renameSpace(space.id, name) },
            onDismiss = { spaceToRename = null }
        )
    }

    spaceToDelete?.let { space ->
        ConfirmDeleteDialog(
            title = "Удалить пространство?",
            message = "«${space.name}» будет удалено вместе со всеми папками и заметками.",
            onConfirm = {
                viewModel.deleteSpace(space.id)
                spaceToDelete = null
            },
            onDismiss = { spaceToDelete = null }
        )
    }
}