package fr.loic.seances.anim

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Animations des exercices de salle (machines, poulies, barre, banc).
 * Les machines sont dessinées avec des décors simples : traits ([Prop.Line]), poulies ([Prop.Circle])
 * et câbles ou bras de levier accrochés à une main ou un pied ([Prop.Link]).
 * Quand une main ou un pied doit rester sur la machine (barre, pédale, plateau), les angles
 * sont calculés par [reach] à partir de la position voulue.
 */
internal object GymAnims {

    private const val FLOOR = GROUND_Y + 2.2f

    // ---------- Outils de pose ----------

    private fun deg(rad: Double) = Math.toDegrees(rad).toFloat()
    private fun rad(deg: Float) = Math.toRadians(deg.toDouble())

    /** Ramène [a] à moins de 180° de [ref], pour que l'interpolation prenne le chemin court. */
    private fun near(a: Float, ref: Float): Float {
        var x = a
        while (x - ref > 180f) x -= 360f
        while (x - ref < -180f) x += 360f
        return x
    }

    /**
     * Angles (segment 1, segment 2) d'un membre qui part de ([ax], [ay]) et dont l'extrémité
     * doit arriver en ([bx], [by]). [bend] = +1 ou -1 choisit de quel côté plie l'articulation.
     */
    private fun reach(
        ax: Float, ay: Float, bx: Float, by: Float, l1: Float, l2: Float, bend: Int, ref: Float,
    ): Pair<Float, Float> {
        val dx = bx - ax
        val dy = by - ay
        val d = hypot(dx, dy).coerceIn(abs(l1 - l2) + 0.01f, l1 + l2 - 0.01f)
        val base = deg(atan2(dx.toDouble(), dy.toDouble()))
        val alpha = deg(acos(((l1 * l1 + d * d - l2 * l2) / (2 * l1 * d)).toDouble().coerceIn(-1.0, 1.0)))
        val a1 = base + bend * alpha
        val ex = ax + sin(rad(a1)).toFloat() * l1
        val ey = ay + cos(rad(a1)).toFloat() * l1
        val a2 = deg(atan2((bx - ex).toDouble(), (by - ey).toDouble()))
        return near(a1, ref) to near(a2, ref)
    }

    /** Épaule en vue de profil. */
    private fun Pose.shoulderX() = px + sin(rad(torso)).toFloat() * TORSO * torsoScale
    private fun Pose.shoulderY() = py - cos(rad(torso)).toFloat() * TORSO * torsoScale

    /** Les deux pieds en ([x], [y]) (vue de profil). */
    private fun Pose.legsTo(x: Float, y: Float, bend: Int = 1, ref: Float = 0f): Pose {
        val (a, b) = reach(px, py, x, y, THIGH, SHIN, bend, ref)
        return copy(nl1 = a, nl2 = b, fl1 = a - 2f, fl2 = b - 2f)
    }

    /** Les deux pieds en ([x], [y]), genoux vers le haut (le pli qui donne le genou le plus haut). */
    private fun Pose.kneesUpTo(x: Float, y: Float): Pose {
        val a = legsTo(x, y, bend = 1)
        val b = legsTo(x, y, bend = -1)
        fun kneeY(p: Pose) = p.py + cos(rad(p.nl1)).toFloat() * THIGH
        return if (kneeY(a) <= kneeY(b)) a else b
    }

    /** Le pied côté spectateur en ([x], [y]). */
    private fun Pose.nearLegTo(x: Float, y: Float, bend: Int = 1, ref: Float = 0f): Pose {
        val (a, b) = reach(px, py, x, y, THIGH, SHIN, bend, ref)
        return copy(nl1 = a, nl2 = b)
    }

    /** Le pied côté opposé en ([x], [y]). */
    private fun Pose.farLegTo(x: Float, y: Float, bend: Int = 1, ref: Float = 0f): Pose {
        val (a, b) = reach(px, py, x, y, THIGH, SHIN, bend, ref)
        return copy(fl1 = a, fl2 = b)
    }

