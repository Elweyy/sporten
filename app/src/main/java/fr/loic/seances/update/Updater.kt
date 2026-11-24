package fr.loic.seances.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import fr.loic.seances.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Une release publiée sur GitHub, avec son APK. */
data class Release(val version: String, val apkUrl: String, val sizeBytes: Long, val notes: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    /** Il faut d'abord autoriser Sporten à installer des applis (réglage Android, une seule fois). */
    data class NeedsPermission(val release: Release, val file: File) : UpdateState
    data class Ready(val release: Release, val file: File) : UpdateState
    data class Failed(val message: String, val release: Release? = null) : UpdateState
}

object Versions {
    /** "v1.10" > "1.9" : compare nombre par nombre, le « v » initial est ignoré. */
    fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V").split('.').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
}

@Serializable
private data class GhAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GhAsset> = emptyList(),
)

/**
 * Mises à jour depuis les releases GitHub : au lancement, on regarde la dernière release du dépôt
 * [BuildConfig.UPDATE_REPO]. Si elle est plus récente, l'accueil le propose ; un appui télécharge
 * l'APK puis ouvre l'installateur d'Android (la mise à jour garde les données).
 * Aucune donnée n'est envoyée : une simple lecture publique de GitHub.
 */
class Updater(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val enabled: Boolean get() = BuildConfig.UPDATE_REPO.isNotBlank() && !BuildConfig.UPDATE_REPO.startsWith("OWNER/")
    val currentVersion: String get() = BuildConfig.VERSION_NAME

    private val dir: File get() = File(context.cacheDir, "update").apply { mkdirs() }

    /** [manual] : demandé depuis les réglages (les erreurs sont alors affichées). */
    fun check(manual: Boolean = false) {
        if (!enabled) return
        val s = _state.value
        if (s is UpdateState.Checking || s is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { fetchLatest() } }
            _state.value = result.fold(
                onSuccess = { r ->
                    if (r != null && Versions.isNewer(r.version, currentVersion)) UpdateState.Available(r)
                    else {
                        cleanup()
                        UpdateState.UpToDate
                    }
                },
                onFailure = { if (manual) UpdateState.Failed("Impossible de joindre GitHub. Connexion internet ?") else UpdateState.Idle },
            )
        }
    }

    private fun fetchLatest(): Release? {
        val conn = open("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conn.responseCode == 404) return null // aucune release publiée
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val gh = json.decodeFromString<GhRelease>(conn.inputStream.bufferedReader().use { it.readText() })
            if (gh.draft || gh.prerelease) return null
            val apk = gh.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null
            return Release(gh.tag.removePrefix("v"), apk.url, apk.size, gh.body.orEmpty().trim())
        } finally {
            conn.disconnect()
        }
    }

    /** Télécharge l'APK de [release] puis lance l'installation. */
    fun download(release: Release) {
        if (_state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Downloading(release, 0f)
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    cleanup()
                    val part = File(dir, "Sporten-${release.version}.apk.part")
                    val conn = open(release.apkUrl) // GitHub redirige vers son stockage : suivi automatiquement (https → https)
                    try {
                        if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
                        val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
                        conn.inputStream.use { input ->
                            part.outputStream().use { out ->
                                val buf = ByteArray(64 * 1024)
                                var done = 0L
                                var lastEmit = 0L
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    out.write(buf, 0, n)
                                    done += n
                                    if (total > 0 && done - lastEmit > 128 * 1024) {
                                        lastEmit = done
                                        _state.value = UpdateState.Downloading(release, (done.toFloat() / total).coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }
                        if (release.sizeBytes > 0 && part.length() != release.sizeBytes) error("Téléchargement incomplet")
                    } finally {
                        conn.disconnect()
                    }
                    File(dir, "Sporten-${release.version}.apk").also { f -> f.delete(); part.renameTo(f) }
                }
            }
            result.fold(
                onSuccess = { file -> install(release, file) },
                onFailure = { _state.value = UpdateState.Failed("Le téléchargement a échoué. Réessaie.", release) },
            )
        }
    }

    /**
     * Ouvre l'installateur d'Android. La première fois, Android demande d'autoriser Sporten à installer
     * des applis : on ouvre ce réglage, puis l'utilisateur relance l'installation.
     */
    fun install(release: Release, file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(release, file)
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        _state.value = UpdateState.Ready(release, file)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Retour du réglage « installer des applis » : si c'est autorisé, on lance l'installation. */
    fun onResume() {
        val s = _state.value
        if (s is UpdateState.NeedsPermission && context.packageManager.canRequestPackageInstalls()) install(s.release, s.file)
    }

    /** Masque le bandeau jusqu'au prochain lancement. */
    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    private fun cleanup() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Sporten-Android")
        }
}
