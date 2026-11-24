package fr.loic.seances.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.loic.seances.data.BodyMap
import fr.loic.seances.data.HistoryEntry
import fr.loic.seances.data.HistoryOps
import fr.loic.seances.data.Muscle

private val PERIODS = listOf(7, 30, 90)
private const val NEGLECTED_DAYS = 7
private val Warn: Color get() = Palette.warn

private class Zone(val muscle: Muscle?, val path: Path, val region: android.graphics.Region)

private fun zones(parts: List<BodyMap.Part>): List<Zone> {
    val clip = android.graphics.Region(0, 0, BodyMap.WIDTH.toInt() + 1, BodyMap.HEIGHT.toInt() + 1)
    return parts.map { p ->
        val path = PathParser().parsePathString(p.path).toPath()
        Zone(p.muscle, path, android.graphics.Region().apply { setPath(path.asAndroidPath(), clip) })
    }
}

private fun lastLabel(s: HistoryOps.MuscleStat) =
    if (s.lastDate == null) "jamais travaillé" else "dernière fois ${HistoryOps.sinceLabel(s.lastDate)}"

private fun neglected(s: HistoryOps.MuscleStat, now: Long) =
    s.lastDate == null || now - s.lastDate > NEGLECTED_DAYS * 86_400_000L

/** Silhouette coloriée selon le nombre d'exercices de chaque groupe ; un appui sélectionne une zone. */
@Composable
private fun Body(
    parts: List<BodyMap.Part>,
    level: (Muscle) -> Float,
    selected: Muscle?,
    onSelect: (Muscle?) -> Unit,
    modifier: Modifier,
) {
    val list = remember(parts) { zones(parts) }
    val base = Palette.surfaceHigh
    val muscleBase = lerp(Palette.surfaceHigh, Palette.textMuted, 0.3f)
    Canvas(
        modifier
            .aspectRatio(BodyMap.WIDTH / BodyMap.HEIGHT)
            .pointerInput(list, selected) {
                detectTapGestures { pos ->
                    val k = size.width / BodyMap.WIDTH
                    val x = (pos.x / k).toInt()
                    val y = (pos.y / k).toInt()
                    val hit = list.lastOrNull { it.muscle != null && it.region.contains(x, y) }?.muscle ?: return@detectTapGestures
                    onSelect(if (hit == selected) null else hit)
                }
            },
    ) {
        val k = size.width / BodyMap.WIDTH
        scale(k, k, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            list.forEach { z ->
                val m = z.muscle
                val l = if (m != null) level(m) else 0f
                drawPath(z.path, if (m == null) base else muscleBase)
                if (l > 0f) drawPath(z.path, Palette.work.copy(alpha = l))
                drawPath(z.path, Palette.background, style = Stroke(width = 1.3f, join = StrokeJoin.Round))
            }
            list.filter { it.muscle != null && it.muscle == selected }.forEach {
                drawPath(it.path, Palette.text, style = Stroke(width = 2.2f, join = StrokeJoin.Round))
            }
        }
    }
}

/** Onglet Muscles de l'historique (même contenu que la web app). */
fun LazyListScope.musclesTab(
    history: List<HistoryEntry>,
    days: Int,
    onDays: (Int) -> Unit,
    selected: Muscle?,
    onSelect: (Muscle?) -> Unit,
) {
    val now = System.currentTimeMillis()
    val stats = HistoryOps.muscleStats(history, days, now)
    val max = maxOf(1, stats.maxOf { it.count })
    val level = { m: Muscle -> stats.first { it.muscle == m }.count.let { if (it > 0) 0.3f + 0.7f * it / max else 0f } }
    val untracked = history.count { it.muscles.isEmpty() }

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PERIODS.forEach { d ->
                Text(
                    "$d jours",
                    Modifier
                        .clip(themedShape(10.dp))
                        .background(if (d == days) Palette.work else Color.Transparent)
                        .border(1.dp, if (d == days) Palette.work else Palette.surfaceHigh, themedShape(10.dp))
                        .clickable { onDays(d) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (d == days) Palette.onWork else Palette.text,
                    fontSize = 13.sp,
                    fontWeight = if (d == days) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
    item {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(themedShape(20.dp))
                .background(Palette.surface)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Face" to BodyMap.front, "Dos" to BodyMap.back).forEach { (name, parts) ->
                    Column(Modifier.weight(1f).widthIn(max = 190.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Body(parts, level, selected, onSelect, Modifier.fillMaxWidth(0.9f))
                        Text(name, color = Palette.textMuted, fontSize = 11.sp)
                    }
                }
            }
            val sel = selected?.let { m -> stats.first { it.muscle == m } }
            Text(
                if (sel == null) {
                    buildAnnotatedString { append("Touche une zone pour le détail") }
                } else {
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Palette.text)) { append(sel.muscle.label) }
                        append(" : ${sel.count} exo${if (sel.count > 1) "s" else ""} sur $days jours · ${lastLabel(sel)}")
                    }
                },
                color = if (sel == null) Palette.textMuted else Palette.text,
                fontSize = 13.sp, textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rien", color = Palette.textMuted, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .clip(themedShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(Palette.work.copy(alpha = 0.3f), Palette.work))),
                )
                Spacer(Modifier.width(8.dp))
                Text("Le plus", color = Palette.textMuted, fontSize = 11.sp)
            }
        }
    }
    items(stats, key = { it.muscle.name }) { s ->
        val isSel = s.muscle == selected
        Column(
            Modifier
                .fillMaxWidth()
                .clip(themedShape(16.dp))
                .background(Palette.surface)
                .then(if (isSel) Modifier.border(2.dp, Palette.work, themedShape(16.dp)) else Modifier)
                .clickable { onSelect(if (isSel) null else s.muscle) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row {
                Text(
                    (if (s.muscle == Muscle.CARDIO) "❤️ " else "") + s.muscle.label,
                    Modifier.weight(1f), color = Palette.text, fontWeight = FontWeight.SemiBold,
                )
                Text("${s.count} exo${if (s.count > 1) "s" else ""}", color = Palette.textMuted, fontSize = 13.sp)
            }
            Box(
                Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(themedShape(4.dp))
                    .background(Palette.surfaceHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(s.count / max.toFloat())
                        .fillMaxHeight()
                        .clip(themedShape(4.dp))
                        .background(Palette.work),
                )
            }
            val warn = neglected(s, now)
            Text(
                (if (warn) "⚠️ À travailler · " else "") + lastLabel(s),
                color = if (warn) Warn else Palette.textMuted, fontSize = 13.sp,
            )
        }
    }
    item {
        Text(
            "Le cardio ne figure pas sur le schéma." +
                if (untracked > 0) " $untracked séance${if (untracked > 1) "s" else ""} d'avant cette version ne " +
                    "${if (untracked > 1) "sont" else "est"} pas comptée${if (untracked > 1) "s" else ""}." else "",
            Modifier.fillMaxWidth(), color = Palette.textMuted, fontSize = 11.sp, textAlign = TextAlign.Center,
        )
    }
}
