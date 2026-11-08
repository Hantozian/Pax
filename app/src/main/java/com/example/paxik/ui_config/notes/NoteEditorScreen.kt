package com.example.paxik.ui_config.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onClose: () -> Unit,
    onOpenNote: (Long) -> Unit,
    viewModel: NoteEditorViewModel = viewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var bodyFocused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val focusManager = LocalFocusManager.current
    val titleFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    val doneColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val spans = viewModel.spans
    val transformation = remember(spans, doneColor) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText =
                TransformedText(
                    RichText.buildStyled(text.text, spans, doneColor),
                    OffsetMapping.Identity
                )
        }
    }

    BackHandler { viewModel.close(onClose) }

    LaunchedEffect(viewModel.loaded) {
        if (viewModel.notFound) {
            onClose()
        } else if (viewModel.loaded && viewModel.title.isEmpty() && viewModel.body.text.isEmpty()) {
            titleFocus.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { viewModel.close(onClose) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    NoteLinksButton(onOpenNote = onOpenNote)
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            )
        }
    ) { padding ->
        if (viewModel.loaded && !viewModel.notFound) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    EditorField(
                        value = viewModel.title,
                        onValueChange = { input ->
                            if (input.contains('\n')) {
                                viewModel.title = input.replace("\n", "")
                                focusManager.moveFocus(FocusDirection.Down)
                            } else {
                                viewModel.title = input
                            }
                        },
                        placeholder = "Заголовок",
                        textStyle = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocus)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BasicTextField(
                        value = viewModel.body,
                        onValueChange = viewModel::onBodyChange,
                        textStyle = bodyStyle,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = transformation,
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 400.dp)
                            .onFocusChanged { bodyFocused = it.isFocused }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial
                                    )
                                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                    if (up != null &&
                                        up.uptimeMillis - down.uptimeMillis <
                                        viewConfiguration.longPressTimeoutMillis &&
                                        (up.position - down.position).getDistance() <
                                        viewConfiguration.touchSlop
                                    ) {
                                        val result = layout
                                        if (result != null) {
                                            val offset = result.getOffsetForPosition(up.position)
                                            scope.launch { viewModel.onBodyTap(offset) }
                                        }
                                    }
                                }
                            },
                        decorationBox = { innerTextField ->
                            Box {
                                if (viewModel.body.text.isEmpty()) {
                                    Text(
                                        text = "Начните писать…",
                                        style = bodyStyle,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                if (bodyFocused) {
                    FormattingToolbar(viewModel)
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Удалить заметку?",
            message = "Заметку нельзя будет восстановить.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete(onClose)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun FormattingToolbar(viewModel: NoteEditorViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(
            label = "B",
            active = viewModel.isActive(Fmt.BOLD),
            fontWeight = FontWeight.Bold
        ) { viewModel.toggleInline(Fmt.BOLD) }
        ToolbarButton(
            label = "I",
            active = viewModel.isActive(Fmt.ITALIC),
            fontStyle = FontStyle.Italic
        ) { viewModel.toggleInline(Fmt.ITALIC) }
        ToolbarButton(
            label = "U",
            active = viewModel.isActive(Fmt.UNDERLINE),
            decoration = TextDecoration.Underline
        ) { viewModel.toggleInline(Fmt.UNDERLINE) }
        ToolbarButton(
            label = "S",
            active = viewModel.isActive(Fmt.STRIKE),
            decoration = TextDecoration.LineThrough
        ) { viewModel.toggleInline(Fmt.STRIKE) }
        Spacer(modifier = Modifier.width(8.dp))
        ToolbarButton(
            label = "H1",
            active = viewModel.isActive(Fmt.H1),
            fontWeight = FontWeight.Bold
        ) { viewModel.toggleHeading(Fmt.H1) }
        ToolbarButton(
            label = "H2",
            active = viewModel.isActive(Fmt.H2),
            fontWeight = FontWeight.Bold
        ) { viewModel.toggleHeading(Fmt.H2) }
        Spacer(modifier = Modifier.width(8.dp))
        ToolbarButton(
            label = "•",
            active = viewModel.isLineKind(setOf(BULLET))
        ) { viewModel.toggleBullet() }
        ToolbarButton(
            label = "☐",
            active = viewModel.isLineKind(setOf(UNCHECKED, CHECKED))
        ) { viewModel.toggleCheckbox() }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    active: Boolean,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    decoration: TextDecoration? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = decoration,
            color = if (active) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val style = textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                innerTextField()
            }
        }
    )
}