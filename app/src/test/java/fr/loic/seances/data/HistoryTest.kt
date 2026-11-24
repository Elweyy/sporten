package fr.loic.seances.data

import fr.loic.seances.session.Announcements
import fr.loic.seances.session.buildSteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

/** Mêmes cas que les tests de la web app (history.js, random.js, announcement). */
class HistoryTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val day = 86_400_000L
    private val monday = LocalDateTime.of(2026, 11, 9, 18, 0).atZone(zone).toInstant().toEpochMilli()

    private fun e(date: Long, durationMs: Long = 1_800_000, done: Int = 20) =
        HistoryEntry("h$date", date, durationMs, 0, done, 27, "t", cycles = 3)

    private val hist = listOf(e(monday - 14 * day), e(monday - 5 * day), e(monday + day))

    @Test
    fun weekAndStreak() {
        assertEquals(1, HistoryOps.weekCount(hist, monday + 2 * day, zone))
        assertEquals(3, HistoryOps.streakWeeks(hist, monday + 2 * day, zone))
        // Semaine suivante sans séance (pas finie) : la série tient encore.
        assertEquals(3, HistoryOps.streakWeeks(hist, monday + 8 * day, zone))
        assertEquals(0, HistoryOps.streakWeeks(hist, monday + 15 * day, zone))
    }

    @Test
    fun records() {
        assertEquals(listOf("🎉 Première séance enregistrée !"), HistoryOps.records(emptyList(), e(monday), zone))
        assertEquals(
            listOf("🏆 Ta plus longue séance !", "🏆 Record d'exercices : 30 !", "💪 2e séance de la semaine"),
            HistoryOps.records(hist, e(monday + 2 * day, 3_600_000, 30), zone),
        )
        assertEquals(listOf("🔥 3 semaines d'affilée !"), HistoryOps.records(hist.take(2), e(monday + day, 60_000, 3), zone))
    }

    @Test
    fun monthGridStartsOnMonday() {
        val grid = HistoryOps.monthGrid(YearMonth.of(2026, 11))
        assertEquals(6, grid.size)
        assertEquals(1, grid[0][6]!!.dayOfMonth) // le 1er novembre 2026 est un dimanche
        assertEquals(30, grid[5][0]!!.dayOfMonth)
        assertNull(grid[5][1])
    }

    @Test
    fun webBackupWithHistoryImports() {
        // Sauvegarde telle qu'exportée par la web app (champ history ajouté en 2.x).
        val json = """{"version":1,"workouts":[],"customExercises":[],"history":[{"id":"h_1a2b3c4d","date":1794336120000,
            "durationMs":38500,"workedMs":30000,"exercisesDone":2,"exercisesPlanned":2,"title":"Séance aléatoire",
            "subtitle":"Bras","cycles":1,"complete":true}]}"""
        val data = LibraryJson.decodeFromString<LibraryData>(json)
        assertEquals(1, data.history.size)
        assertTrue(data.history[0].complete)
        val merged = LibraryOps.merge(LibraryData(history = listOf(e(monday))), data)
        assertEquals(2, merged.history.size)
        assertEquals(2, LibraryOps.merge(merged, data).history.size) // pas de doublon
        // Anciennes sauvegardes sans historique : toujours lisibles.
        assertEquals(0, LibraryJson.decodeFromString<LibraryData>("""{"workouts":[]}""").history.size)
    }

    @Test
    fun muscleStats() {
        val h = listOf(
            e(monday - 20 * day).copy(muscles = mapOf("JAMBES" to 4)),
            e(monday - 2 * day).copy(muscles = mapOf("JAMBES" to 9, "DOS" to 3)),
            e(monday - 40 * day), // séance d'avant la 1.4 : pas de détail
        )
        val week = HistoryOps.muscleStats(h, 7, monday).associateBy { it.muscle }
        assertEquals(9, week.getValue(Muscle.JAMBES).count)
        assertEquals(3, week.getValue(Muscle.DOS).count)
        assertEquals(0, week.getValue(Muscle.CARDIO).count)
        assertNull(week.getValue(Muscle.CARDIO).lastDate)
        assertEquals(13, HistoryOps.muscleStats(h, 30, monday).first { it.muscle == Muscle.JAMBES }.count)
        assertEquals("il y a 2 jours", HistoryOps.sinceLabel(monday - 2 * day, monday, zone))
        assertEquals("jamais", HistoryOps.sinceLabel(null, monday, zone))
        // Format web : objet { "JAMBES": 9 }
        val json = """{"history":[{"id":"h1","date":1,"durationMs":1,"exercisesDone":9,"exercisesPlanned":9,"title":"t","muscles":{"JAMBES":9}}]}"""
        assertEquals(9, LibraryJson.decodeFromString<LibraryData>(json).history[0].muscles["JAMBES"])
    }

    @Test
    fun bodyMapCoversEveryMuscleButCardioAndIsSymmetric() {
        val shown = (BodyMap.front + BodyMap.back).mapNotNull { it.muscle }.toSet()
        assertEquals(Muscle.entries.toSet() - Muscle.CARDIO, shown)
        assertEquals("M74 45 C84 45", BodyMap.mirror("M46 45 C36 45"))
        assertEquals("M60.6 49", BodyMap.mirror("M59.4 49"))
        assertEquals(BodyMap.front.size, BodyMap.front.distinctBy { it.path }.size)
    }

    @Test
    fun randomWorkout() {
        val ex = Catalog.builtIn
        val r = Random(1)
        val plan = RandomWorkout.generate(ex, listOf(Muscle.CARDIO, Muscle.JAMBES, Muscle.ABDOS), 4, Durations(), r)
        assertEquals("Jambes — Abdos — Cardio", plan.subtitle)
        plan.blocks.forEach { b -> assertEquals(4, b.exerciseIds.toSet().size) }
        val again = RandomWorkout.reroll(ex, plan, 0, 1, r)!!
        assertNotEquals(plan.blocks[0].exerciseIds[1], again.blocks[0].exerciseIds[1])
        assertEquals(4, again.blocks[0].exerciseIds.toSet().size)
        val all = RandomWorkout.generate(ex, listOf(Muscle.EPAULES), 50, Durations(), r)
        assertEquals(ex.count { it.muscle == Muscle.EPAULES }, all.blocks[0].exerciseIds.size)
    }

    @Test
    fun announcements() {
        val plan = WorkoutPlan("x", "X", blocks = listOf(BlockPlan("A", listOf("wall_sit", "good_morning")), BlockPlan("B", listOf("plank"))))
        val steps = buildSteps(plan.resolve(Catalog.builtIn.associateBy { it.id }), 2, Durations())
        val said = steps.indices.map { Announcements.forStep(steps, it, 2) }
        assertEquals("Prépare-toi. Premier exercice : Chaise contre le mur.", said[0])
        assertEquals("C'est parti !", said[1])
        assertEquals("Exercice suivant : Good Morning aux haltères.", said[2])
        assertEquals("Repos. Cycle 1 sur 2 terminé.", said[6])
        assertEquals("Chaise contre le mur, c'est parti !", said[7])
        assertEquals("Dernier exercice : Gainage planche.", said[said.size - 2])
    }
}
