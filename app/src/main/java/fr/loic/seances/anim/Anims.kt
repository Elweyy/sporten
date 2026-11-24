package fr.loic.seances.anim

/**
 * Bibliothèque des animations d'exercices.
 * Chaque animation = une liste de poses clés jouées en boucle (voir [Pose] pour les angles).
 */
object Anims {

    // ---------- Poses de base ----------

    /** Debout, de profil. */
    private val stand = Pose()

    /** Debout, de face, jambes légèrement écartées, bras le long du corps. */
    private val standFront = Pose(
        na1 = 8f, na2 = 4f, fa1 = -8f, fa2 = -4f,
        nl1 = 6f, nl2 = 3f, fl1 = -6f, fl2 = -3f,
    )

    /** Couché sur le dos, tête à gauche, genoux pliés pieds au sol. */
    private val lyingKneesBent = Pose(
        px = 60f, py = 86f, torso = -88f,
        nl1 = 140f, nl2 = 30f, fl1 = 136f, fl2 = 28f,
    )

    /** Squat bas, de profil. */
    private val squatLow = Pose(
        px = 36f, py = 70f, torso = 40f,
        nl1 = 80f, nl2 = -12f, fl1 = 77f, fl2 = -12f,
        na1 = 5f, na2 = 5f, fa1 = 3f, fa2 = 3f,
    )

    /** Pompe bras tendus (tête à gauche). */
    private val plankHigh = Pose(
        px = 57f, py = 75.4f, torso = -66f,
        nl1 = 66f, nl2 = 66f, fl1 = 66f, fl2 = 66f,
    )

    /** Buste penché pour rowing. */
    private val bentOver = Pose(
        px = 38f, py = 56f, torso = 60f,
        nl1 = 20f, nl2 = -10f, fl1 = 17f, fl2 = -10f,
    )

    // ---------- Jour 1 ----------

    val arnoldPress = run {
        val start = standFront.copy(na1 = 15f, na2 = 190f, fa1 = -15f, fa2 = -190f)
        val mid = standFront.copy(na1 = 70f, na2 = 175f, fa1 = -70f, fa2 = -175f)
        val top = standFront.copy(na1 = 162f, na2 = 174f, fa1 = -162f, fa2 = -174f)
        ExerciseAnimation(listOf(start, mid, top, mid), segmentMs = 550, view = ViewMode.FRONT, weight = Weight.BAR)
    }

    val lateralRaise = ExerciseAnimation(
        listOf(
            standFront.copy(na1 = 10f, na2 = 12f, fa1 = -10f, fa2 = -12f),
            standFront.copy(na1 = 88f, na2 = 93f, fa1 = -88f, fa2 = -93f),
        ),
        segmentMs = 800, view = ViewMode.FRONT, weight = Weight.END_ON,
    )

    val reverseFly = run {
        // De profil, buste penché : les bras s'écartent sur les côtés, donc vers / loin du
        // spectateur. On le suggère en raccourcissant les bras quand ils montent.
        val base = bentOver.copy(torso = 72f, px = 36f)
        ExerciseAnimation(
            listOf(
                base.copy(na1 = 2f, na2 = 6f, fa1 = 0f, fa2 = 4f),
                base.copy(na1 = -70f, na2 = -60f, fa1 = -74f, fa2 = -64f, armScale = 0.75f),
            ),
            segmentMs = 800, weight = Weight.END_ON,
        )
    }

    val overheadCrunch = ExerciseAnimation(
        listOf(
            lyingKneesBent.copy(na1 = 180f, na2 = 180f, fa1 = 178f, fa2 = 178f),
            lyingKneesBent.copy(torso = -48f, na1 = 168f, na2 = 168f, fa1 = 166f, fa2 = 166f),
        ),
        segmentMs = 800, weight = Weight.SINGLE,
    )

    val russianTwist = run {
        val seated = Pose(
            px = 48f, py = 86f, torso = -40f,
            nl1 = 120f, nl2 = 60f, fl1 = 117f, fl2 = 57f,
        )
        val center = seated.copy(na1 = 100f, na2 = 110f, fa1 = 98f, fa2 = 108f)
        val sideNear = seated.copy(torso = -36f, na1 = 40f, na2 = 28f, fa1 = 44f, fa2 = 32f)
        val sideFar = seated.copy(torso = -46f, na1 = -8f, na2 = -18f, fa1 = -4f, fa2 = -14f)
        ExerciseAnimation(listOf(center, sideNear, center, sideFar), segmentMs = 450, weight = Weight.SINGLE)
    }

