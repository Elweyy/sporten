package fr.loic.seances.anim

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/*
 * Bonhomme articulé dessiné dans une boîte 100 x 100 (avec une marge), sol à y = 90.
 *
 * Conventions d'angles (en degrés) :
 *  - Membres : 0 = vers le bas, 90 = vers l'avant (droite de l'écran), 180 = vers le haut,
 *    -90 = vers l'arrière (gauche).
 *  - Buste : 0 = droit, 90 = penché complètement vers l'avant (droite), -90 = couché tête à gauche.
 *
 * "near" = membres côté spectateur (dessinés devant, couleur pleine),
 * "far"  = membres côté opposé (dessinés derrière, couleur atténuée).
 * En vue de face / de dessus, near = côté droit de l'écran, far = côté gauche.
 */

enum class ViewMode { SIDE, FRONT, TOP }

/** Décor dessiné derrière le bonhomme (coordonnées de la boîte 100 x 100, sol à 90). */
sealed interface Prop {
    /**
     * Chaise vue de profil : assise de [x] à [x] + [width] à la hauteur [seatY],
     * dossier du côté gauche si [backLeft], sinon à droite.
     */
    data class Chair(val x: Float, val seatY: Float = 71f, val width: Float = 16f, val backLeft: Boolean = true) : Prop

    /** Mur vertical à l'abscisse [x]. */
    data class Wall(val x: Float) : Prop

    /** Trait fixe (cadre de machine, banc, barre, rail…). */
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val width: Float = 2f) : Prop

    /** Cercle fixe, contour seul (poulie, roue). */
    data class Circle(val x: Float, val y: Float, val r: Float, val width: Float = 1.6f) : Prop

    /**
     * Trait entre un point fixe et une articulation qui bouge : câble de poulie (fin)
     * ou bras de levier d'une machine (épais).
     */
    data class Link(val x: Float, val y: Float, val to: JointRef, val width: Float = 1f) : Prop
}

/** Articulation à laquelle un [Prop.Link] est accroché. */
enum class JointRef { N_HAND, F_HAND, N_ELBOW, F_ELBOW, N_FOOT, F_FOOT, N_KNEE }

fun Joints.get(ref: JointRef): Offset = when (ref) {
    JointRef.N_HAND -> nHand
    JointRef.F_HAND -> fHand
    JointRef.N_ELBOW -> nElbow
    JointRef.F_ELBOW -> fElbow
    JointRef.N_FOOT -> nFoot
    JointRef.F_FOOT -> fFoot
    JointRef.N_KNEE -> nKnee
}

enum class Weight {
    NONE,

    /** Haltère vu de bout (cercle) dans chaque main. */
    END_ON,

    /** Haltère vu de côté : barre perpendiculaire à l'avant-bras. */
    BAR,

    /** Un seul poids tenu à deux mains. */
    SINGLE,
}

data class Pose(
    val px: Float = 50f,
    val py: Float = 54f,
    val torso: Float = 0f,
    val na1: Float = 0f, val na2: Float = 0f,
    val fa1: Float = 0f, val fa2: Float = 0f,
    val nl1: Float = 0f, val nl2: Float = 0f,
    val fl1: Float = 0f, val fl2: Float = 0f,
    val armScale: Float = 1f,
    val legScale: Float = 1f,
    val torsoScale: Float = 1f,
) {
    fun lerp(o: Pose, t: Float) = Pose(
        l(px, o.px, t), l(py, o.py, t), l(torso, o.torso, t),
        l(na1, o.na1, t), l(na2, o.na2, t), l(fa1, o.fa1, t), l(fa2, o.fa2, t),
        l(nl1, o.nl1, t), l(nl2, o.nl2, t), l(fl1, o.fl1, t), l(fl2, o.fl2, t),
        l(armScale, o.armScale, t), l(legScale, o.legScale, t), l(torsoScale, o.torsoScale, t),
    )

    private fun l(a: Float, b: Float, t: Float) = a + (b - a) * t
}

data class ExerciseAnimation(
    val keyframes: List<Pose>,
    /** Durée de transition entre deux poses clés. */
    val segmentMs: Int = 700,
    val view: ViewMode = ViewMode.SIDE,
    val weight: Weight = Weight.NONE,
    val ground: Boolean = true,
    /** Décor fixe : chaise, mur. */
    val props: List<Prop> = emptyList(),
    /** false = haltère seulement dans la main côté spectateur (exercices unilatéraux). */
    val weightBothHands: Boolean = true,
) {
    val cycleMs: Int get() = segmentMs * keyframes.size

    /** Pose à l'instant [timeMs], en bouclant sur les poses clés. */
    fun poseAt(timeMs: Float): Pose {
        if (keyframes.size == 1) return keyframes[0]
        val pos = (timeMs % cycleMs) / segmentMs
        val i = floor(pos).toInt()
        val frac = pos - i
        val eased = frac * frac * (3 - 2 * frac) // smoothstep
        return keyframes[i].lerp(keyframes[(i + 1) % keyframes.size], eased)
    }
}

