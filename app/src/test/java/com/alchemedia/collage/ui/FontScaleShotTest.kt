package com.alchemedia.collage.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.alchemedia.collage.Strings
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.LayoutCatalog
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The screens with the system font size turned up to 150% (build/shots/ui/bigtext-*.png), to see that nothing is cut off. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class FontScaleShotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        RuntimeEnvironment.setFontScale(1.5f)
        h = UiHarness(rule)
    }

    @After fun tearDown() { Strings.setLanguage(""); RuntimeEnvironment.setFontScale(1f) }

    @Test fun bigText() {
        val p = h.makeProject("hero", "Weekend")
        h.start()
        h.shot("bigtext-1-home")
        h.vm.openSheet(Sheet.Pro); h.shot("bigtext-2-pro"); h.vm.closeSheet()
        h.vm.openProject(p.id)
        val ed = h.vm.editor!!
        h.shot("bigtext-3-editor")
        ed.selectTab(EditorTab.Style); h.shot("bigtext-4-style")
        ed.selectTab(EditorTab.Text); h.shot("bigtext-5-text")
        h.vm.openSheet(Sheet.Export); h.shot("bigtext-6-export")
    }
}