    val bigL = run {
        val hold = Pose(
            px = 58f, py = 86f, torso = -82f,
            na1 = -80f, na2 = -80f, fa1 = -82f, fa2 = -82f,
            nl1 = 96f, nl2 = 96f, fl1 = 94f, fl2 = 94f,
        )
        // Angles choisis pour que l'interpolation passe par le haut (195 = -165, -220 = 140).
        val touchNearLeg = hold.copy(nl1 = 195f, nl2 = 195f, fa1 = -220f, fa2 = -220f, torso = -74f)
        val touchFarLeg = hold.copy(fl1 = 195f, fl2 = 195f, na1 = -220f, na2 = -220f, torso = -74f)
        ExerciseAnimation(listOf(hold, touchNearLeg, hold, touchFarLeg), segmentMs = 650)
    }

    val dumbbellSquat = ExerciseAnimation(
        listOf(stand.copy(na1 = 2f, na2 = 2f), squatLow),
        segmentMs = 900, weight = Weight.BAR,
    )

    val lunge = run {
        val up = Pose(px = 45f, py = 58.2f, nl1 = 28f, nl2 = 28f, fl1 = -28f, fl2 = -28f)
        val down = Pose(px = 45f, py = 70f, nl1 = 90f, nl2 = 0f, fl1 = -10f, fl2 = -80f)
        ExerciseAnimation(listOf(up, down), segmentMs = 900, weight = Weight.BAR)
    }

    val goodMorning = run {
        val up = Pose(px = 45f, py = 54f, na1 = 20f, na2 = 160f, fa1 = 18f, fa2 = 158f)
        val down = Pose(
            px = 40f, py = 55f, torso = 75f,
            nl1 = 15f, nl2 = -5f, fl1 = 13f, fl2 = -5f,
            na1 = -55f, na2 = 85f, fa1 = -57f, fa2 = 83f,
        )
        ExerciseAnimation(listOf(up, down), segmentMs = 1000, weight = Weight.END_ON)
    }

    // ---------- Jour 2 ----------

    val floorPress = ExerciseAnimation(
        listOf(
            lyingKneesBent.copy(na1 = 90f, na2 = 180f, fa1 = 88f, fa2 = 178f),
            lyingKneesBent.copy(na1 = 180f, na2 = 180f, fa1 = 178f, fa2 = 178f),
        ),
        segmentMs = 800, weight = Weight.END_ON,
    )

    val dumbbellFly = run {
        // Vu de dessus : couché, tête en haut.
        val lying = Pose(px = 50f, py = 60f, nl1 = 4f, nl2 = 2f, fl1 = -4f, fl2 = -2f)
        val open = lying.copy(na1 = 90f, na2 = 98f, fa1 = -90f, fa2 = -98f)
        val closed = lying.copy(na1 = 100f, na2 = 120f, fa1 = -100f, fa2 = -120f, armScale = 0.2f)
        ExerciseAnimation(listOf(open, closed), segmentMs = 900, view = ViewMode.TOP, weight = Weight.END_ON, ground = false)
    }

    val pushUp = ExerciseAnimation(
        listOf(
            plankHigh,
            Pose(
                px = 54.3f, py = 85.6f, torso = -83f,
                nl1 = 83f, nl2 = 83f, fl1 = 83f, fl2 = 83f,
                na1 = 100f, na2 = -39f, fa1 = 100f, fa2 = -39f,
            ),
        ),
        segmentMs = 700,
    )

    val alternateCurl = run {
        val down = stand.copy(na1 = 2f, na2 = 2f, fa1 = -2f, fa2 = -2f)
        ExerciseAnimation(
            listOf(down, down.copy(na1 = 8f, na2 = 165f), down, down.copy(fa1 = 6f, fa2 = 163f)),
            segmentMs = 650, weight = Weight.END_ON,
        )
    }

    val skullCrusher = ExerciseAnimation(
        listOf(
            lyingKneesBent.copy(na1 = -172f, na2 = -172f, fa1 = -170f, fa2 = -170f),
            lyingKneesBent.copy(na1 = -172f, na2 = -60f, fa1 = -170f, fa2 = -58f),
        ),
        segmentMs = 800, weight = Weight.END_ON,
    )

    val hammerCurl = run {
        val down = stand.copy(na1 = 2f, na2 = 2f, fa1 = -2f, fa2 = -2f)
        ExerciseAnimation(
            listOf(down, down.copy(na1 = 8f, na2 = 160f, fa1 = 6f, fa2 = 158f)),
            segmentMs = 800, weight = Weight.BAR,
        )
    }

    val legRaise = run {
        val base = Pose(px = 55f, py = 86f, torso = -88f, na1 = 88f, na2 = 88f, fa1 = 86f, fa2 = 86f)
        ExerciseAnimation(
            listOf(
                base.copy(nl1 = 94f, nl2 = 94f, fl1 = 93f, fl2 = 93f),
                base.copy(nl1 = 178f, nl2 = 178f, fl1 = 176f, fl2 = 176f),
            ),
            segmentMs = 900,
        )
    }

