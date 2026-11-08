package com.example.paxik.ui_config.notes

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object GraphLayout {
    const val SIZE = 1000f

    fun compute(ids: List<Long>, edges: List<Pair<Long, Long>>): Map<Long, Offset> {
        val n = ids.size
        if (n == 0) return emptyMap()
        if (n == 1) return mapOf(ids[0] to Offset(SIZE / 2, SIZE / 2))

        val index = HashMap<Long, Int>()
        ids.forEachIndexed { i, id -> index[id] = i }
        val pairs = ArrayList<Pair<Int, Int>>()
        for ((a, b) in edges) {
            val ia = index[a]
            val ib = index[b]
            if (ia != null && ib != null && ia != ib) pairs.add(ia to ib)
        }

        val center = SIZE / 2
        val px = FloatArray(n)
        val py = FloatArray(n)
        for (i in 0 until n) {
            val angle = (2.0 * PI * i / n).toFloat()
            val radius = SIZE * 0.35f * (0.6f + 0.4f * ((i % 3) / 2f))
            px[i] = center + cos(angle) * radius
            py[i] = center + sin(angle) * radius
        }

        val k = sqrt(SIZE * SIZE / n) * 0.7f
        var temperature = SIZE / 8f
        val dx = FloatArray(n)
        val dy = FloatArray(n)

        repeat(250) {
            dx.fill(0f)
            dy.fill(0f)

            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    var ddx = px[i] - px[j]
                    var ddy = py[i] - py[j]
                    var dist = sqrt(ddx * ddx + ddy * ddy)
                    if (dist < 0.01f) {
                        ddx = 0.1f * (i - j)
                        ddy = 0.1f
                        dist = sqrt(ddx * ddx + ddy * ddy)
                    }
                    val force = k * k / dist
                    val fx = ddx / dist * force
                    val fy = ddy / dist * force
                    dx[i] += fx
                    dy[i] += fy
                    dx[j] -= fx
                    dy[j] -= fy
                }
            }

            for ((a, b) in pairs) {
                val ddx = px[a] - px[b]
                val ddy = py[a] - py[b]
                val dist = maxOf(sqrt(ddx * ddx + ddy * ddy), 0.01f)
                val force = dist * dist / k
                val fx = ddx / dist * force
                val fy = ddy / dist * force
                dx[a] -= fx
                dy[a] -= fy
                dx[b] += fx
                dy[b] += fy
            }

            for (i in 0 until n) {
                dx[i] -= (px[i] - center) * 0.06f
                dy[i] -= (py[i] - center) * 0.06f
                val disp = sqrt(dx[i] * dx[i] + dy[i] * dy[i])
                if (disp > 0f) {
                    val limited = min(disp, temperature)
                    px[i] = (px[i] + dx[i] / disp * limited).coerceIn(20f, SIZE - 20f)
                    py[i] = (py[i] + dy[i] / disp * limited).coerceIn(20f, SIZE - 20f)
                }
            }
            temperature *= 0.96f
        }

        val result = HashMap<Long, Offset>()
        ids.forEachIndexed { i, id -> result[id] = Offset(px[i], py[i]) }
        return result
    }
}