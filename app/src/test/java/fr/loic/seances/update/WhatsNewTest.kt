package fr.loic.seances.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WhatsNewTest {
    @Test
    fun parsesBulletsAndBold() {
        val lines = WhatsNew.parse("Intro.\n\n- **Quoi de neuf** : une fenêtre.\n- Simple\n")
        assertEquals(3, lines.size)
        assertEquals(false, lines[0].bullet)
        assertEquals(listOf(NoteSpan("Quoi de neuf", true), NoteSpan(" : une fenêtre.", false)), lines[1].spans)
        assertEquals(listOf(NoteSpan("Simple", false)), lines[2].spans)
    }

    @Test
    fun currentVersionHasNotes() {
        val version = File("build.gradle.kts").readText().let { Regex("""versionName = "(.+)"""").find(it)!!.groupValues[1] }
        val notes = File("src/main/assets/release-notes/$version.md")
        assertTrue("Notes de version manquantes : $notes", notes.exists() && notes.readText().isNotBlank())
    }
}