    val crossCrunch = run {
        val down = Pose(
            px = 58f, py = 86f, torso = -85f,
            na1 = 175f, na2 = -35f, fa1 = 172f, fa2 = -38f,
            nl1 = 175f, nl2 = 90f, fl1 = 173f, fl2 = 90f,
        )
        val upArms = down.copy(torso = -60f, na1 = 170f, na2 = -44f, fa1 = 168f, fa2 = -46f)
        val nearKnee = upArms.copy(nl1 = 210f, nl2 = 100f, fl1 = 97f, fl2 = 97f)
        val farKnee = upArms.copy(fl1 = 210f, fl2 = 100f, nl1 = 97f, nl2 = 97f)
        ExerciseAnimation(listOf(down, nearKnee, down, farKnee), segmentMs = 550)
    }

    val shoulderTaps = run {
        val tapNear = plankHigh.copy(na1 = -30f, na2 = 150f)
        val tapFar = plankHigh.copy(fa1 = -30f, fa2 = 150f)
        ExerciseAnimation(listOf(plankHigh, tapNear, plankHigh, tapFar), segmentMs = 500)
    }

    // ---------- Jour 3 ----------

    val jumpSquat = run {
        val squat = squatLow.copy(na1 = -40f, na2 = -40f, fa1 = -38f, fa2 = -38f)
        val air = Pose(py = 48f, na1 = 150f, na2 = 160f, fa1 = 148f, fa2 = 158f, nl1 = 4f, nl2 = -4f, fl1 = 2f, fl2 = -6f)
        ExerciseAnimation(listOf(stand, squat, air), segmentMs = 480)
    }

    val jumpLunge = run {
        val lungeNear = Pose(
            px = 45f, py = 70f, nl1 = 90f, nl2 = 0f, fl1 = -10f, fl2 = -80f,
            na1 = -30f, na2 = -20f, fa1 = 30f, fa2 = 40f,
        )
        val lungeFar = Pose(
            px = 45f, py = 70f, fl1 = 90f, fl2 = 0f, nl1 = -10f, nl2 = -80f,
            fa1 = -30f, fa2 = -20f, na1 = 30f, na2 = 40f,
        )
        val air = Pose(px = 45f, py = 48f, nl1 = 20f, nl2 = -10f, fl1 = -20f, fl2 = -30f)
        val air2 = air.copy(nl1 = -20f, nl2 = -30f, fl1 = 20f, fl2 = -10f)
        ExerciseAnimation(listOf(lungeNear, air, lungeFar, air2), segmentMs = 450)
    }

    val thruster = run {
        val down = squatLow.copy(na1 = 40f, na2 = 200f, fa1 = 38f, fa2 = 198f)
        val rack = stand.copy(na1 = 20f, na2 = 165f, fa1 = 18f, fa2 = 163f)
        val press = stand.copy(na1 = 180f, na2 = 180f, fa1 = 178f, fa2 = 178f)
        ExerciseAnimation(listOf(down, rack, press, rack), segmentMs = 550, weight = Weight.END_ON)
    }

    val flutterKicks = run {
        val base = Pose(px = 55f, py = 86f, torso = -80f, na1 = 88f, na2 = 88f, fa1 = 86f, fa2 = 86f)
        ExerciseAnimation(
            listOf(
                base.copy(nl1 = 122f, nl2 = 122f, fl1 = 96f, fl2 = 96f),
                base.copy(nl1 = 96f, nl2 = 96f, fl1 = 122f, fl2 = 122f),
            ),
            segmentMs = 320,
        )
    }

    val hipDips = run {
        val plank = Pose(
            px = 54.8f, py = 82.4f, torso = -77.8f,
            na1 = 0f, na2 = -90f, fa1 = 0f, fa2 = -90f,
            nl1 = 77.8f, nl2 = 77.8f, fl1 = 77.8f, fl2 = 77.8f,
        )
        val dip = plank.copy(px = 54f, py = 86.5f, torso = -70.7f, nl1 = 84f, nl2 = 84f, fl1 = 84f, fl2 = 84f)
        ExerciseAnimation(listOf(plank, dip), segmentMs = 650)
    }

    val windshieldWipers = run {
        // Vu de dessus : couché, bras en croix, tête en haut.
        val base = Pose(px = 50f, py = 60f, na1 = 90f, na2 = 90f, fa1 = -90f, fa2 = -90f)
        val center = base.copy(nl1 = 4f, nl2 = 4f, fl1 = -4f, fl2 = -4f, legScale = 0.15f)
        val left = base.copy(nl1 = -62f, nl2 = -62f, fl1 = -70f, fl2 = -70f, legScale = 0.85f)
        val right = base.copy(nl1 = 70f, nl2 = 70f, fl1 = 62f, fl2 = 62f, legScale = 0.85f)
        ExerciseAnimation(listOf(center, left, center, right), segmentMs = 800, view = ViewMode.TOP, ground = false)
    }

