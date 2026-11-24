package fr.loic.seances.session

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Phrases des annonces vocales : mêmes textes que la web app (model.js, announcement). */
object Announcements {
    const val TEN_SECONDS = "Plus que dix secondes."
    const val FINISHED = "Séance terminée. Bravo !"

    /** Phrase dite au début de l'étape [i]. */
    fun forStep(steps: List<Step>, i: Int, cycles: Int): String {
        val lastWork = steps.indexOfLast { it is Step.Work }
        return when (val step = steps[i]) {
            is Step.Prepare -> "Prépare-toi. Premier exercice : ${step.first.name}."
            is Step.Transition ->
                "${if (i + 1 == lastWork) "Dernier exercice" else "Exercice suivant"} : ${step.next.exercise.name}."
            is Step.Rest -> "Repos. Cycle ${step.afterCycle} sur $cycles terminé."
            is Step.NextPart -> "Séance terminée. Pause avant ${step.title}. Premier exercice : ${step.first.name}."
            is Step.Work -> {
                val prev = steps.getOrNull(i - 1)
                val reps = step.reps?.let { "$it répétitions, " } ?: ""
                if (prev is Step.Transition || prev is Step.Prepare || prev is Step.NextPart) "${reps}c'est parti !".replaceFirstChar { it.uppercase() }
                else "${if (i == lastWork) "Dernier exercice : " else ""}${step.exercise.name}, ${reps}c'est parti !"
            }
        }
    }
}

/** Synthèse vocale du téléphone, en français. Silencieuse tant qu'elle n'est pas prête ou si indisponible. */
class Speaker(context: Context) {
    private var ready = false
    private var pending: String? = null
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val lang = tts.setLanguage(Locale.FRANCE)
            ready = lang != TextToSpeech.LANG_MISSING_DATA && lang != TextToSpeech.LANG_NOT_SUPPORTED
            tts.setSpeechRate(1.05f)
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            if (ready) pending?.let { say(it) }
            pending = null
        }
    }

    fun say(text: String) {
        if (!ready) {
            pending = text // la première phrase (« Prépare-toi… ») arrive souvent avant la fin de l'init
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sporten")
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    fun release() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }
}
