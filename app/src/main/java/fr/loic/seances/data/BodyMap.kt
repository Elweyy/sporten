package fr.loic.seances.data

/**
 * Silhouette (face et dos) découpée en zones musculaires, pour l'écran Muscles de l'historique.
 * Chemins SVG dans une boîte 120 x 260. Les parties en double (bras, jambes…) sont décrites côté
 * gauche de l'image puis recopiées en miroir. Exporté tel quel vers la web app (WebExportTest).
 */
object BodyMap {
    const val WIDTH = 120f
    const val HEIGHT = 260f

    /** Une zone : [muscle] null = partie neutre (tête, mains, genoux…). */
    data class Part(val muscle: Muscle?, val path: String)

    private data class Src(val muscle: Muscle?, val path: String, val pair: Boolean)

    private val head = Src(null, "M60 6 C68 6 73 12 73 21 C73 30 68 37 60 37 C52 37 47 30 47 21 C47 12 52 6 60 6 Z", false)
    private val neck = Src(null, "M53 34 L67 34 L68 46 L52 46 Z", false)
    private val delt = Src(Muscle.EPAULES, "M46 45 C36 45 28 50 26 60 C25 66 26 71 28 75 C31 68 36 63 41 61 C42 55 43 49 46 45 Z", true)
    private val upperArm = Src(Muscle.BRAS, "M27 78 C31 72 36 66 41 64 L40 99 C36 102 30 102 26 99 C25 92 25 84 27 78 Z", true)
    private val forearm = Src(Muscle.BRAS, "M25.5 103 C30 105 35 105 39.5 102 L37 140 C34 143 29 143 27 140 C25 128 25 115 25.5 103 Z", true)
    private val hand = Src(null, "M27 143 C30 145 34 145 37 143 L38 156 C35 160 29 160 26 156 Z", true)
    private val knee = Src(null, "M45 196 C49 199 54 199 57.5 196 L57 205 C54 208 48 208 45.5 205 Z", true)
    private val calf = Src(Muscle.JAMBES, "M45.5 208 C49 211 54 211 57 208 C58 220 57 232 55.5 243 C52 246 49 246 47 243 C45 232 44.5 220 45.5 208 Z", true)
    private val foot = Src(null, "M47 246 C50 248 53 248 55.5 246 L57 254 C53 257 46 257 44 254 Z", true)

    val front: List<Part> = expand(
        head, neck, delt,
        Src(Muscle.PECTORAUX, "M47 47 C52 46 57 47 59.4 49 L59.4 73 C53 77 46 76 42 71 C41 63 42 54 47 47 Z", true),
        upperArm, forearm, hand,
        Src(Muscle.ABDOS, "M43 76 C48 79 54 80 59.4 79 L59.4 124 C53 124 48 121 45 116 C44 103 43 89 43 76 Z", true),
        Src(null, "M45 119 C50 125 55 127 59.4 127 L59.4 142 C53 140 46 136 41 131 C42 127 43 123 45 119 Z", true),
        Src(Muscle.JAMBES, "M41 134 C46 139 53 143 59 145 L58 192 C55 198 48 198 45 193 C40 174 39 153 41 134 Z", true),
        knee, calf, foot,
    )

    val back: List<Part> = expand(
        head, neck,
        Src(Muscle.DOS, "M52 44 L60 44 L60 70 C54 64 46 56 40 50 C44 47 48 45 52 44 Z", true), // trapèzes
        delt,
        Src(Muscle.DOS, "M41 53 C47 59 54 66 59.4 72 L59.4 103 C54 104 48 102 45 98 C42 84 40 68 41 53 Z", true), // dorsaux
        Src(Muscle.DOS, "M45.5 101 C50 105 55 106 59.4 106 L59.4 124 C53 124 48 121 45 117 Z", true), // lombaires
        upperArm, forearm, hand,
        Src(Muscle.JAMBES, "M45 120 C50 125 55 127 59.4 127 L59.4 150 C53 152 46 150 41.5 145 C41 136 42 127 45 120 Z", true), // fessiers
        Src(Muscle.JAMBES, "M41.5 148 C47 152 53 154 59 153 L58 192 C55 198 48 198 45 193 C42 178 41 163 41.5 148 Z", true), // ischios
        knee, calf, foot,
    )

    private fun expand(vararg parts: Src): List<Part> = parts.flatMap { s ->
        if (s.pair) listOf(Part(s.muscle, s.path), Part(s.muscle, mirror(s.path))) else listOf(Part(s.muscle, s.path))
    }

    /** Symétrie gauche-droite : x → WIDTH - x pour chaque couple de coordonnées. */
    fun mirror(path: String): String = Regex("""(-?[\d.]+) (-?[\d.]+)""").replace(path) { m ->
        val x = WIDTH - m.groupValues[1].toFloat()
        val xs = if (x == x.toInt().toFloat()) x.toInt().toString() else "%.1f".format(java.util.Locale.ROOT, x)
        "$xs ${m.groupValues[2]}"
    }
}
