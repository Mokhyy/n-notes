package com.xnotes.stylus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusSettingsTest {

    private fun device(name: String, vendor: Int = 1, product: Int = 2) =
        StylusDevice(id = 7, name = name, vendorId = vendor, productId = product, sources = 0)

    @Test
    fun aPenWithNoProfileGetsTheGeneralSettings() {
        val settings = StylusSettings(
            general = StylusGeneralSettings(
                penButtonTool = "pan",
                penButtonHover = true,
                doubleTapAction = "undo",
                buttonTapAction = "redo",
                hoverPreview = false,
                hoverDistancePreview = false,
            ),
            profiles = emptyList(),
        )

        val effective = settings.effectiveFor(device("Unknown Pen"))

        assertNull(effective.profileId)
        assertEquals("pan", effective.penButtonTool)
        assertTrue(effective.penButtonHover)
        assertEquals("undo", effective.doubleTapAction)
        assertEquals("redo", effective.buttonTapAction)
        assertFalse(effective.hoverPreview)
        assertFalse(effective.hoverDistancePreview)
    }

    @Test
    fun aProfileOverridesOnlyTheFieldsItSets() {
        val profile = StylusProfile(
            id = "custom",
            name = "Custom",
            match = StylusDeviceMatch(nameContains = "Custom Pen"),
            penButtonTool = "eraser",
            doubleTapAction = "none",
            hoverPreview = false,
            hoverDistancePreview = true,
        )
        val settings = StylusSettings(
            general = StylusGeneralSettings(penButtonTool = "pan", penButtonHover = true, doubleTapAction = "undo"),
            profiles = listOf(profile),
        )

        val effective = settings.effectiveFor(device("My Custom Pen"))

        assertEquals("custom", effective.profileId)
        assertEquals("eraser", effective.penButtonTool)
        assertEquals("none", effective.doubleTapAction)
        assertFalse(effective.hoverPreview)
        assertTrue(effective.hoverDistancePreview)
        assertTrue("unset fields inherit the general value", effective.penButtonHover)
    }

    @Test
    fun aDisabledProfileNeverMatches() {
        val disabled = StylusProfile(
            id = "off",
            name = "Off",
            match = StylusDeviceMatch(nameContains = "M-Pencil"),
            enabled = false,
        )
        val effective = StylusSettings(profiles = listOf(disabled)).effectiveFor(device("Huawei M-Pencil"))

        assertNull(effective.profileId)
    }

    @Test
    fun aPinnedProfileWinsOverDeviceMatching() {
        val pinned = StylusProfile(id = "pinned", name = "Pinned", penButtonTool = "eraser")
        val matched = StylusProfile(
            id = "matched",
            name = "Matched",
            match = StylusDeviceMatch(nameContains = "M-Pencil"),
            penButtonTool = "pan",
        )
        val settings = StylusSettings(
            profiles = listOf(pinned, matched),
            activeProfileId = "pinned",
        )

        assertEquals("pinned", settings.effectiveFor(device("Huawei M-Pencil")).profileId)
    }

    @Test
    fun thePageKeysRunTheirOwnTapActions() {
        val effective = StylusSettings(
            general = StylusGeneralSettings(
                buttonTapAction = "eraser",
                button1TapAction = "page_up",
                button2TapAction = "page_down",
            ),
            profiles = emptyList(),
        ).effectiveFor(null)

        assertEquals("page_up", effective.tapActionFor(android.view.KeyEvent.KEYCODE_PAGE_UP))
        assertEquals("page_down", effective.tapActionFor(android.view.KeyEvent.KEYCODE_PAGE_DOWN))
        assertEquals("eraser", effective.tapActionFor(StylusKeys.BARREL_DOUBLE_TAP))
    }

    @Test
    fun aMatchNeedsAnIdentifyingField() {
        assertFalse(StylusDeviceMatch().matches(device("Anything")))
    }

    @Test
    fun aProfileAddsItsKeycodesToTheGeneralSet() {
        val profile = StylusProfile(
            id = "extra",
            name = "Extra",
            match = StylusDeviceMatch(nameContains = "Extra Pen"),
            heldKeycodes = setOf(999),
        )
        val effective = StylusSettings(
            general = StylusGeneralSettings(heldKeycodes = setOf(1)),
            profiles = listOf(profile),
        ).effectiveFor(device("Extra Pen"))

        assertTrue(effective.isHeldButton(1))
        assertTrue(effective.isHeldButton(999))
    }

    @Test
    fun profilesRoundTripThroughJson() {
        val profiles = listOf(
            StylusProfile(
                id = "one",
                name = "One",
                match = StylusDeviceMatch(vendorId = 10, productId = 20, nameContains = "Pen"),
                penButtonTool = "eraser",
                heldKeycodes = setOf(1, 2),
                doubleTapKeycode = 718,
                barrelDoubleTapWindowMs = 500L,
                hoverPreview = true,
                hoverDistancePreview = false,
            ),
            StylusProfile(id = "two", name = "Two"),
        )

        val back = StylusJson.profilesFromJson(StylusJson.profilesToJson(profiles))

        assertEquals(profiles, back)
    }

    @Test
    fun aMissingProfileArrayFallsBackToThePresets() {
        assertEquals(StylusPresets.profiles, StylusJson.profilesFromJson(null))
    }
}
