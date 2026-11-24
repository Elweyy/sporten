package fr.loic.seances.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    @Test
    fun comparesNumberByNumber() {
        assertTrue(Versions.isNewer("v1.10", "1.9"))
        assertTrue(Versions.isNewer("2.0", "1.9"))
        assertTrue(Versions.isNewer("v1.9.1", "1.9"))
        assertFalse(Versions.isNewer("v1.9", "1.9"))
        assertFalse(Versions.isNewer("1.8", "1.9"))
        assertFalse(Versions.isNewer("v1.9.0", "1.9"))
    }
}