    /** Les deux mains en ([x], [y]) (vue de profil). */
    private fun Pose.armsTo(x: Float, y: Float, bend: Int = 1, ref: Float = 0f): Pose {
        val (a, b) = reach(shoulderX(), shoulderY(), x, y, UPPER_ARM, FOREARM, bend, ref)
        return copy(na1 = a, na2 = b, fa1 = a - 2f, fa2 = b - 2f)
    }

    // ---------- Décors ----------

    /** Banc vu de profil : plateau de [x0] à [x1] à la hauteur [top], deux pieds. */
    private fun bench(x0: Float, x1: Float, top: Float) = listOf(
        Prop.Line(x0, top, x1, top, 3.2f),
        Prop.Line(x0 + 3f, top, x0 + 3f, FLOOR, 1.6f),
        Prop.Line(x1 - 3f, top, x1 - 3f, FLOOR, 1.6f),
    )

    /** Montant vertical de machine. */
    private fun post(x: Float, top: Float, width: Float = 2.4f) = Prop.Line(x, top, x, FLOOR, width)

    /** Poulie en ([x], [y]) reliée à la main par un câble. */
    private fun pulley(x: Float, y: Float, to: JointRef = JointRef.N_HAND) =
        listOf(Prop.Circle(x, y, 1.8f), Prop.Link(x, y, to, 1f))

    /** Siège de machine : assise de [x0] à [x1] à la hauteur [y], un pied central. */
    private fun seat(x0: Float, x1: Float, y: Float) = listOf(
        Prop.Line(x0, y, x1, y, 3.2f),
        Prop.Line((x0 + x1) / 2f, y, (x0 + x1) / 2f, FLOOR, 2f),
    )

    /** Assis sur un siège de machine, de profil, cuisses à l'horizontale. */
    private val seated = Pose(px = 40f, py = 71.5f, nl1 = 88f, nl2 = 0f, fl1 = 86f, fl2 = 0f)

    // ---------- Pectoraux ----------

    val benchPress = run {
        val lying = Pose(px = 62f, py = 70f, torso = -90f).legsTo(80f, 90f, bend = 1)
        // En bas : coude sous le niveau du buste, côté pieds, avant-bras vertical, barre sur le bas des pecs.
        // En haut : bras tendus à la verticale des épaules.
        ExerciseAnimation(
            listOf(
                lying.copy(na1 = 70f, na2 = 172f, fa1 = 68f, fa2 = 170f),
                lying.copy(na1 = 176f, na2 = 178f, fa1 = 174f, fa2 = 176f),
            ),
            segmentMs = 900, weight = Weight.SINGLE,
            props = bench(20f, 68f, 73.5f) + post(27f, 38f, 1.8f),
        )
    }

    val chestPress = run {
        val base = seated.copy(torso = -8f)
        ExerciseAnimation(
            listOf(base.armsTo(46f, 44f, bend = -1), base.armsTo(60f, 43f, bend = -1)),
            segmentMs = 850,
            props = seat(30f, 52f, 74f) + Prop.Line(31f, 74f, 27f, 38f, 3.4f) +
                Prop.Link(44f, 12f, JointRef.N_HAND, 2.2f),
        )
    }

    val pecDeck = run {
        val base = Pose(px = 50f, py = 66f, nl1 = 25f, nl2 = 0f, fl1 = -25f, fl2 = 0f, legScale = 0.7f)
        ExerciseAnimation(
            listOf(
                base.copy(na1 = 90f, na2 = 180f, fa1 = -90f, fa2 = -180f),
                base.copy(na1 = 90f, na2 = 180f, fa1 = -90f, fa2 = -180f, armScale = 0.2f),
            ),
            segmentMs = 900, view = ViewMode.FRONT,
            props = listOf(
                Prop.Line(38f, 68.5f, 62f, 68.5f, 3.2f),
                Prop.Line(50f, 68.5f, 50f, FLOOR, 2f),
                Prop.Line(50f, 64f, 50f, 30f, 9f),
                Prop.Line(16f, 8f, 84f, 8f, 2.4f),
                Prop.Link(76f, 8f, JointRef.N_ELBOW, 2.2f),
                Prop.Link(24f, 8f, JointRef.F_ELBOW, 2.2f),
            ),
        )
    }