    val bentOverRow = ExerciseAnimation(
        listOf(
            bentOver.copy(na1 = 0f, na2 = 0f, fa1 = 2f, fa2 = 2f),
            bentOver.copy(na1 = -120f, na2 = 0f, fa1 = -118f, fa2 = 2f),
        ),
        segmentMs = 750, weight = Weight.BAR,
    )

    val pullover = ExerciseAnimation(
        listOf(
            lyingKneesBent.copy(na1 = 180f, na2 = 180f, fa1 = 178f, fa2 = 178f),
            lyingKneesBent.copy(na1 = 260f, na2 = 260f, fa1 = 258f, fa2 = 258f),
        ),
        segmentMs = 1000, weight = Weight.SINGLE,
    )

    val deadStopRow = run {
        val base = Pose(px = 34f, py = 59f, torso = 88f, nl1 = 40f, nl2 = -15f, fl1 = 37f, fl2 = -15f)
        val floor = base.copy(na1 = 0f, na2 = 0f, fa1 = 2f, fa2 = 2f)
        val row = base.copy(na1 = -120f, na2 = 0f, fa1 = -118f, fa2 = 2f)
        ExerciseAnimation(listOf(floor, floor, row), segmentMs = 550, weight = Weight.BAR)
    }

    // ---------- Catalogue : poids du corps, haltères, chaise ----------

    /** Planche sur les avant-bras (tête à gauche). */
    private val plankForearm = Pose(
        px = 54.8f, py = 82.4f, torso = -77.8f,
        na1 = 0f, na2 = -90f, fa1 = 0f, fa2 = -90f,
        nl1 = 77.8f, nl2 = 77.8f, fl1 = 77.8f, fl2 = 77.8f,
    )

    val plank = ExerciseAnimation(
        listOf(plankForearm, plankForearm.copy(py = 81.6f, torso = -78.6f, nl1 = 77f, nl2 = 77f, fl1 = 77f, fl2 = 77f)),
        segmentMs = 1400,
    )

    val mountainClimbers = run {
        val nearIn = plankHigh.copy(nl1 = -60f, nl2 = 75f)
        val farIn = plankHigh.copy(fl1 = -60f, fl2 = 75f)
        ExerciseAnimation(listOf(nearIn, farIn), segmentMs = 300)
    }

    val burpee = run {
        val crouch = Pose(
            px = 44f, py = 70f, torso = 75f,
            nl1 = 70f, nl2 = -40f, fl1 = 68f, fl2 = -40f,
            na1 = 5f, na2 = 5f, fa1 = 3f, fa2 = 3f,
        )
        val plankRight = Pose(
            px = 43f, py = 75.4f, torso = 66f,
            nl1 = -66f, nl2 = -66f, fl1 = -66f, fl2 = -66f,
        )
        val air = Pose(py = 48f, na1 = 150f, na2 = 160f, fa1 = 148f, fa2 = 158f, nl1 = 3f, nl2 = -3f, fl1 = 1f, fl2 = -5f)
        ExerciseAnimation(listOf(stand, crouch, plankRight, crouch, air), segmentMs = 450)
    }

    val jumpingJacks = ExerciseAnimation(
        listOf(
            standFront,
            standFront.copy(
                na1 = 150f, na2 = 162f, fa1 = -150f, fa2 = -162f,
                nl1 = 20f, nl2 = 20f, fl1 = -20f, fl2 = -20f,
            ),
        ),
        segmentMs = 380, view = ViewMode.FRONT,
    )

    val kneePushUp = ExerciseAnimation(
        listOf(
            Pose(
                px = 54.6f, py = 78.7f, torso = -59f,
                nl1 = 59f, nl2 = 95f, fl1 = 59f, fl2 = 95f,
            ),
            Pose(
                px = 52.3f, py = 85f, torso = -80.4f,
                nl1 = 80.4f, nl2 = 95f, fl1 = 80.4f, fl2 = 95f,
                na1 = 100f, na2 = -31f, fa1 = 100f, fa2 = -31f,
            ),
        ),
        segmentMs = 700,
    )

    val declinePushUp = ExerciseAnimation(
        listOf(
            Pose(
                px = 52.2f, py = 66.3f, torso = -84f,
                nl1 = 84f, nl2 = 84f, fl1 = 84f, fl2 = 84f,
            ),
            Pose(
                px = 52.2f, py = 73.8f, torso = -96.1f,
                nl1 = 96.1f, nl2 = 96.1f, fl1 = 96.1f, fl2 = 96.1f,
                na1 = 55f, na2 = -66f, fa1 = 55f, fa2 = -66f,
            ),
        ),
        segmentMs = 750,
        props = listOf(Prop.Chair(x = 80f, seatY = 70f, backLeft = false)),
    )

