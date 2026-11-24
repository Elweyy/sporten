package fr.loic.seances.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/** Ce que contient un code de partage : la séance et les exercices perso qu'elle utilise. */
@Serializable
data class SharePayload(
    val w: WorkoutPlan,
    val x: List<ExerciseDef> = emptyList(),
)

/**
 * Code de partage d'une séance, utilisé tel quel dans le QR code et dans le texte à envoyer :
 * « SDL1- » suivi du JSON compressé en base64 (caractères sans risque dans un SMS ou une URL).
 */
object ShareCode {
    private const val PREFIX = "SDL1-"
    private val CODE = Regex("""SDL1-([A-Za-z0-9_-]+)""")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(plan: WorkoutPlan, exercises: Map<String, ExerciseDef>): String {
        val custom = plan.blocks.flatMap { it.exerciseIds }.distinct()
            .mapNotNull { exercises[it] }.filter { it.custom }
        val payload = SharePayload(plan.copy(id = "s"), custom)
        val raw = json.encodeToString(SharePayload.serializer(), payload).encodeToByteArray()
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(deflate(raw))
    }

    /** Retrouve un code dans un texte collé (message entier accepté). Null si absent ou illisible. */
    fun decode(text: String): SharePayload? {
        val body = CODE.find(text)?.groupValues?.get(1) ?: return null
        return runCatching {
            val raw = inflate(Base64.getUrlDecoder().decode(body))
            json.decodeFromString(SharePayload.serializer(), raw.decodeToString())
        }.getOrNull()
    }

    /** Texte envoyé par message. Le code est à la fin pour que le copier-coller soit facile. */
    fun message(plan: WorkoutPlan, code: String): String =
        "Séance « ${plan.title} » partagée depuis Sporten 💪\n" +
            "Pour l'ajouter : Sporten → Importer → colle ce message ou le code :\n\n$code"

    private fun deflate(data: ByteArray): ByteArray {
        val d = Deflater(Deflater.BEST_COMPRESSION, true)
        d.setInput(data)
        d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inf = Inflater(true)
        inf.setInput(data + 0.toByte()) // octet de bourrage conseillé en mode « nowrap »
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!inf.finished()) {
            val n = inf.inflate(buf)
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
            out.write(buf, 0, n)
            require(out.size() < 1_000_000) { "code trop gros" }
        }
        inf.end()
        return out.toByteArray()
    }
}
