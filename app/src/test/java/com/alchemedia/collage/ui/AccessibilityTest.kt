package com.alchemedia.collage.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.LayoutCatalog
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screen-reader checks: every control that can be tapped must have a name TalkBack can read, and none may be
 * too small to hit. The sizes are printed so they can be reviewed; the names are asserted.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class AccessibilityTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness
    private val unnamed = mutableListOf<String>()
    private val small = sortedSetOf<String>()

    @Before fun setUp() {
        TestAssets.loadCatalog()
        LayoutCatalog.setMine(emptyList())
        h = UiHarness(rule)
    }

    private fun audit(screen: String) {
        h.settle(300)
        val density = rule.density.density
        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val cfg = node.config
            val name = (cfg.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ") ?: "") +
                (cfg.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text } ?: "") +
                (cfg.getOrNull(SemanticsProperties.EditableText)?.text ?: "") +
                (cfg.getOrNull(SemanticsProperties.StateDescription) ?: "")
            // Compose widens the tappable area of small controls to the minimum touch target (48dp), so measure that
            val touch = node.touchBoundsInRoot
            val w = touch.width / density
            val hgt = touch.height / density
            if (w <= 0f || hgt <= 0f) return@forEach // scrolled out of view
            if (name.isBlank()) unnamed += "$screen at ${node.positionInRoot}"
            // a name with a size: anything under 48dp in either direction is below the recommended size
            else if ((w < 47f || hgt < 47f) && cfg.getOrNull(SemanticsActions.OnClick) != null) small += "$screen: \"${name.trim().take(40)}\" ${w.toInt()}x${hgt.toInt()}dp"
        }
    }

    @Test fun everyControlHasAName() {
        val p = h.makeProject("hero", "Weekend")
        h.start()
        audit("Home")
        h.vm.openSheet(Sheet.Language); audit("Language"); h.vm.closeSheet()
        h.vm.openSheet(Sheet.Pro); audit("Pro"); h.vm.closeSheet()
        h.vm.openSheet(Sheet.About); audit("About"); h.vm.closeSheet()
        h.vm.openBrowse(BrowseTarget.Home); audit("Browse"); h.vm.closeOverlay()
        h.vm.openHistory(); audit("History"); h.vm.closeOverlay()
        h.vm.openDesigner(BrowseTarget.Home); audit("Designer"); h.vm.closeOverlay()
        h.vm.openProject(p.id)
        val ed = h.vm.editor!!
        EditorTab.entries.forEach { ed.selectTab(it); audit("Editor ${it.name}") }
        ed.selectTab(EditorTab.Photo); ed.selectCell(0); audit("Editor Photo with a cell")
        ed.selectTab(EditorTab.Style); StyleSub.entries.forEach { ed.styleSub = it; audit("Style ${it.name}") }
        ed.selectTab(EditorTab.Text); ed.addItem(com.alchemedia.collage.model.CanvasItem.text("Hi")); TextSub.entries.forEach { ed.textSub = it; audit("Text ${it.name}") }
        h.vm.openSheet(Sheet.Export); audit("Export")
        println("ACCESSIBILITY unnamed controls (${unnamed.size}):\n" + unnamed.distinct().joinToString("\n"))
        println("ACCESSIBILITY targets under 48dp (${small.size}):\n" + small.joinToString("\n"))
        assertTrue("controls under 48dp: $small", small.isEmpty())
        assertTrue("controls with no name: $unnamed", unnamed.isEmpty())
    }
}
