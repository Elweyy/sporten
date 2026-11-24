package fr.loic.seances.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.loic.seances.anim.Anims
import fr.loic.seances.anim.StickFigure
import fr.loic.seances.data.Catalog
import fr.loic.seances.data.ExerciseDef
import fr.loic.seances.data.Library
import fr.loic.seances.data.LibraryOps
import fr.loic.seances.data.Muscle
import java.text.Normalizer

private fun String.normalized() =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

/**
 * Catalogue d'exercices. Avec [onPick], sert de sélecteur (pour l'éditeur de séance) :
 * toucher un exercice le choisit.
 */
@Composable
fun CatalogScreen(
    library: Library,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPick: ((ExerciseDef) -> Unit)? = null,
) {
    val data by library.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    var detail by remember { mutableStateOf<ExerciseDef?>(null) }
    var editing by remember { mutableStateOf<Pair<ExerciseDef, Boolean>?>(null) } // (brouillon, nouveau ?)

    editing?.let { (draft, isNew) ->
        ExerciseEditorScreen(
            initial = draft,
            isNew = isNew,
            onCancel = { editing = null },
            onSave = { saved ->
                library.saveExercise(saved)
                editing = null
                detail = null
                if (isNew && onPick != null) onPick(saved.copy(custom = true))
            },
            modifier = modifier,
        )
        return
    }

    BackHandler(onBack = onBack)

    val all = (Catalog.builtIn + data.customExercises)
    val q = query.trim().normalized()
    val filtered = all
        .filter { muscle == null || it.muscle == muscle }
        .filter { q.isEmpty() || it.name.normalized().contains(q) }
        .sortedWith(compareBy({ it.muscle.ordinal }, { it.name.normalized() }))
    val grouped = filtered.groupBy { it.muscle }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("←  Retour", color = Palette.textMuted) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    editing = ExerciseDef(
                        id = LibraryOps.newId("perso"), name = "", cue = "",
                        muscle = muscle ?: Muscle.ABDOS, animId = "plank", custom = true,
                    ) to true
                }) { Text("+ Nouvel exercice", color = Palette.accentText, fontWeight = FontWeight.SemiBold) }
            }
            Text(
                if (onPick != null) "Choisir un exercice" else "Catalogue",
                style = titleStyle(30), color = Palette.text,
            )
            Text(
                "${all.size} exercices · poids du corps, haltères, chaise" +
                    if (onPick != null) " · touche un exercice pour l'ajouter" else "",
                color = Palette.textMuted, fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Rechercher…") },
                singleLine = true,
                colors = textFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            MuscleChips(muscle, { muscle = it })
            Spacer(Modifier.height(4.dp))
        }
        if (filtered.isEmpty()) {
            item { Text("Aucun exercice trouvé.", color = Palette.textMuted, modifier = Modifier.padding(16.dp)) }
        }
        grouped.forEach { (m, list) ->
            item(key = "h_${m.name}") {
                Text(
                    m.label, color = Palette.work, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                )
            }
            items(list, key = { it.id }) { ex ->
                ExerciseRow(
                    ex,
                    onClick = { if (onPick != null) onPick(ex) else detail = ex },
                    trailing = {
                        if (onPick != null) {
                            TextButton(onClick = { detail = ex }) { Text("voir", color = Palette.textMuted, fontSize = 12.sp) }
                        }
                    },
                )
            }
        }
    }

    detail?.let { ex ->
        ExerciseDetailDialog(
            ex = ex,
            onDismiss = { detail = null },
            onPick = onPick?.let { pick -> { detail = null; pick(ex) } },
            onEdit = { editing = ex to false; detail = null },
            onVariant = {
                editing = ex.copy(id = LibraryOps.newId("perso"), name = ex.name + " (variante)", custom = true) to true
                detail = null
            },
            onDelete = {
                val users = LibraryOps.usages(data, ex.id)
                if (users.isEmpty()) {
                    library.deleteExercise(ex.id)
                    detail = null
                } else {
                    Toast.makeText(
                        context,
                        "Utilisé dans : ${users.joinToString { it.title }}. Retire-le d'abord de ces séances.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
        )
    }
}

@Composable
private fun ExerciseDetailDialog(
    ex: ExerciseDef,
    onDismiss: () -> Unit,
    onPick: (() -> Unit)?,
    onEdit: () -> Unit,
    onVariant: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(themedShape(20.dp))
                .background(Palette.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(ex.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.text, textAlign = TextAlign.Center)
            Text(ex.muscle.label + if (ex.custom) " · perso" else "", color = Palette.textMuted, fontSize = 13.sp)
            StickFigure(Anims.get(ex.animId), Modifier.size(240.dp), accentColor = Palette.work)
            Text(ex.cue, color = Palette.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            if (onPick != null) {
                Button(
                    onClick = onPick,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Choisir", color = Palette.onWork, fontWeight = FontWeight.Bold) }
            }
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                if (ex.custom) {
                    TextButton(onClick = onEdit) { Text("Modifier") }
                    if (confirmDelete) {
                        TextButton(onClick = onDelete) { Text("Confirmer ?", color = Palette.danger) }
                    } else {
                        TextButton(onClick = { confirmDelete = true }) { Text("Supprimer", color = Palette.danger) }
                    }
                } else {
                    TextButton(onClick = onVariant) { Text("Créer une variante") }
                }
                TextButton(onClick = onDismiss) { Text("Fermer", color = Palette.textMuted) }
            }
        }
    }
}

/** Création / modification d'un exercice perso : nom, consigne, groupe, animation. */
@Composable
fun ExerciseEditorScreen(
    initial: ExerciseDef,
    isNew: Boolean,
    onCancel: () -> Unit,
    onSave: (ExerciseDef) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initial.name) }
    var cue by remember { mutableStateOf(initial.cue) }
    var muscle by remember { mutableStateOf(initial.muscle) }
    var animId by remember { mutableStateOf(if (Anims.has(initial.animId)) initial.animId else "plank") }
    BackHandler(onBack = onCancel)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) { Text("Annuler", color = Palette.textMuted) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onSave(initial.copy(name = name.trim(), cue = cue.trim(), muscle = muscle, animId = animId)) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Palette.work),
            ) { Text("Enregistrer", color = Palette.onWork, fontWeight = FontWeight.Bold) }
        }
        Text(
            if (isNew) "Nouvel exercice" else "Modifier l'exercice",
            style = titleStyle(26), color = Palette.text,
        )
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Nom") }, singleLine = true,
            colors = textFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = cue, onValueChange = { cue = it },
            label = { Text("Consigne (affichée pendant la séance)") }, minLines = 2,
            colors = textFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
        Text("Groupe musculaire", color = Palette.text, fontWeight = FontWeight.SemiBold)
        MuscleChips(muscle, { if (it != null) muscle = it }, allowAll = false)

        Text("Animation", color = Palette.text, fontWeight = FontWeight.SemiBold)
        Text("Choisis le mouvement le plus proche.", color = Palette.textMuted, fontSize = 13.sp)
        Anims.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { e ->
                    val selected = e.id == animId
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(themedShape(12.dp))
                            .background(Palette.surface)
                            .border(2.dp, if (selected) Palette.work else Color.Transparent, themedShape(12.dp))
                            .clickable { animId = e.id }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        StickFigure(
                            e.animation,
                            Modifier.size(84.dp),
                            playing = selected,
                            accentColor = Palette.work,
                        )
                        Text(
                            e.label, color = if (selected) Palette.work else Palette.textMuted,
                            fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.width(1.dp))
    }
}