internal const val TORSO = 30f
private const val NECK_TO_HEAD = 8.5f
private const val HEAD_R = 6f
internal const val UPPER_ARM = 14f
internal const val FOREARM = 13f
internal const val THIGH = 18f
internal const val SHIN = 18f
private const val SHOULDER_HALF = 7f
private const val HIP_HALF = 4f
internal const val GROUND_Y = 90f
private const val VIEW_SIZE = 108f

/** Positions des articulations dans la boîte 100 x 100. */
data class Joints(
    val pelvis: Offset, val neck: Offset, val head: Offset,
    val nShoulder: Offset, val fShoulder: Offset, val nHip: Offset, val fHip: Offset,
    val nElbow: Offset, val nHand: Offset, val fElbow: Offset, val fHand: Offset,
    val nKnee: Offset, val nFoot: Offset, val fKnee: Offset, val fFoot: Offset,
)

/**
 * Calcule les articulations. Avec [clampToGround], si un point passe sous le sol
 * (effet de l'interpolation entre deux poses), tout le corps est remonté d'autant.
 */
fun computeJoints(p: Pose, view: ViewMode, clampToGround: Boolean = false): Joints {
    val j = rawJoints(p, view)
    if (!clampToGround) return j
    val lowest = maxOf(
        maxOf(j.nFoot.y, j.fFoot.y, j.nKnee.y, j.fKnee.y),
        maxOf(j.nHand.y, j.fHand.y, j.nElbow.y, j.fElbow.y, j.pelvis.y),
    )
    val lift = lowest - (GROUND_Y + 0.5f)
    if (lift <= 0f) return j
    val d = Offset(0f, -lift)
    return Joints(
        j.pelvis + d, j.neck + d, j.head + d, j.nShoulder + d, j.fShoulder + d, j.nHip + d, j.fHip + d,
        j.nElbow + d, j.nHand + d, j.fElbow + d, j.fHand + d, j.nKnee + d, j.nFoot + d, j.fKnee + d, j.fFoot + d,
    )
}

private fun rawJoints(p: Pose, view: ViewMode): Joints {
    val pelvis = Offset(p.px, p.py)
    val torsoDir = Offset(sin(Math.toRadians(p.torso.toDouble())).toFloat(), -cos(Math.toRadians(p.torso.toDouble())).toFloat())
    val neck = pelvis + torsoDir * (TORSO * p.torsoScale)
    val head = neck + torsoDir * NECK_TO_HEAD
    val split = view != ViewMode.SIDE
    val perp = Offset(-torsoDir.y, torsoDir.x) // vers la droite quand le buste est droit
    val nShoulder = if (split) neck + perp * SHOULDER_HALF else neck
    val fShoulder = if (split) neck - perp * SHOULDER_HALF else neck
    val nHip = if (split) pelvis + perp * HIP_HALF else pelvis
    val fHip = if (split) pelvis - perp * HIP_HALF else pelvis
    val ua = UPPER_ARM * p.armScale
    val fa = FOREARM * p.armScale
    val th = THIGH * p.legScale
    val sh = SHIN * p.legScale
    val nElbow = nShoulder + dir(p.na1) * ua
    val fElbow = fShoulder + dir(p.fa1) * ua
    val nKnee = nHip + dir(p.nl1) * th
    val fKnee = fHip + dir(p.fl1) * th
    return Joints(
        pelvis, neck, head, nShoulder, fShoulder, nHip, fHip,
        nElbow, nElbow + dir(p.na2) * fa, fElbow, fElbow + dir(p.fa2) * fa,
        nKnee, nKnee + dir(p.nl2) * sh, fKnee, fKnee + dir(p.fl2) * sh,
    )
}

private fun dir(deg: Float): Offset {
    val r = Math.toRadians(deg.toDouble())
    return Offset(sin(r).toFloat(), cos(r).toFloat())
}

/**
 * Affiche l'animation. [playing] = false fige le mouvement (pause).
 */
@Composable
fun StickFigure(
    animation: ExerciseAnimation,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    bodyColor: Color = fr.loic.seances.ui.Palette.text,
    accentColor: Color = fr.loic.seances.ui.Palette.work,
    groundColor: Color = bodyColor.copy(alpha = 0.33f),
) {
    var time by remember(animation) { mutableFloatStateOf(0f) }
    LaunchedEffect(animation, playing) {
        if (!playing) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                time += (now - last)
                last = now
            }
        }
    }
    Canvas(modifier) {
        drawFigure(animation, animation.poseAt(time), bodyColor, accentColor, groundColor)
    }
}

