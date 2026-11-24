package fr.loic.seances.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random

private class Piece(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val size: Float, var rot: Float, val vr: Float, val color: Color, val round: Boolean, var flip: Float,
)

/** Pluie de confettis, une fois, par-dessus l'écran de fin (même rendu que la web app). */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val colors = listOf(Palette.work, Palette.rest, Palette.prepare, Color(0xFFFFD54F), Color(0xFFFF6B9A), Palette.text)
    val pixel = Palette.isArcade // carrés alignés sur une grille, sans rotation
    val density = LocalDensity.current.density
    var pieces by remember { mutableStateOf<List<Piece>?>(null) }
    var age by remember { mutableFloatStateOf(0f) }
    var frame by remember { mutableFloatStateOf(0f) } // force le redessin à chaque image

    Canvas(modifier) {
        val list = pieces ?: List(140) { i ->
            Piece(
                x = size.width * (0.15f + 0.7f * Random.nextFloat()), y = size.height * 0.32f + Random.nextFloat() * 30 * density,
                vx = (Random.nextFloat() - 0.5f) * 9 * density, vy = (-7 - Random.nextFloat() * 9) * density,
                size = (5 + Random.nextFloat() * 6) * density, rot = Random.nextFloat() * 6, vr = (Random.nextFloat() - 0.5f) * 0.35f,
                color = colors[i % colors.size], round = !pixel && Random.nextFloat() < 0.3f, flip = Random.nextFloat() * 6,
            )
        }.also { pieces = it }
        frame // lu pour redessiner
        val alpha = (4.2f - age).coerceIn(0f, 1f)
        if (alpha <= 0f) return@Canvas
        list.forEach { q ->
            if (pixel) {
                val g = 6 * density
                val w = if (q.size > 8 * density) 2 * g else g
                drawRect(q.color.copy(alpha = alpha), Offset(Math.round(q.x / g) * g, Math.round(q.y / g) * g), Size(w, g))
                return@forEach
            }
            rotate(Math.toDegrees(q.rot.toDouble()).toFloat(), Offset(q.x, q.y)) {
                if (q.round) {
                    drawCircle(q.color.copy(alpha = alpha), q.size / 2.4f, Offset(q.x, q.y))
                } else {
                    val h = q.size / 2 * abs(cos(q.flip)) + 0.5f
                    drawRect(q.color.copy(alpha = alpha), Offset(q.x - q.size / 2, q.y - h / 2), Size(q.size, h))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        var last = start
        while (age < 4.3f) {
            withFrameNanos { now ->
                // Physique calée sur 60 images/s, comme la web app.
                val steps = ((now - last) / 16_666_667f).coerceIn(0.5f, 3f)
                last = now
                age = (now - start) / 1e9f
                pieces?.forEach { q ->
                    q.vy += 0.32f * density * steps
                    q.vx *= 0.985f
                    q.vy *= 0.985f
                    q.x += q.vx * steps
                    q.y += q.vy * steps
                    q.rot += q.vr * steps
                    q.flip += 0.12f * steps
                }
                frame = age
            }
        }
    }
}

/** Pastille de record (« 🏆 Ta plus longue séance ! ») qui apparaît avec un petit rebond. */
@Composable
fun RecordChip(text: String, delayMs: Int) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Text(
        text,
        modifier = Modifier
            .padding(vertical = 3.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = scale.value.coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(50))
            .background(Palette.surfaceHigh)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Palette.text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
    )
}