    val cableCrossover = run {
        val open = standFront().copy(na1 = 125f, na2 = 112f, fa1 = -125f, fa2 = -112f)
        val (a, b) = reach(57f, 24f, 51f, 58f, UPPER_ARM, FOREARM, -1, 0f)
        val closed = standFront().copy(na1 = a, na2 = b, fa1 = -a, fa2 = -b)
        ExerciseAnimation(
            listOf(open, closed),
            segmentMs = 900, view = ViewMode.FRONT,
            props = listOf(post(6f, 2f), post(94f, 2f)) +
                pulley(94f, 5f, JointRef.N_HAND) + pulley(6f, 5f, JointRef.F_HAND),
        )
    }

    // ---------- Dos ----------

    val latPulldown = run {
        val base = seated.copy(torso = -10f)
        ExerciseAnimation(
            listOf(base.armsTo(41f, 16f, bend = -1, ref = 180f), base.armsTo(41f, 40f, bend = -1, ref = 180f)),
            segmentMs = 900,
            props = seat(30f, 52f, 74f) + Prop.Line(53f, 68f, 64f, 68f, 3.4f) +
                post(22f, 2f) + Prop.Line(22f, 2f, 44f, 2f, 2.4f) + pulley(41f, 3f),
        )
    }

    val seatedCableRow = run {
        val catch = Pose(px = 34f, py = 78f, torso = 20f).legsTo(66f, 82f, bend = 1)
        val finish = catch.copy(torso = -6f)
        ExerciseAnimation(
            // Bras tendus vers la poulie, puis coudes tirés vers l'arrière le long du corps, mains au nombril.
            listOf(
                catch.copy(na1 = 80f, na2 = 86f, fa1 = 78f, fa2 = 84f),
                finish.copy(na1 = -32f, na2 = 84f, fa1 = -34f, fa2 = 82f),
            ),
            segmentMs = 900,
            props = listOf(Prop.Line(22f, 80.5f, 46f, 80.5f, 3.2f), Prop.Line(70f, 70f, 70f, FLOOR, 2.4f)) +
                post(82f, 74f) + pulley(82f, 76f),
        )
    }

    val pullUp = run {
        val legs = Pose(nl1 = 15f, nl2 = -50f, fl1 = 13f, fl2 = -52f)
        val hang = legs.copy(px = 51f, py = 62f).armsTo(53f, 8f, bend = 1, ref = 180f)
        val top = legs.copy(px = 47f, py = 46f, torso = 6f).armsTo(53f, 8f, bend = 1, ref = 180f)
        ExerciseAnimation(
            listOf(hang, top),
            segmentMs = 900, ground = false,
            props = listOf(Prop.Line(28f, 7f, 78f, 7f, 2.4f), Prop.Line(30f, 7f, 30f, -6f, 2f), Prop.Line(76f, 7f, 76f, -6f, 2f)),
        )
    }

    val backExtension = run {
        val up = Pose(px = 45f, py = 50f, torso = 50f, nl1 = -50f, nl2 = -50f, fl1 = -52f, fl2 = -52f, na1 = -40f, na2 = 130f, fa1 = -42f, fa2 = 128f)
        val down = up.copy(torso = 150f, na1 = -140f, na2 = 30f, fa1 = -142f, fa2 = 28f)
        ExerciseAnimation(
            listOf(up, down),
            segmentMs = 1000,
            props = listOf(
                Prop.Line(47f, 53f, 40.5f, 58.5f, 4.5f),
                Prop.Line(14f, 72f, 20f, 77f, 3.6f),
                Prop.Line(42f, 57f, 34f, FLOOR, 2f),
                Prop.Line(18f, 76f, 18f, FLOOR, 2f),
                Prop.Line(12f, FLOOR - 1f, 46f, FLOOR - 1f, 2.4f),
            ),
        )
    }

    // ---------- Épaules ----------

    val machineShoulderPress = run {
        val base = seated
        ExerciseAnimation(
            // En bas : coudes sous les poignées, poignées à hauteur d'épaules. En haut : bras tendus au-dessus de la tête.
            listOf(
                base.copy(na1 = 25f, na2 = 175f, fa1 = 23f, fa2 = 173f),
                base.copy(na1 = 172f, na2 = 176f, fa1 = 170f, fa2 = 174f),
            ),
            segmentMs = 850,
            props = seat(30f, 52f, 74f) + Prop.Line(33f, 74f, 33f, 26f, 3.4f) + post(26f, 20f),
        )
    }

