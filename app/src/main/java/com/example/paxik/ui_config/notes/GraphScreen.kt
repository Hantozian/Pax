package com.example.paxik.ui_config.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.paxik.data.Link
import com.example.paxik.data.Note
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    spaceName: String,
    notes: List<Note>,
    links: List<Link>,
    onOpenNote: (Long) -> Unit,
    onBack: () -> Unit
) {
    val ids = notes.map { it.id }
    val edges = links.map { it.fromId to it.toId }
    val positions = remember { mutableStateMapOf<Long, Offset>() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var layoutVersion by remember { mutableIntStateOf(0) }
    var draggingId by remember { mutableStateOf<Long?>(null) }

    val degree = remember(links) {
        val map = HashMap<Long, Int>()
        links.forEach {
            map[it.fromId] = (map[it.fromId] ?: 0) + 1
            map[it.toId] = (map[it.toId] ?: 0) + 1
        }
        map
    }

    val textMeasurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) { 110.dp.roundToPx() }
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    val labels = remember(notes, labelStyle, labelWidth) {
        notes.associate { note ->
            note.id to textMeasurer.measure(
                text = noteLabel(note).take(40),
                style = labelStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = labelWidth)
            )
        }
    }

    val nodeColor = MaterialTheme.colorScheme.primary
    val orphanColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val edgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)

    fun radiusOf(id: Long): Float = 14f + 3f * minOf(degree[id] ?: 0, 6)

    fun hit(screen: Offset): Long? {
        val world = (screen - offset) / scale
        var best: Long? = null
        var bestDistance = Float.MAX_VALUE
        for ((id, point) in positions) {
            val distance = (point - world).getDistance()
            if (distance <= radiusOf(id) + 14f / scale && distance < bestDistance) {
                best = id
                bestDistance = distance
            }
        }
        return best
    }

    LaunchedEffect(ids, edges) {
        val result = withContext(Dispatchers.Default) { GraphLayout.compute(ids, edges) }
        positions.clear()
        positions.putAll(result)
        layoutVersion++
    }

    LaunchedEffect(canvasSize, layoutVersion) {
        if (canvasSize == IntSize.Zero || positions.isEmpty()) return@LaunchedEffect
        val minX = positions.values.minOf { it.x }
        val maxX = positions.values.maxOf { it.x }
        val minY = positions.values.minOf { it.y }
        val maxY = positions.values.maxOf { it.y }
        val width = maxOf(maxX - minX, 200f)
        val height = maxOf(maxY - minY, 200f)
        val fit = minOf(
            canvasSize.width / (width + 160f),
            canvasSize.height / (height + 160f)
        ).coerceIn(0.2f, 1.5f)
        scale = fit
        offset = Offset(
            canvasSize.width / 2f - (minX + maxX) / 2f * fit,
            canvasSize.height / 2f - (minY + maxY) / 2f * fit
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Граф · $spaceName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (notes.isEmpty()) {
                Text(
                    text = "В этом пространстве пока нет заметок",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(degree) {
                            detectTapGestures(onTap = { position ->
                                hit(position)?.let(onOpenNote)
                            })
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(0.2f, 4f)
                                val applied = newScale / scale
                                offset = (offset - centroid) * applied + centroid + pan
                                scale = newScale
                            }
                        }
                        .pointerInput(degree) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { position -> draggingId = hit(position) },
                                onDrag = { change, dragAmount ->
                                    val id = draggingId
                                    if (id != null) {
                                        change.consume()
                                        val current = positions[id]
                                        if (current != null) {
                                            positions[id] = current + dragAmount / scale
                                        }
                                    }
                                },
                                onDragEnd = { draggingId = null },
                                onDragCancel = { draggingId = null }
                            )
                        }
                ) {
                    val strokeWidth = 1.5.dp.toPx()
                    val arrowSize = 9.dp.toPx()
                    val arrowGap = 3.dp.toPx()

                    for ((from, to) in edges) {
                        val a = positions[from] ?: continue
                        val b = positions[to] ?: continue
                        val start = a * scale + offset
                        val end = b * scale + offset
                        drawLine(edgeColor, start, end, strokeWidth)

                        val direction = end - start
                        val length = direction.getDistance()
                        if (length > 1f) {
                            val unit = direction / length
                            val tip = end - unit * (radiusOf(to) * scale + arrowGap)
                            for (angle in listOf(0.45f, -0.45f)) {
                                val wing = Offset(
                                    unit.x * cos(angle) - unit.y * sin(angle),
                                    unit.x * sin(angle) + unit.y * cos(angle)
                                )
                                drawLine(edgeColor, tip, tip - wing * arrowSize, strokeWidth)
                            }
                        }
                    }

                    for (note in notes) {
                        val point = positions[note.id] ?: continue
                        val center = point * scale + offset
                        val radius = radiusOf(note.id) * scale
                        val color = if ((degree[note.id] ?: 0) > 0) nodeColor else orphanColor
                        drawCircle(color, radius, center)

                        val label = labels[note.id]
                        if (label != null && scale > 0.45f) {
                            drawText(
                                textLayoutResult = label,
                                topLeft = Offset(
                                    center.x - label.size.width / 2f,
                                    center.y + radius + 4.dp.toPx()
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "Тап по узлу открывает заметку. Удерживайте и тяните, чтобы переместить.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}