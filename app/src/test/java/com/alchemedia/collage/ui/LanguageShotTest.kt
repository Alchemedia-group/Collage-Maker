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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots of the busiest screens in each language (build/shots/ui/lang-*.png), so long words can be checked for fit. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class LanguageShotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        h = UiHarness(rule)
    }

    @After fun tearDown() { Strings.setLanguage("") }

    private fun tour(tag: String) {
        Strings.setLanguage(tag)
        val p = h.makeProject("hero", "Weekend")
        h.start()
        h.shot("lang-$tag-1-home")
        h.vm.openSheet(Sheet.Language); h.shot("lang-$tag-2-language")
        h.vm.closeSheet()
        h.vm.openProject(p.id)
        val ed = h.vm.editor!!
        ed.selectCell(0); ed.photoSub = PhotoSub.Rotate; h.shot("lang-$tag-3-photo-rotate")
        ed.photoSub = PhotoSub.Light; h.shot("lang-$tag-4-photo-light")
        ed.selectTab(EditorTab.Style); ed.styleSub = StyleSub.Frame; h.shot("lang-$tag-5-frame")
        ed.styleSub = StyleSub.Spacing; h.shot("lang-$tag-6-spacing")
        ed.selectTab(EditorTab.Shape); h.shot("lang-$tag-7-shape")
        ed.addItem(com.alchemedia.collage.model.CanvasItem.text("Hi")); ed.textSub = TextSub.Color; ed.colorTarget = ColorTarget.Gradient; h.shot("lang-$tag-8-text-gradient")
        h.vm.openSheet(Sheet.Export); h.shot("lang-$tag-9-export")
    }

    @Test fun english() = tour("en")
    @Test fun spanish() = tour("es")
    @Test fun german() = tour("de")
    @Test fun french() = tour("fr")
    @Test fun italian() = tour("it")
    @Test fun portuguese() = tour("pt")
    @Test fun hindi() = tour("hi")
    @Test fun indonesian() = tour("id")
}
