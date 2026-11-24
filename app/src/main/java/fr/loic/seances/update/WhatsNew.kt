package fr.loic.seances.update

import android.content.Context

/** Un morceau de texte des notes, en gras ou non. */
data class NoteSpan(val text: String, val bold: Boolean)

/** Une ligne des notes : puce (« - ») ou paragraphe. */
data class NoteLine(val bullet: Boolean, val spans: List<NoteSpan>)

/**
 * « Quoi de neuf » : au premier lancement après une mise à jour, on montre les notes de la version
 * (app/src/main/assets/release-notes/<version>.md, le même fichier que la release GitHub).
 */
object WhatsNew {
    /** Notes de [version] embarquées dans l'APK, ou null s'il n'y en a pas. */
    fun notes(context: Context, version: String): String? =
        runCatching { context.assets.open("release-notes/$version.md").bufferedReader().use { it.readText() } }
            .getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Vrai si l'app vient d'être mise à jour (pas installée pour la première fois) et que les nouveautés
     * de [current] n'ont pas encore été vues.
     */
    fun shouldShow(context: Context, lastSeen: String?, current: String): Boolean {
        if (lastSeen == current) return false
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() ?: return false
        return info.lastUpdateTime > info.firstInstallTime
    }

    /** Markdown minimal des notes : lignes « - » en puces, **gras**, lignes vides ignorées. */
    fun parse(md: String): List<NoteLine> = md.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val bullet = line.startsWith("- ") || line.startsWith("* ")
            NoteLine(bullet, spans(if (bullet) line.substring(2).trim() else line))
        }

    private fun spans(text: String): List<NoteSpan> =
        text.split("**").mapIndexedNotNull { i, part -> part.takeIf { it.isNotEmpty() }?.let { NoteSpan(it, i % 2 == 1) } }
}
