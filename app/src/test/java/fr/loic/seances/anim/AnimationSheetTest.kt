package fr.loic.seances.anim

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Vérifie que chaque pose reste dans le cadre, et exporte les articulations
 * de chaque exercice (8 instants par cycle, vérification sur 64) dans build/animation-joints.txt,
 * pour pouvoir les dessiner hors émulateur.
 */
class AnimationSheetTest {
    @Test
    fun exportJointsAndCheckBounds() {
        val exercises = Anims.entries
        val sb = StringBuilder()
        val errors = mutableListOf<String>()
        exercises.forEach { ex ->
            val a = ex.animation
            val props = a.props.joinToString(";") { p ->
                when (p) {
                    is Prop.Chair -> "chair:${p.x}:${p.seatY}:${p.width}:${p.backLeft}"
                    is Prop.Wall -> "wall:${p.x}"
                }
            }
            sb.append("EX|${ex.label}|${a.view}|${a.weight}|${a.ground}|$props|${a.weightBothHands}|${ex.id}\n")
            for (c in 0 until 64) {
                val j = computeJoints(a.poseAt(a.cycleMs * c / 64f), a.view, clampToGround = a.ground)
                val pts = listOf(
                    j.pelvis, j.neck, j.head, j.nShoulder, j.fShoulder, j.nHip, j.fHip,
                    j.nElbow, j.nHand, j.fElbow, j.fHand, j.nKnee, j.nFoot, j.fKnee, j.fFoot,
                )
                pts.forEach { p ->
                    if (!(p.x in -4f..104f && p.y in -7f..100f)) errors += "${ex.label} sort du cadre : $p"
                    if (a.ground && p.y > 91f) errors += "${ex.label} passe sous le sol : $p (instant $c/64)"
                }
                if (c % 8 == 0) sb.append(pts.joinToString(";") { "${it.x},${it.y}" }).append('\n')
            }
        }
        assertTrue(errors.distinctBy { it.substringBefore(" :") }.joinToString("\n"), errors.isEmpty())
        File("build/animation-joints.txt").apply { parentFile?.mkdirs() }.writeText(sb.toString())
    }
}