    val facePull = run {
        val base = Pose(px = 46f, torso = -4f, nl1 = 10f, nl2 = 4f, fl1 = -12f, fl2 = -8f)
        ExerciseAnimation(
            listOf(base.armsTo(70f, 26f, bend = -1, ref = 90f), base.copy(armScale = 0.8f).armsTo(48f, 16f, bend = -1, ref = 90f)),
            segmentMs = 850,
            props = listOf(post(90f, 2f)) + pulley(90f, 26f),
        )
    }

    // ---------- Bras ----------

    val tricepsPushdown = run {
        val base = Pose(px = 42f, torso = 10f, na1 = 5f, fa1 = 3f)
        ExerciseAnimation(
            listOf(base.copy(na2 = 150f, fa2 = 148f), base.copy(na2 = 10f, fa2 = 8f)),
            segmentMs = 800,
            props = listOf(post(74f, 2f), Prop.Line(74f, 3f, 64f, 3f, 2f)) + pulley(64f, 4f),
        )
    }

    val cableCurl = run {
        val down = Pose(px = 44f, na1 = 6f, na2 = 8f, fa1 = 4f, fa2 = 6f)
        ExerciseAnimation(
            listOf(down, down.copy(na1 = 10f, na2 = 160f, fa1 = 8f, fa2 = 158f)),
            segmentMs = 800,
            props = listOf(post(80f, 30f)) + pulley(76f, 86f),
        )
    }

    val barDips = run {
        val legs = Pose(nl1 = 25f, nl2 = -100f, fl1 = 23f, fl2 = -102f)
        val up = legs.copy(px = 53f, py = 54f, torso = 5f).armsTo(55f, 50f, bend = -1)
        val down = legs.copy(px = 46f, py = 66f, torso = 20f).armsTo(55f, 50f, bend = -1)
        ExerciseAnimation(
            listOf(up, down),
            segmentMs = 850,
            props = listOf(Prop.Line(40f, 51f, 68f, 51f, 2.4f), post(43f, 51f, 2f), post(65f, 51f, 2f)),
        )
    }

    // ---------- Jambes ----------

    val legPress = run {
        val base = Pose(px = 40f, py = 74f, torso = -55f, na1 = 10f, na2 = 60f, fa1 = 8f, fa2 = 58f)
        ExerciseAnimation(
            listOf(base.legsTo(64f, 50f, bend = 1, ref = 120f), base.legsTo(52f, 62f, bend = 1, ref = 120f)),
            segmentMs = 950,
            props = listOf(
                Prop.Line(30f, 77.5f, 50f, 77.5f, 3.2f),
                Prop.Line(30f, 77.5f, 12f, 64f, 3.4f),
                Prop.Line(40f, 78f, 40f, FLOOR, 2f),
                Prop.Line(60f, 82f, 96f, 46f, 2f),
                Prop.Link(90f, 24f, JointRef.N_FOOT, 3f),
                post(90f, 24f, 2f),
            ),
        )
    }

    val legExtension = run {
        val base = seated.copy(torso = -5f, na1 = 10f, na2 = 20f, fa1 = 8f, fa2 = 18f)
        ExerciseAnimation(
            listOf(base, base.copy(nl2 = 85f, fl2 = 83f)),
            segmentMs = 850,
            props = seat(28f, 58f, 74.5f) + Prop.Line(30f, 74f, 27f, 40f, 3.4f) + Prop.Link(60f, 70f, JointRef.N_FOOT, 2.6f),
        )
    }

    val legCurl = run {
        val base = Pose(px = 58f, py = 72f, torso = -88f, na1 = -80f, na2 = -30f, fa1 = -82f, fa2 = -32f, nl1 = 90f, fl1 = 88f)
        ExerciseAnimation(
            listOf(base.copy(nl2 = 90f, fl2 = 88f), base.copy(nl2 = 200f, fl2 = 198f)),
            segmentMs = 850,
            props = bench(18f, 78f, 75.5f) + Prop.Link(78f, 70f, JointRef.N_FOOT, 2.6f),
        )
    }