    val chairDips = ExerciseAnimation(
        listOf(
            Pose(
                px = 31.1f, py = 72.8f, torso = -6f,
                nl1 = 75f, nl2 = 46f, fl1 = 73f, fl2 = 46f,
            ),
            Pose(
                px = 34.1f, py = 82.8f, torso = -6f,
                na1 = -60f, na2 = 42f, fa1 = -60f, fa2 = 42f,
                nl1 = 78f, nl2 = 79f, fl1 = 76f, fl2 = 79f,
            ),
        ),
        segmentMs = 750,
        props = listOf(Prop.Chair(x = 13f, seatY = 70f)),
    )

    val bulgarianSquat = ExerciseAnimation(
        listOf(
            Pose(
                px = 44f, py = 58.8f, torso = 8f,
                nl1 = 30f, nl2 = 30f, fl1 = -10f, fl2 = -112f,
                na1 = 2f, na2 = 2f, fa1 = 0f, fa2 = 0f,
            ),
            Pose(
                px = 40f, py = 72f, torso = 14f,
                nl1 = 85f, nl2 = 14f, fl1 = -25f, fl2 = -152f,
                na1 = 4f, na2 = 4f, fa1 = 2f, fa2 = 2f,
            ),
        ),
        segmentMs = 900, weight = Weight.BAR,
        props = listOf(Prop.Chair(x = 8f, seatY = 70f)),
    )

    val wallSit = run {
        val hold = Pose(
            px = 25f, py = 72f,
            nl1 = 90f, nl2 = 0f, fl1 = 88f, fl2 = 0f,
            na1 = 90f, na2 = 90f, fa1 = 88f, fa2 = 88f,
        )
        ExerciseAnimation(
            listOf(hold, hold.copy(na1 = 84f, na2 = 84f, fa1 = 82f, fa2 = 82f)),
            segmentMs = 1400,
            props = listOf(Prop.Wall(x = 17f)),
        )
    }

    val gluteBridge = ExerciseAnimation(
        listOf(
            Pose(
                px = 60f, py = 86f, torso = -88f,
                nl1 = 140f, nl2 = 30f, fl1 = 138f, fl2 = 29f,
                na1 = 92f, na2 = 92f, fa1 = 92f, fa2 = 92f,
            ),
            Pose(
                px = 58f, py = 72f, torso = -115f,
                nl1 = 90f, nl2 = 14f, fl1 = 89f, fl2 = 14f,
                na1 = 116f, na2 = 116f, fa1 = 116f, fa2 = 116f,
            ),
        ),
        segmentMs = 800, weight = Weight.SINGLE,
    )

    val superman = run {
        val down = Pose(
            px = 60f, py = 86f, torso = -90f,
            na1 = -90f, na2 = -90f, fa1 = -90f, fa2 = -90f,
            nl1 = 90f, nl2 = 90f, fl1 = 90f, fl2 = 90f,
        )
        val up = down.copy(
            torso = -80f,
            na1 = -102f, na2 = -102f, fa1 = -100f, fa2 = -100f,
            nl1 = 101f, nl2 = 101f, fl1 = 99f, fl2 = 99f,
        )
        ExerciseAnimation(listOf(down, up, up), segmentMs = 650)
    }

    val sidePlank = run {
        // Vu de face, allongé sur le côté, appui sur l'avant-bras (côté bas = far).
        val up = Pose(
            px = 59f, py = 76.8f, torso = -75f,
            fa1 = 0f, fa2 = -90f, na1 = 180f, na2 = 180f,
            nl1 = 75f, nl2 = 75f, fl1 = 75f, fl2 = 75f,
        )
        val dip = up.copy(px = 58f, py = 82f, torso = -65f, nl1 = 82f, nl2 = 82f, fl1 = 82f, fl2 = 82f)
        ExerciseAnimation(listOf(up, dip), segmentMs = 800, view = ViewMode.FRONT)
    }

    val highKnees = ExerciseAnimation(
        listOf(
            stand.copy(nl1 = 90f, nl2 = 0f, na1 = -40f, na2 = 60f, fa1 = 60f, fa2 = 150f),
            stand.copy(fl1 = 90f, fl2 = 0f, fa1 = -40f, fa2 = 60f, na1 = 60f, na2 = 150f),
        ),
        segmentMs = 300,
    )

    val militaryPress = ExerciseAnimation(
        listOf(
            standFront.copy(na1 = 80f, na2 = 180f, fa1 = -80f, fa2 = -180f),
            standFront.copy(na1 = 165f, na2 = 175f, fa1 = -165f, fa2 = -175f),
        ),
        segmentMs = 800, view = ViewMode.FRONT, weight = Weight.BAR,
    )

    val romanianDeadlift = ExerciseAnimation(
        listOf(
            Pose(px = 45f, py = 54f, na1 = 4f, na2 = 4f, fa1 = 2f, fa2 = 2f),
            Pose(
                px = 40f, py = 55f, torso = 80f,
                nl1 = 12f, nl2 = -6f, fl1 = 10f, fl2 = -6f,
                na1 = 0f, na2 = 0f, fa1 = -2f, fa2 = -2f,
            ),
        ),
        segmentMs = 1000, weight = Weight.BAR,
    )

