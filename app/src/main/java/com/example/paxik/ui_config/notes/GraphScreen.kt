package com.example.paxik.ui_config.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paxik.data.NodeType
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class GraphView(
    val nodes: List<GraphNode>,
    val linkEdges: List<Pair<NodeRef, NodeRef>>,
    val structureEdges: List<Pair<NodeRef, NodeRef>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    activeSpaceId: Long,
    onOpenNode: (NodeRef) -> Unit,
    onBack: () -> Unit,
    viewModel: LinksViewModel = viewModel()
) {
    val data by viewModel.graph.collectAsState()
    var allSpaces by rememberSaveable { mutableStateOf(false) }
    var showStructure by rememberSaveable { mutableStateOf(true) }
    var actionNode by remember { mutableStateOf<NodeRef?>(null) }
    var linksNode by remember { mutableStateOf<NodeRef?>(null) }

    val view = remember(data, allSpaces, showStructure, activeSpaceId) {
        val nodes = if (allSpaces) data.nodes else data.nodes.filter { it.spaceId == activeSpaceId }
        val shown = nodes.map { it.ref }.toSet()
        GraphView(
            nodes = nodes,
            linkEdges = data.links
                .filter { it.from in shown && it.to in shown }
                .map { it.from to it.to },
            structureEdges = if (showStructure) {
                nodes.mapNotNull { node ->
                    node.parent?.takeIf { it in shown }?.let { it to node.ref }
                }
            } else {
                emptyList()
            }
        )
    }
    val nodes = view.nodes
    val linkEdges = view.linkEdges
    val structureEdges = view.structureEdges
    val refs = remember(nodes) { nodes.map { it.ref } }
    val layoutEdges = remember(linkEdges, structureEdges) { linkEdges + structureEdges }

    val positions = remember { mutableStateMapOf<NodeRef, Offset>() }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var layoutVersion by remember { mutableIntStateOf(0) }
    var draggingRef by remember { mutableStateOf<NodeRef?>(null) }

    val degree = remember(linkEdges) {
        val map = HashMap<NodeRef, Int>()
        linkEdges.forEach { (from, to) ->
            map[from] = (map[from] ?: 0) + 1
            map[to] = (map[to] ?: 0) + 1
        }
        map
    }

    val textMeasurer = rememberTextMeasurer()
    val labelWidth = with(LocalDensity.current) { 110.dp.roundToPx() }
    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    val labels = remember(nodes, labelStyle, labelWidth) {
        nodes.associate { node ->
            node.ref to textMeasurer.measure(
                text = node.displayTitle.take(40),
                style = labelStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = labelWidth)
            )
        }
    }

    val noteColor = MaterialTheme.colorScheme.primary
    val folderColor = MaterialTheme.colorScheme.tertiary
    val spaceColor = MaterialTheme.colorScheme.secondary
    val orphanColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val linkColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    val structureColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    fun radiusOf(ref: NodeRef): Float {
        val base = when (ref.type) {
            NodeType.SPACE -> 26f
            NodeType.FOLDER -> 18f
            NodeType.NOTE -> 12f
        }
        return base + 2.5f * minOf(degree[ref] ?: 0, 6)
    }

    fun hit(screen: Offset): NodeRef? {
        val world = (screen - offset) / scale
        var best: NodeRef? = null
        var bestDistance = Float.MAX_VALUE
        for ((ref, point) in positions) {
            val distance = (point - world).getDistance()
            if (distance <= radiusOf(ref) + 14f / scale && distance < bestDistance) {
                best = ref
                bestDistance = distance
            }
        }
        return best
    }

    LaunchedEffect(refs, layoutEdges) {
        val result = withContext(Dispatchers.Default) { GraphLayout.compute(refs, layoutEdges) }
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
                title = { Text("Граф") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = allSpaces,
                    onClick = { allSpaces = !allSpaces },
                    label = { Text("Все пространства") }
                )
                FilterChip(
                    selected = showStructure,
                    onClick = { showStructure = !showStructure },
                    label = { Text("Структура") }
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (nodes.isEmpty()) {
                    Text(
                        text = "Здесь пока ничего нет",
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
                                    hit(position)?.let { actionNode = it }
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
                                    onDragStart = { position -> draggingRef = hit(position) },
                                    onDrag = { change, dragAmount ->
                                        val ref = draggingRef
                                        if (ref != null) {
                                            change.consume()
                                            val current = positions[ref]
                                            if (current != null) {
                                                positions[ref] = current + dragAmount / scale
                                            }
                                        }
                                    },
                                    onDragEnd = { draggingRef = null },
                                    onDragCancel = { draggingRef = null }
                                )
                            }
                    ) {
                        val strokeWidth = 1.5.dp.toPx()
                        val arrowSize = 9.dp.toPx()
                        val arrowGap = 3.dp.toPx()
                        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))

                        for ((from, to) in structureEdges) {
                            val a = positions[from] ?: continue
                            val b = positions[to] ?: continue
                            drawLine(
                                color = structureColor,
                                start = a * scale + offset,
                                end = b * scale + offset,
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash
                            )
                        }

                        for ((from, to) in linkEdges) {
                            val a = positions[from] ?: continue
                            val b = positions[to] ?: continue
                            val start = a * scale + offset
                            val end = b * scale + offset
                            drawLine(linkColor, start, end, strokeWidth)

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
                                    drawLine(linkColor, tip, tip - wing * arrowSize, strokeWidth)
                                }
                            }
                        }

                        for (node in nodes) {
                            val point = positions[node.ref] ?: continue
                            val center = point * scale + offset
                            val radius = radiusOf(node.ref) * scale
                            when (node.ref.type) {
                                NodeType.NOTE -> {
                                    val color = if ((degree[node.ref] ?: 0) > 0) noteColor else orphanColor
                                    drawCircle(color, radius, center)
                                }

                                NodeType.FOLDER -> drawRoundRect(
                                    color = folderColor,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = Size(radius * 2, radius * 2),
                                    cornerRadius = CornerRadius(radius * 0.35f)
                                )

                                NodeType.SPACE -> {
                                    drawCircle(spaceColor, radius, center)
                                    drawCircle(
                                        color = spaceColor,
                                        radius = radius + 4.dp.toPx(),
                                        center = center,
                                        style = Stroke(2.dp.toPx())
                                    )
                                }
                            }

                            val label = labels[node.ref]
                            if (label != null && (scale > 0.45f || node.ref.type == NodeType.SPACE)) {
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
                        text = "Тап по узлу — действия. Удерживайте и тяните, чтобы переместить.",
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

    actionNode?.let { ref ->
        val node = data.byRef[ref]
        if (node != null) {
            AppBottomSheet(onDismiss = { actionNode = null }) { close ->
                SheetTitle(node.displayTitle)
                SheetAction(text = "Открыть", onClick = { close { onOpenNode(ref) } })
                SheetAction(text = "Связи", onClick = { close { linksNode = ref } })
            }
        }
    }

    linksNode?.let { ref ->
        NodeLinksSheets(
            node = ref,
            onOpenNode = onOpenNode,
            onDismiss = { linksNode = null }
        )
    }
}