    val barbellSquat = run {
        val up = Pose(px = 48f, na1 = 20f, na2 = -160f, fa1 = 18f, fa2 = -162f)
        val down = Pose(
            px = 36f, py = 70f, torso = 40f,
            nl1 = 80f, nl2 = -12f, fl1 = 77f, fl2 = -12f,
            na1 = 60f, na2 = -120f, fa1 = 58f, fa2 = -122f,
        )
        ExerciseAnimation(
            listOf(up, down),
            segmentMs = 1000, weight = Weight.SINGLE,
            props = listOf(post(20f, 8f, 2.2f), Prop.Line(20f, 22f, 25f, 22f, 2f)),
        )
    }

    val hipThrust = run {
        // Le haut du dos (omoplates, à 22 du bassin) reste appuyé sur le bord du banc ; pieds fixes au sol.
        fun at(torso: Float): Pose {
            val t = rad(torso)
            val px = 31f - sin(t).toFloat() * 22f
            val py = 70f + cos(t).toFloat() * 22f
            return Pose(px = px, py = py, torso = torso).kneesUpTo(74f, 90f).armsTo(px + 1f, py - 5f, bend = 1)
        }
        ExerciseAnimation(
            listOf(at(-58f), at(-90f)),
            segmentMs = 950, weight = Weight.SINGLE,
            props = bench(6f, 31f, 73f),
        )
    }

    // ---------- Abdos ----------

    val captainKneeRaise = run {
        val base = Pose(px = 46f, py = 50f, na1 = 0f, na2 = 90f, fa1 = -2f, fa2 = 88f)
        ExerciseAnimation(
            listOf(
                base.copy(nl1 = 2f, nl2 = 0f, fl1 = 0f, fl2 = -2f),
                base.copy(nl1 = 105f, nl2 = 10f, fl1 = 103f, fl2 = 8f),
            ),
            segmentMs = 850,
            props = listOf(
                Prop.Line(40.5f, 18f, 40.5f, 58f, 3.6f),
                Prop.Line(42f, 36f, 62f, 36f, 3.2f),
                Prop.Line(40.5f, 58f, 40.5f, FLOOR, 2f),
                Prop.Line(62f, 36f, 62f, FLOOR, 2f),
            ),
        )
    }

    // ---------- Cardio ----------

    val rowingMachine = run {
        val foot = 80f to 82f
        // Attaque : bras tendus vers la poignée. Poussée des jambes : bras toujours tendus.
        // Fin : buste en arrière, coudes tirés vers l'arrière le long du corps, mains au bas des côtes.
        val catch = Pose(px = 56f, py = 80f, torso = 25f).legsTo(foot.first, foot.second, bend = 1, ref = 90f)
            .copy(na1 = 58f, na2 = 52f, fa1 = 56f, fa2 = 50f)
        val mid = Pose(px = 45f, py = 80f, torso = 15f).legsTo(foot.first, foot.second, bend = 1, ref = 90f)
            .copy(na1 = 70f, na2 = 70f, fa1 = 68f, fa2 = 68f)
        val finish = Pose(px = 45f, py = 80f, torso = -20f).legsTo(foot.first, foot.second, bend = 1, ref = 90f)
            .copy(na1 = -40f, na2 = 95f, fa1 = -42f, fa2 = 93f)
        ExerciseAnimation(
            listOf(catch, mid, finish, mid),
            segmentMs = 550,
            props = listOf(
                Prop.Line(16f, 83.5f, 92f, 83.5f, 2.4f),
                Prop.Line(80f, 76f, 82f, 88f, 2.4f),
                Prop.Circle(93f, 77f, 5f),
                Prop.Link(91f, 73f, JointRef.N_HAND, 1f),
            ),
        )
    }

