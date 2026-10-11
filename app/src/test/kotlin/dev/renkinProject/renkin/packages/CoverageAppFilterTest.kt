package dev.renkinProject.renkin.packages

import android.app.Application
import android.content.Context
import android.util.Xml
import androidx.test.core.app.ApplicationProvider
import dev.renkinProject.renkin.apk.indexAppDrawableNames
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.StringReader

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class CoverageAppFilterTest {
    private val component = "ComponentInfo{app/Main}"
    private val manager get() = ApplicationManager(ApplicationProvider.getApplicationContext<Context>())
    private fun read(xml: String): CoverageAppFilterRead {
        val parser = Xml.newPullParser().apply { setInput(StringReader(xml)) }
        return manager.readAppFilterForCoverage(parser, listOf(component, "ComponentInfo{app/Other}"))
    }
    @Test fun provenEmptyRootIsDifferentFromUnsupportedOrMissingXml() {
        assertTrue(read("<resources/>").proven)
        assertTrue(read("<appfilter/>").mappings.isEmpty())
        assertFalse(read("<unsupported/>").proven)
        assertNull(manager.readAppFilterForCoverage("missing.pack", emptyList()))
    }
    @Test fun lastValidDuplicateMatchesTheExistingLegacyIndexExactly() {
        val result = read("<resources><item component='$component' drawable='first'/><item component='$component' drawable='last'/></resources>")
        assertEquals("last", result.mappings[component])
        assertEquals(indexAppDrawableNames(result.elements)[component], result.mappings[component])
    }
    @Test fun soleMissingDrawableIsDeclaredBrokenButLegacyElementsStayUnchanged() {
        val malformed = read("<resources><item component='$component'/></resources>")
        assertTrue(malformed.mappings.containsKey(component))
        assertEquals("", malformed.mappings[component])
        assertTrue(malformed.elements.isEmpty())
        val duplicate = read("<resources><item component='$component' drawable='valid'/><item component='$component'/></resources>")
        assertEquals("valid", duplicate.mappings[component])
        assertEquals("valid", indexAppDrawableNames(duplicate.elements)[component])
    }
    @Test fun aliasesExactActivitiesAndFallbackAreNotConflated() {
        val result = read("<resources><iconback img1='plate'/><item component='$component' drawable='one'/><item component='ComponentInfo{app/Other}' drawable='two'/><item component='ComponentInfo{app/Unknown}' drawable='three'/></resources>")
        assertEquals(mapOf(component to "one", "ComponentInfo{app/Other}" to "two"), result.mappings)
    }
    @Test fun incompleteDynamicDeclarationsStillRemainUnprovenInCoverageMetadata() {
        val result = read("<resources><calendar component='$component'/><dynamic-clock drawable='clock'/></resources>")
        assertEquals(setOf(component), result.calendarComponents)
        assertEquals(setOf("clock"), result.clockDrawables)
        assertTrue(result.elements.isEmpty())
    }
    @Test fun malformedXmlPropagatesForRepositoryIsolationRatherThanProvingZeroMapping() {
        try { read("<resources><item"); fail("Malformed XML cannot be proven empty") }
        catch (_: org.xmlpull.v1.XmlPullParserException) { }
    }
    @Test fun sameComponentAcrossUsersIsUnsupportedWithoutInventingRoomIdentity() {
        assertEquals(setOf("app/Main", "work/Only"), uncertainLauncherKeys(
            listOf(0 to "app/Main", 10 to "app/Main", 10 to "work/Only", 0 to "app/Other"), 0))
        assertTrue(uncertainLauncherKeys(listOf(0 to "app/Main", 0 to "app/Main"), 0).isEmpty())
    }
}
