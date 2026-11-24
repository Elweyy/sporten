package fr.loic.seances.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.loic.seances.update.UpdateState
import fr.loic.seances.update.Updater

/** Bandeau de l'accueil quand une nouvelle version est disponible (rien sinon). */
@Composable
fun UpdateBanner(updater: Updater) {
    val state by updater.state.collectAsStateWithLifecycle()
    val release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.NeedsPermission -> s.release
        is UpdateState.Ready -> s.release
        is UpdateState.Failed -> s.release
        else -> null
    } ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(themedShape(16.dp))
            .background(Palette.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✨", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Nouvelle version ${release.version}", color = Palette.text, fontWeight = FontWeight.SemiBold)
                Text(
                    when (val s = state) {
                        is UpdateState.Downloading -> "Téléchargement… ${(s.progress * 100).toInt()} %"
                        is UpdateState.NeedsPermission -> "Autorise Sporten à installer des applis, puis reviens ici"
                        is UpdateState.Ready -> "Confirme l'installation. Tes séances et ton historique sont gardés."
                        is UpdateState.Failed -> s.message
                        else -> "Tu as la ${updaterVersion(updater)}. Tes séances et ton historique sont gardés."
                    },
                    color = Palette.textMuted, fontSize = 13.sp,
                )
            }
        }
        val s = state
        if (s is UpdateState.Downloading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth(), color = Palette.work)
        }
        if (s is UpdateState.Available && s.release.notes.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(s.release.notes.take(400), color = Palette.textMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        if (s !is UpdateState.Downloading) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = updater::dismiss) { Text("Plus tard", color = Palette.textMuted) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        when (s) {
                            is UpdateState.NeedsPermission -> updater.install(s.release, s.file)
                            is UpdateState.Ready -> updater.install(s.release, s.file)
                            else -> updater.download(release)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
                ) {
                    Text(
                        when (s) {
                            is UpdateState.NeedsPermission, is UpdateState.Ready -> "Installer"
                            is UpdateState.Failed -> "Réessayer"
                            else -> "Mettre à jour"
                        },
                        color = Palette.onWork, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

private fun updaterVersion(updater: Updater) = "version ${updater.currentVersion}"

/** Section « Mises à jour » des réglages : version installée et vérification à la demande. */
@Composable
fun UpdateSettings(updater: Updater, onShowWhatsNew: (() -> Unit)? = null) {
    val state by updater.state.collectAsStateWithLifecycle()
    Text("Mises à jour", color = Palette.accentText, fontWeight = FontWeight.SemiBold)
    Text(
        if (!updater.enabled) "Version ${updater.currentVersion} (version de développement : pas de mises à jour automatiques)"
        else "Version ${updater.currentVersion}. Sporten regarde à chaque lancement si une nouvelle version est publiée.",
        color = Palette.textMuted, fontSize = 13.sp,
    )
    if (onShowWhatsNew != null) {
        TextButton(onClick = onShowWhatsNew) { Text("✨ Voir les nouveautés de la ${updater.currentVersion}", color = Palette.accentText) }
    }
    if (updater.enabled) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { updater.check(manual = true) }, enabled = state !is UpdateState.Checking) {
                Text("Vérifier maintenant")
            }
            Spacer(Modifier.width(12.dp))
            Text(
                when (val s = state) {
                    UpdateState.Checking -> "Recherche…"
                    UpdateState.UpToDate -> "✓ À jour"
                    is UpdateState.Failed -> if (s.release == null) s.message else ""
                    is UpdateState.Idle -> ""
                    else -> "Nouvelle version : voir l'accueil"
                },
                color = Palette.textMuted, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
        }
    }
}
