package app.kiosksync.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun parsesStrictNumericVersions() {
        assertEquals(SemanticVersion(1, 2, 3), parseAppVersion("1.2.3"))
        assertEquals(SemanticVersion(1, 2, 3), parseAppVersion(" v1.2.3 "))
        assertNull(parseAppVersion("1.2"))
        assertNull(parseAppVersion("1.2.3-beta"))
        assertNull(parseAppVersion("1.2.x"))
        assertNull(parseAppVersion(""))
        assertTrue(isValidAppVersion("10.0.20"))
    }

    @Test
    fun sameVersionNeedsNoUpdate() {
        val plan = AppVersionPolicy.plan("1.0.0", "1.0.0")
        assertEquals(UpdateUrgency.NONE, plan.urgency)
    }

    @Test
    fun olderScreenInSameMajorIsOptional() {
        val plan = AppVersionPolicy.plan("1.2.0", "1.1.9")
        assertEquals(UpdateUrgency.OPTIONAL, plan.urgency)
        assertEquals("1.2.0", plan.targetVersion)
    }

    @Test
    fun newerScreenInSameMajorIsCompatible() {
        assertEquals(UpdateUrgency.NONE, AppVersionPolicy.plan("1.2.0", "1.3.0").urgency)
    }

    @Test
    fun differentMajorForcesUpdateInBothDirections() {
        assertEquals(UpdateUrgency.FORCED, AppVersionPolicy.plan("2.0.0", "1.9.9").urgency)
        assertEquals(UpdateUrgency.FORCED, AppVersionPolicy.plan("1.9.9", "2.0.0").urgency)
        assertFalse(AppVersionPolicy.isCompatible("1.0.0", "2.0.0"))
        assertTrue(AppVersionPolicy.isCompatible("1.5.0", "1.0.0"))
    }

    @Test
    fun unknownScreenVersionDoesNotTriggerAnUpdate() {
        assertEquals(UpdateUrgency.NONE, AppVersionPolicy.plan("1.0.0", null).urgency)
        assertEquals(UpdateUrgency.NONE, AppVersionPolicy.plan("1.0.0", "garbage").urgency)
    }
}
