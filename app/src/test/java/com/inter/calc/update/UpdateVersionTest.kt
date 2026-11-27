package com.inter.calc.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun detectsNewerPatchVersion() {
        assertTrue(isNewerVersion("1.2.2", "1.2.1"))
    }

    @Test
    fun sameVersionIsNotNewer() {
        assertFalse(isNewerVersion("1.2.1", "1.2.1"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(isNewerVersion("1.2.0", "1.2.1"))
    }

    @Test
    fun comparesNumericSegmentsInsteadOfStrings() {
        assertTrue(isNewerVersion("1.10.0", "1.9.9"))
        assertFalse(isNewerVersion("1.9.9", "1.10.0"))
    }

    @Test
    fun acceptsVPrefixAndSuffix() {
        assertTrue(isNewerVersion("v1.3.0", "1.2.1"))
        assertTrue(isNewerVersion("1.2.1-beta", "1.2.0"))
        assertEquals(listOf(1, 2, 1), versionParts("v1.2.1"))
    }

    @Test
    fun cleansMarkdownReleaseNotes() {
        val notes = cleanReleaseNotes("## v1.2.1\n- 新增检查更新\n- 修复 `崩溃` 问题\n")
        assertTrue(notes.contains("v1.2.1"))
        assertTrue(notes.contains("• 新增检查更新"))
        assertTrue(notes.contains("修复 崩溃 问题"))
        assertFalse(notes.contains("##"))
        assertFalse(notes.contains("`"))
    }
}