    val oneArmRow = run {
        val base = Pose(
            px = 40f, py = 56f, torso = 70f,
            fa1 = 15f, fa2 = 21f,
            nl1 = 15f, nl2 = -8f, fl1 = 13f, fl2 = -8f,
        )
        ExerciseAnimation(
            listOf(base.copy(na1 = 0f, na2 = 0f), base.copy(na1 = -120f, na2 = 0f)),
            segmentMs = 750, weight = Weight.BAR, weightBothHands = false,
            props = listOf(Prop.Chair(x = 70f, seatY = 70f, backLeft = false)),
        )
    }

    val frontRaise = ExerciseAnimation(
        listOf(
            stand.copy(na1 = 4f, na2 = 4f, fa1 = 2f, fa2 = 2f),
            stand.copy(na1 = 90f, na2 = 92f, fa1 = 88f, fa2 = 90f),
        ),
        segmentMs = 800, weight = Weight.END_ON,
    )

    val overheadTricepsExtension = ExerciseAnimation(
        listOf(
            stand.copy(na1 = 170f, na2 = 170f, fa1 = 168f, fa2 = 168f),
            stand.copy(na1 = 170f, na2 = -30f, fa1 = 168f, fa2 = -32f),
        ),
        segmentMs = 800, weight = Weight.SINGLE,
    )

    /** Assis sur une chaise (dossier à gauche), de profil. */
    private val seated = Pose(
        px = 40f, py = 71.5f,
        nl1 = 88f, nl2 = 0f, fl1 = 86f, fl2 = 0f,
    )

    val concentrationCurl = run {
        val base = seated.copy(torso = 40f, fa1 = 40f, fa2 = 60f)
        ExerciseAnimation(
            listOf(base.copy(na1 = 3f, na2 = 0f), base.copy(na1 = 3f, na2 = 160f)),
            segmentMs = 800, weight = Weight.END_ON, weightBothHands = false,
            props = listOf(Prop.Chair(x = 30f, seatY = 73f)),
        )
    }

    val seatedPress = run {
        val base = seated.copy(px = 36f)
        ExerciseAnimation(
            listOf(
                base.copy(na1 = 20f, na2 = 175f, fa1 = 18f, fa2 = 173f),
                base.copy(na1 = 178f, na2 = 178f, fa1 = 176f, fa2 = 176f),
            ),
            segmentMs = 800, weight = Weight.END_ON,
            props = listOf(Prop.Chair(x = 26f, seatY = 73f)),
        )
    }

    val sumoSquat = ExerciseAnimation(
        listOf(
            standFront.copy(py = 58f, nl1 = 28f, nl2 = 28f, fl1 = -28f, fl2 = -28f, na1 = -10f, na2 = -5f, fa1 = 10f, fa2 = 5f),
            standFront.copy(py = 68f, nl1 = 71f, nl2 = -2f, fl1 = -71f, fl2 = 2f, na1 = -10f, na2 = -5f, fa1 = 10f, fa2 = 5f),
        ),
        segmentMs = 900, view = ViewMode.FRONT, weight = Weight.SINGLE,
    )

    // ---------- Ajouts : rowing menton, kickback, quadrupédie, fentes latérales… ----------

    val uprightRow = ExerciseAnimation(
        listOf(
            standFront.copy(na1 = 6f, na2 = -4f, fa1 = -6f, fa2 = 4f),
            standFront.copy(na1 = 110f, na2 = -78f, fa1 = -110f, fa2 = 78f),
        ),
        segmentMs = 800, view = ViewMode.FRONT, weight = Weight.END_ON,
    )

    val tricepsKickback = run {
        val base = bentOver.copy(torso = 70f, px = 40f)
        ExerciseAnimation(
            listOf(
                base.copy(na1 = -100f, na2 = 0f, fa1 = -98f, fa2 = 2f),
                base.copy(na1 = -100f, na2 = -105f, fa1 = -98f, fa2 = -103f),
            ),
            segmentMs = 700, weight = Weight.END_ON,
        )
    }

    /** À quatre pattes (tête à gauche) : mains sous les épaules, genoux sous les hanches. */
    private val quadruped = Pose(
        px = 62f, py = 72f, torso = -73f,
        na1 = 0f, na2 = 0f, fa1 = 0f, fa2 = 0f,
        nl1 = 0f, nl2 = 90f, fl1 = 0f, fl2 = 90f,
    )

    val birdDog = run {
        val nearArmFarLeg = quadruped.copy(na1 = -100f, na2 = -100f, fl1 = 96f, fl2 = 96f)
        val farArmNearLeg = quadruped.copy(fa1 = -100f, fa2 = -100f, nl1 = 96f, nl2 = 96f)
        ExerciseAnimation(listOf(quadruped, nearArmFarLeg, nearArmFarLeg, quadruped, farArmNearLeg, farArmNearLeg), segmentMs = 550)
    }

