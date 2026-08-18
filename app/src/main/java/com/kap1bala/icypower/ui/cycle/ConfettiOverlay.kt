package com.kap1bala.icypower.ui.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntSize
import com.kap1bala.icypower.ui.theme.ChartSeries1
import com.kap1bala.icypower.ui.theme.ChartSeries2
import com.kap1bala.icypower.ui.theme.ChartSeries3
import com.kap1bala.icypower.ui.theme.ChartSeries4
import com.kap1bala.icypower.ui.theme.LocalSuccess
import com.kap1bala.icypower.ui.theme.LocalWarning
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotationDeg: Float,
    var angularVelocityDeg: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val lifeSeconds: Float,
    val originSide: Side,
) {
    enum class Side { Left, Right }
    var ageSeconds: Float = 0f

    fun step(deltaSec: Float, gravity: Float, airDrag: Float, canvas: IntSize): Boolean {
        ageSeconds += deltaSec
        vy += gravity * deltaSec
        vx *= (1f - airDrag * deltaSec).coerceAtLeast(0f)
        x += vx * deltaSec
        y += vy * deltaSec
        rotationDeg = (rotationDeg + angularVelocityDeg * deltaSec) % 360f
        return ageSeconds < lifeSeconds &&
            y < canvas.height + 200f && y > -300f &&
            x > -200f && x < canvas.width + 200f
    }

    val alpha: Float
        get() = (1f - (ageSeconds / lifeSeconds)).coerceIn(0f, 1f)
}

class ConfettiState {
    val particles: MutableList<ConfettiParticle> = mutableListOf()

    fun emit(canvas: IntSize, palette: List<Color>, perSide: Int = 60) {
        val rng = Random(System.nanoTime())
        val baseY = canvas.height.toFloat() + 24f
        repeat(perSide) { spawn(ConfettiParticle.Side.Left, canvas, palette, rng, baseY) }
        repeat(perSide) { spawn(ConfettiParticle.Side.Right, canvas, palette, rng, baseY) }
    }

    private fun spawn(
        side: ConfettiParticle.Side,
        canvas: IntSize,
        palette: List<Color>,
        rng: Random,
        baseY: Float,
    ) {
        val xAnchor = if (side == ConfettiParticle.Side.Left) {
            rng.nextFloat() * canvas.width * 0.3f
        } else {
            canvas.width * 0.7f + rng.nextFloat() * canvas.width * 0.3f
        }
        val dirX = if (side == ConfettiParticle.Side.Left) 1f else -1f
        val angle = rng.nextFloat() * 0.6f - 0.3f
        val speed = rng.nextFloat() * 400f + 800f
        val vx = (sin(angle) * speed + dirX * 150f) * if (side == ConfettiParticle.Side.Right) -1f else 1f
        val vy = -cos(angle) * speed

        val width = rng.nextFloat() * 6f + 8f
        val height = rng.nextFloat() * 3f + 4f
        val color = palette[rng.nextInt(palette.size)]

        particles.add(
            ConfettiParticle(
                x = xAnchor,
                y = baseY,
                vx = vx,
                vy = vy,
                rotationDeg = rng.nextFloat() * 360f,
                angularVelocityDeg = (rng.nextFloat() * 1440f - 720f) *
                    if (side == ConfettiParticle.Side.Right) 1f else -1f,
                color = color,
                width = width,
                height = height,
                lifeSeconds = rng.nextFloat() * 0.6f + 1.2f,
                originSide = side,
            )
        )
    }

    fun step(deltaSec: Float, canvas: IntSize) {
        if (particles.isEmpty()) return
        val gravity = 900f
        val airDrag = 0.5f
        val iterator = particles.listIterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            val keep = p.step(deltaSec, gravity, airDrag, canvas)
            if (!keep) iterator.remove()
        }
    }
}

@Composable
fun ConfettiOverlay(
    state: ConfettiState,
    modifier: Modifier = Modifier,
    onSize: (IntSize) -> Unit = {},
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var particleCount by remember { mutableStateOf(0) }

    LaunchedEffect(state) {
        var lastNanos = 0L
        while (true) {
            val nowNanos = awaitFrame()
            if (lastNanos == 0L) lastNanos = nowNanos
            val deltaSec = ((nowNanos - lastNanos) / 1_000_000_000.0f).coerceAtMost(0.05f)
            lastNanos = nowNanos
            if (state.particles.isNotEmpty()) {
                state.step(deltaSec, canvasSize)
                particleCount = state.particles.size
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidthPx = this.size.width
        val canvasHeightPx = this.size.height
        val size = IntSize(canvasWidthPx.toInt(), canvasHeightPx.toInt())
        if (size != canvasSize) {
            canvasSize = size
            onSize(size)
        }
        // particleCount is read here so that adding/removing particles
        // outside this Composable (e.g. via emit()) triggers a
        // recomposition, even though we iterate the underlying
        // MutableList directly.
        @Suppress("UNUSED_VARIABLE") val _count = particleCount
        val list = state.particles
        for (i in 0 until list.size) {
            val p = list[i]
            rotate(degrees = p.rotationDeg, pivot = Offset(p.x, p.y)) {
                drawRect(
                    color = p.color.copy(alpha = p.alpha),
                    topLeft = Offset(p.x - p.width / 2f, p.y - p.height / 2f),
                    size = Size(p.width, p.height),
                )
            }
        }
    }
}