    val exerciseBike = run {
        val cx = 60f
        val cy = 78f
        val r = 7f
        val base = Pose(px = 46f, py = 52f, torso = 35f).armsTo(74f, 36f, bend = -1)
        // Cycliste tourné vers la droite : la pédale passe en haut, puis devant, puis en bas, puis derrière
        // (angle 0 = en bas, 90 = devant : on tourne donc dans le sens des angles décroissants).
        val frames = (0 until 8).map { i ->
            val a = rad(-i * 45f)
            val b = rad(-i * 45f + 180f)
            base
                .nearLegTo(cx + sin(a).toFloat() * r, cy + cos(a).toFloat() * r, bend = 1, ref = 30f)
                .farLegTo(cx + sin(b).toFloat() * r, cy + cos(b).toFloat() * r, bend = 1, ref = 30f)
        }
        ExerciseAnimation(
            frames,
            segmentMs = 140,
            props = listOf(
                Prop.Circle(cx, cy, r, 1.2f),
                Prop.Line(41f, 54.5f, 50f, 54.5f, 3f),
                Prop.Line(46f, 55f, 54f, 84f, 2.4f),
                Prop.Line(54f, 84f, cx, cy, 2.4f),
                Prop.Line(70f, FLOOR, 74f, 36f, 2.4f),
                Prop.Line(40f, FLOOR, 80f, FLOOR, 3f),
            ),
        )
    }

    val treadmill = run {
        val a = Pose(
            px = 46f, py = 56f, torso = 8f,
            nl1 = 35f, nl2 = -10f, fl1 = -25f, fl2 = -90f,
            na1 = -35f, na2 = 40f, fa1 = 35f, fa2 = 110f,
        )
        val b = a.copy(nl1 = -25f, nl2 = -90f, fl1 = 35f, fl2 = -10f, na1 = 35f, na2 = 110f, fa1 = -35f, fa2 = 40f)
        ExerciseAnimation(
            listOf(a, b),
            segmentMs = 330,
            props = listOf(
                Prop.Line(12f, FLOOR, 86f, FLOOR, 4f),
                Prop.Line(84f, FLOOR, 80f, 40f, 2.4f),
                Prop.Line(80f, 40f, 70f, 44f, 2.4f),
            ),
        )
    }

    // ---------- Registre ----------

    val entries: List<Anims.Entry> = listOf(
        Anims.Entry("bench_press", "Développé couché (barre)", benchPress),
        Anims.Entry("chest_press", "Presse pectorale", chestPress),
        Anims.Entry("pec_deck", "Butterfly (pec deck)", pecDeck),
        Anims.Entry("cable_crossover", "Vis-à-vis à la poulie", cableCrossover),
        Anims.Entry("lat_pulldown", "Tirage vertical", latPulldown),
        Anims.Entry("seated_cable_row", "Tirage horizontal", seatedCableRow),
        Anims.Entry("pull_up", "Tractions", pullUp),
        Anims.Entry("back_extension", "Banc à lombaires", backExtension),
        Anims.Entry("machine_shoulder_press", "Développé épaules (machine)", machineShoulderPress),
        Anims.Entry("face_pull", "Face pull", facePull),
        Anims.Entry("triceps_pushdown", "Extension triceps à la poulie", tricepsPushdown),
        Anims.Entry("cable_curl", "Curl à la poulie", cableCurl),
        Anims.Entry("bar_dips", "Dips aux barres", barDips),
        Anims.Entry("leg_press", "Presse à cuisses", legPress),
        Anims.Entry("leg_extension", "Leg extension", legExtension),
        Anims.Entry("leg_curl", "Leg curl allongé", legCurl),
        Anims.Entry("barbell_squat", "Squat à la barre", barbellSquat),
        Anims.Entry("hip_thrust", "Hip thrust", hipThrust),
        Anims.Entry("captain_knee_raise", "Relevés de genoux (chaise romaine)", captainKneeRaise),
        Anims.Entry("rowing_machine", "Rameur", rowingMachine),
        Anims.Entry("exercise_bike", "Vélo", exerciseBike),
        Anims.Entry("treadmill", "Tapis de course", treadmill),
    )

    /** Debout de face (même pose que dans [Anims]). */
    private fun standFront() = Pose(
        na1 = 8f, na2 = 4f, fa1 = -8f, fa2 = -4f,
        nl1 = 6f, nl2 = 3f, fl1 = -6f, fl2 = -3f,
    )
}