    val donkeyKick = ExerciseAnimation(
        listOf(quadruped, quadruped.copy(nl1 = 105f, nl2 = 190f)),
        segmentMs = 650,
    )

    val sideLunge = run {
        val up = standFront.copy(py = 56.2f, nl1 = 20f, nl2 = 20f, fl1 = -20f, fl2 = -20f, na1 = 8f, na2 = 4f, fa1 = -8f, fa2 = -4f)
        val right = up.copy(px = 53f, py = 62f, nl1 = 45f, nl2 = 8f, fl1 = -36f, fl2 = -36f, na1 = 30f, na2 = -40f, fa1 = -30f, fa2 = 40f)
        val left = up.copy(px = 47f, py = 62f, fl1 = -45f, fl2 = -8f, nl1 = 36f, nl2 = 36f, na1 = 30f, na2 = -40f, fa1 = -30f, fa2 = 40f)
        ExerciseAnimation(listOf(up, right, up, left), segmentMs = 800, view = ViewMode.FRONT)
    }

    val calfRaise = run {
        val down = stand.copy(na1 = 2f, na2 = 2f, fa1 = 0f, fa2 = 0f)
        ExerciseAnimation(listOf(down, down.copy(py = 50.5f)), segmentMs = 700, weight = Weight.BAR)
    }

    val deadBug = run {
        val table = Pose(
            px = 60f, py = 86f, torso = -88f,
            na1 = 180f, na2 = 180f, fa1 = 178f, fa2 = 178f,
            nl1 = 180f, nl2 = 90f, fl1 = 178f, fl2 = 90f,
        )
        val nearArmFarLeg = table.copy(na1 = -95f, na2 = -95f, fl1 = 95f, fl2 = 95f)
        val farArmNearLeg = table.copy(fa1 = -95f, fa2 = -95f, nl1 = 95f, nl2 = 95f)
        ExerciseAnimation(listOf(table, nearArmFarLeg, table, farArmNearLeg), segmentMs = 750)
    }

    val buttKicks = ExerciseAnimation(
        listOf(
            stand.copy(nl1 = -12f, nl2 = -168f, fl1 = 4f, fl2 = 0f, na1 = -40f, na2 = 60f, fa1 = 40f, fa2 = 140f),
            stand.copy(fl1 = -12f, fl2 = -168f, nl1 = 4f, nl2 = 0f, fa1 = -40f, fa2 = 60f, na1 = 40f, na2 = 140f),
        ),
        segmentMs = 300,
    )

    // ---------- Programme salle ----------

    /** Petits sauts rebondis sur la pointe des pieds, jambes presque tendues. */
    val pogoHop = run {
        val down = stand.copy(na1 = -12f, na2 = 20f, fa1 = -10f, fa2 = 22f)
        ExerciseAnimation(listOf(down, down.copy(py = 47f, nl2 = 4f, fl2 = 4f)), segmentMs = 260)
    }

    /** Swing kettlebell : charnière de hanche puis bras à l'horizontale. */
    val kbSwing = run {
        val hinge = Pose(
            px = 40f, py = 56f, torso = 62f,
            nl1 = 22f, nl2 = -8f, fl1 = 19f, fl2 = -8f,
            na1 = -25f, na2 = -30f, fa1 = -27f, fa2 = -32f,
        )
        val top = stand.copy(px = 46f, torso = -4f, na1 = 88f, na2 = 90f, fa1 = 86f, fa2 = 88f)
        ExerciseAnimation(listOf(hinge, top), segmentMs = 600, weight = Weight.SINGLE)
    }

    /** Slam au medecine ball : bras au-dessus de la tête puis lancer vers le sol. */
    val ballSlam = run {
        val top = stand.copy(py = 52f, torso = -6f, na1 = 172f, na2 = 178f, fa1 = 170f, fa2 = 176f)
        val down = Pose(
            px = 42f, py = 64f, torso = 55f,
            nl1 = 60f, nl2 = -18f, fl1 = 57f, fl2 = -18f,
            na1 = 35f, na2 = 25f, fa1 = 33f, fa2 = 23f,
        )
        ExerciseAnimation(listOf(top, down), segmentMs = 520, weight = Weight.SINGLE)
    }

    /** Traction (vue de face, pas de sol) : suspendu bras tendus, puis menton à la barre. */
    val chinUp = run {
        val hang = standFront.copy(py = 62f, na1 = 168f, na2 = 174f, fa1 = -168f, fa2 = -174f, nl1 = 3f, nl2 = 2f, fl1 = -3f, fl2 = -2f)
        val top = hang.copy(py = 51f, na1 = 128f, na2 = 186f, fa1 = -128f, fa2 = -186f, nl1 = 20f, nl2 = -30f, fl1 = -20f, fl2 = -30f)
        ExerciseAnimation(listOf(hang, top), segmentMs = 800, view = ViewMode.FRONT, ground = false)
    }