private fun DrawScope.drawFigure(
    anim: ExerciseAnimation,
    p: Pose,
    body: Color,
    accent: Color,
    groundColor: Color,
) {
    // Zone visible : x de -4 à 104, y de -8 à 100 (marge pour les bras tendus au-dessus de la tête).
    val s = min(size.width, size.height) / VIEW_SIZE
    val ox = (size.width - VIEW_SIZE * s) / 2f + 4f * s
    val oy = (size.height - VIEW_SIZE * s) / 2f + 8f * s
    fun pt(o: Offset) = Offset(ox + o.x * s, oy + o.y * s)
    val stroke = 4.2f * s

    val split = anim.view != ViewMode.SIDE
    val j = computeJoints(p, anim.view, clampToGround = anim.ground)
    val pelvis = j.pelvis; val neck = j.neck; val head = j.head
    val nShoulder = j.nShoulder; val fShoulder = j.fShoulder; val nHip = j.nHip; val fHip = j.fHip
    val nElbow = j.nElbow; val nHand = j.nHand; val fElbow = j.fElbow; val fHand = j.fHand
    val nKnee = j.nKnee; val nFoot = j.nFoot; val fKnee = j.fKnee; val fFoot = j.fFoot

    val farColor = if (split) body else body.copy(alpha = 0.45f)

    fun seg(a: Offset, b: Offset, c: Color) =
        drawLine(c, pt(a), pt(b), strokeWidth = stroke, cap = StrokeCap.Round)

    fun dumbbell(elbow: Offset, hand: Offset, c: Color) {
        when (anim.weight) {
            Weight.NONE -> Unit
            Weight.END_ON -> drawCircle(c, radius = 3.4f * s, center = pt(hand))
            Weight.SINGLE -> drawCircle(c, radius = 4.6f * s, center = pt(hand))
            Weight.BAR -> {
                val f = hand - elbow
                val len = f.getDistance().coerceAtLeast(0.001f)
                val n = Offset(-f.y / len, f.x / len)
                val a = hand + n * 4.5f
                val b = hand - n * 4.5f
                drawLine(c, pt(a), pt(b), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
                drawLine(c, pt(a - n * 0.8f), pt(a + n * 0.8f), strokeWidth = 4.5f * s, cap = StrokeCap.Round)
                drawLine(c, pt(b - n * 0.8f), pt(b + n * 0.8f), strokeWidth = 4.5f * s, cap = StrokeCap.Round)
            }
        }
    }

    if (anim.ground) {
        drawLine(groundColor, pt(Offset(4f, GROUND_Y + 2.2f)), pt(Offset(96f, GROUND_Y + 2.2f)), strokeWidth = 1.5f * s)
    }

    // Décor
    val propColor = groundColor.copy(alpha = (groundColor.alpha * 1.6f).coerceAtMost(1f))
    anim.props.forEach { prop ->
        when (prop) {
            is Prop.Chair -> {
                val seatT = 2.2f * s
                val y = prop.seatY + 1.4f
                val x0 = prop.x
                val x1 = prop.x + prop.width
                drawLine(propColor, pt(Offset(x0, y)), pt(Offset(x1, y)), strokeWidth = seatT, cap = StrokeCap.Round)
                drawLine(propColor, pt(Offset(x0 + 1f, y)), pt(Offset(x0 + 1f, GROUND_Y + 2.2f)), strokeWidth = 1.6f * s)
                drawLine(propColor, pt(Offset(x1 - 1f, y)), pt(Offset(x1 - 1f, GROUND_Y + 2.2f)), strokeWidth = 1.6f * s)
                val bx = if (prop.backLeft) x0 + 1f else x1 - 1f
                drawLine(propColor, pt(Offset(bx, y)), pt(Offset(bx, y - 22f)), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            }
            is Prop.Wall -> drawLine(
                propColor, pt(Offset(prop.x, -6f)), pt(Offset(prop.x, GROUND_Y + 2.2f)), strokeWidth = 2.4f * s,
            )
            is Prop.Line -> drawLine(
                propColor, pt(Offset(prop.x1, prop.y1)), pt(Offset(prop.x2, prop.y2)),
                strokeWidth = prop.width * s, cap = StrokeCap.Round,
            )
            is Prop.Circle -> drawCircle(
                propColor, radius = prop.r * s, center = pt(Offset(prop.x, prop.y)), style = Stroke(prop.width * s),
            )
            is Prop.Link -> drawLine(
                propColor, pt(Offset(prop.x, prop.y)), pt(j.get(prop.to)), strokeWidth = prop.width * s, cap = StrokeCap.Round,
            )
        }
    }

    // Côté opposé (derrière)
    seg(fHip, fKnee, farColor); seg(fKnee, fFoot, farColor)
    seg(fShoulder, fElbow, farColor); seg(fElbow, fHand, farColor)
    if (anim.weight != Weight.SINGLE && anim.weightBothHands) dumbbell(fElbow, fHand, if (split) accent else accent.copy(alpha = 0.55f))

    // Buste + tête
    seg(pelvis, neck, body)
    if (split) {
        seg(fShoulder, nShoulder, body)
        seg(fHip, nHip, body)
    }
    drawCircle(body, radius = HEAD_R * s, center = pt(head))

    // Côté spectateur (devant)
    seg(nHip, nKnee, body); seg(nKnee, nFoot, body)
    seg(nShoulder, nElbow, body); seg(nElbow, nHand, body)
    dumbbell(nElbow, nHand, accent)

    // Petit contour sombre sur la tête pour la lisibilité
    drawCircle(Color.Black.copy(alpha = 0.15f), radius = HEAD_R * s, center = pt(head), style = Stroke(0.8f * s))
}
