package com.alchemedia.collage.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.LayoutCatalog
import com.alchemedia.collage.ui.components.LocalApp
import com.alchemedia.collage.ui.theme.CollageTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The app on a landscape tablet and on a portrait tablet: screens stay phone-shaped in the middle (build/shots/ui/tablet-*.png). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land-xhdpi")
class LargeScreenLandscapeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        h = UiHarness(rule)
    }

    private fun startFramed() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalApp provides h.app) { CollageTheme(false) { WideScreenFrame { AppRoot(h.vm) } } }
        }
        h.settle()
    }

    @Test fun tourOnALandscapeTablet() {
        val p = h.makeProject("hero", "Weekend")
        startFramed()
        h.shot("tablet-land-1-home")
        h.vm.openProject(p.id)
        h.shot("tablet-land-2-editor")
        h.vm.openSheet(Sheet.Pro); h.shot("tablet-land-3-pro")
        h.vm.closeSheet()
        h.vm.openSheet(Sheet.About); h.shot("tablet-land-4-about")
        assertTrue(true)
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-xhdpi")
class LargeScreenPortraitTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        h = UiHarness(rule)
    }

    @Test fun tourOnAPortraitTablet() {
        val p = h.makeProject("hero", "Weekend")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalApp provides h.app) { CollageTheme(false) { WideScreenFrame { AppRoot(h.vm) } } }
        }
        h.settle()
        h.shot("tablet-port-1-home")
        h.vm.openProject(p.id)
        h.shot("tablet-port-2-editor")
        assertTrue(true)
    }
}