    // ---------- Registre ----------

    data class Entry(val id: String, val label: String, val animation: ExerciseAnimation)

    /** Toutes les animations, avec un identifiant stable (utilisé dans les sauvegardes). */
    val entries: List<Entry> = listOf(
        Entry("arnold_press", "Arnold Press", arnoldPress),
        Entry("lateral_raise", "Élévations latérales", lateralRaise),
        Entry("reverse_fly", "Oiseau", reverseFly),
        Entry("front_raise", "Élévations frontales", frontRaise),
        Entry("military_press", "Développé militaire", militaryPress),
        Entry("seated_press", "Développé assis (chaise)", seatedPress),
        Entry("overhead_crunch", "Crunch bras tendus", overheadCrunch),
        Entry("russian_twist", "Russian Twist", russianTwist),
        Entry("big_l", "Big L", bigL),
        Entry("leg_raise", "Relevés de jambes", legRaise),
        Entry("cross_crunch", "Crunch croisé", crossCrunch),
        Entry("shoulder_taps", "Planche touchés d'épaules", shoulderTaps),
        Entry("flutter_kicks", "Flutter kicks", flutterKicks),
        Entry("hip_dips", "Planche balancés de hanches", hipDips),
        Entry("windshield_wipers", "Essuie-glaces", windshieldWipers),
        Entry("plank", "Planche", plank),
        Entry("side_plank", "Gainage latéral", sidePlank),
        Entry("dumbbell_squat", "Squat", dumbbellSquat),
        Entry("lunge", "Fente", lunge),
        Entry("good_morning", "Good Morning", goodMorning),
        Entry("jump_squat", "Squat sauté", jumpSquat),
        Entry("jump_lunge", "Fente sautée", jumpLunge),
        Entry("thruster", "Thruster", thruster),
        Entry("bulgarian_squat", "Squat bulgare (chaise)", bulgarianSquat),
        Entry("wall_sit", "Chaise contre le mur", wallSit),
        Entry("glute_bridge", "Pont fessier", gluteBridge),
        Entry("romanian_deadlift", "Soulevé de terre jambes tendues", romanianDeadlift),
        Entry("sumo_squat", "Squat sumo", sumoSquat),
        Entry("floor_press", "Développé au sol", floorPress),
        Entry("dumbbell_fly", "Écartés", dumbbellFly),
        Entry("push_up", "Pompe", pushUp),
        Entry("knee_push_up", "Pompe sur les genoux", kneePushUp),
        Entry("decline_push_up", "Pompe pieds sur chaise", declinePushUp),
        Entry("alternate_curl", "Curl alterné", alternateCurl),
        Entry("hammer_curl", "Curl marteau", hammerCurl),
        Entry("concentration_curl", "Curl concentré (chaise)", concentrationCurl),
        Entry("skull_crusher", "Skull Crusher", skullCrusher),
        Entry("overhead_triceps", "Extension triceps au-dessus de la tête", overheadTricepsExtension),
        Entry("chair_dips", "Dips sur chaise", chairDips),
        Entry("bent_over_row", "Rowing penché", bentOverRow),
        Entry("one_arm_row", "Rowing unilatéral (chaise)", oneArmRow),
        Entry("dead_stop_row", "Dead Stop Row", deadStopRow),
        Entry("pullover", "Pullover", pullover),
        Entry("superman", "Superman", superman),
        Entry("mountain_climbers", "Mountain climbers", mountainClimbers),
        Entry("burpee", "Burpee", burpee),
        Entry("jumping_jacks", "Jumping jacks", jumpingJacks),
        Entry("high_knees", "Montées de genoux", highKnees),
        Entry("upright_row", "Rowing menton", uprightRow),
        Entry("triceps_kickback", "Kickback triceps", tricepsKickback),
        Entry("bird_dog", "Bird dog", birdDog),
        Entry("donkey_kick", "Donkey kick", donkeyKick),
        Entry("side_lunge", "Fente latérale", sideLunge),
        Entry("calf_raise", "Mollets debout", calfRaise),
        Entry("dead_bug", "Dead bug", deadBug),
        Entry("butt_kicks", "Talons-fesses", buttKicks),
        Entry("pogo_hop", "Pogo hops", pogoHop),
        Entry("kb_swing", "Swing kettlebell", kbSwing),
        Entry("ball_slam", "Slam medecine ball", ballSlam),
        Entry("chin_up", "Traction", chinUp),
    ) + GymAnims.entries

    private val byId = entries.associateBy { it.id }

    /** Animation par identifiant ; repli sur la planche si l'identifiant est inconnu. */
    fun get(id: String): ExerciseAnimation = byId[id]?.animation ?: plank

    fun has(id: String): Boolean = id in byId
}
