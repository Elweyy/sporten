package fr.loic.seances.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Une séance faite (terminée, ou arrêtée après au moins un exercice). Même format que la web app :
 * l'historique voyage avec les sauvegardes exportées.
 */
@Serializable
data class HistoryEntry(
    val id: String,
    /** Début de la séance (ms depuis 1970). */
    val date: Long,
    val durationMs: Long,
    val workedMs: Long = 0,
    val exercisesDone: Int,
    val exercisesPlanned: Int,
    val title: String,
    val subtitle: String = "",
    val cycles: Int = 1,
    val complete: Boolean = false,
    /** Exercices faits par groupe musculaire (nom de [Muscle] → nombre). Absent des séances d'avant la 1.4. */
    val muscles: Map<String, Int> = emptyMap(),
)

/** Statistiques et records : mêmes règles que web/js/history.js. */
object HistoryOps {

    fun day(ms: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    /** Lundi de la semaine qui contient ce jour. */
    fun weekStart(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekCount(history: List<HistoryEntry>, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val week = weekStart(day(now, zone))
        return history.count { weekStart(day(it.date, zone)) == week }
    }

    /**
     * Semaines d'affilée avec au moins une séance. La semaine en cours compte si elle en a une ;
     * sinon on part de la précédente (la semaine n'est pas finie, la série tient encore).
     */
    fun streakWeeks(history: List<HistoryEntry>, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val weeks = history.map { weekStart(day(it.date, zone)) }.toSet()
        var cursor = weekStart(day(now, zone))
        if (cursor !in weeks) cursor = cursor.minusWeeks(1)
        var n = 0
        while (cursor in weeks) {
            n++
            cursor = cursor.minusWeeks(1)
        }
        return n
    }

    /** Messages de l'écran de fin, en comparant la nouvelle séance à l'historique d'avant. */
    fun records(previous: List<HistoryEntry>, entry: HistoryEntry, zone: ZoneId = ZoneId.systemDefault()): List<String> {
        if (previous.isEmpty()) return listOf("🎉 Première séance enregistrée !")
        val out = mutableListOf<String>()
        if (entry.durationMs > previous.maxOf { it.durationMs }) out += "🏆 Ta plus longue séance !"
        if (entry.exercisesDone > previous.maxOf { it.exercisesDone }) out += "🏆 Record d'exercices : ${entry.exercisesDone} !"
        val all = previous + entry
        val firstOfWeek = weekCount(previous, entry.date, zone) == 0
        val streak = streakWeeks(all, entry.date, zone)
        if (firstOfWeek && streak >= 2) out += "🔥 $streak semaines d'affilée !"
        val week = weekCount(all, entry.date, zone)
        if (week >= 2) out += "💪 ${week}e séance de la semaine"
        return out
    }

    /** Statistiques d'un groupe musculaire : exercices faits sur la période, dernière séance (tout l'historique). */
    data class MuscleStat(val muscle: Muscle, val count: Int, val lastDate: Long?)

    fun muscleStats(history: List<HistoryEntry>, days: Int, now: Long = System.currentTimeMillis()): List<MuscleStat> {
        val since = now - days * 86_400_000L
        return Muscle.entries.map { m ->
            val with = history.filter { (it.muscles[m.name] ?: 0) > 0 }
            MuscleStat(m, with.filter { it.date >= since }.sumOf { it.muscles[m.name] ?: 0 }, with.maxOfOrNull { it.date })
        }
    }

    /** « aujourd'hui », « hier », « il y a 5 jours », « jamais ». */
    fun sinceLabel(date: Long?, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
        if (date == null) return "jamais"
        val days = java.time.temporal.ChronoUnit.DAYS.between(day(date, zone), day(now, zone))
        return when (days) {
            0L -> "aujourd'hui"
            1L -> "hier"
            else -> "il y a $days jours"
        }
    }

    /** Fusion d'un import : union par identifiant, triée par date. */
    fun merge(a: List<HistoryEntry>, b: List<HistoryEntry>): List<HistoryEntry> =
        (a.associateBy { it.id } + b.associateBy { it.id }).values.sortedBy { it.date }

    /** Semaines du mois (lundi d'abord), null hors du mois. */
    fun monthGrid(month: YearMonth): List<List<LocalDate?>> {
        val lead = month.atDay(1).dayOfWeek.value - 1
        val cells = MutableList<LocalDate?>(lead) { null }
        for (d in 1..month.lengthOfMonth()) cells += month.atDay(d)
        while (cells.size % 7 != 0) cells += null
        return cells.chunked(7)
    }
}
