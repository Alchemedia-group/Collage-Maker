package com.alchemedia.collage.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.alchemedia.collage.TestAssets
import com.alchemedia.collage.model.CanvasItem
import com.alchemedia.collage.model.LayoutCatalog
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xhdpi")
class UiShotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var h: UiHarness

    @Before fun setUp() {
        TestAssets.loadCatalog()
        h = UiHarness(rule)
        LayoutCatalog.setMine(emptyList())
    }

    @Test fun home() {
        h.makeProject("hearts4", "Summer trip")
        h.makeProject("hero", "Weekend")
        h.makeProject("w-love", "Us")
        h.start()
        h.shot("01-home")
    }

    @Test fun newCollage() {
        h.start()
        h.vm.startNew("hearts4", samples = true)
        h.shot("02-new")
        h.vm.category = "shape"
        h.shot("02b-new-shapes")
    }

    @Test fun browse() {
        h.start()
        h.vm.startNew("g2x2", samples = true)
        h.vm.openBrowse(BrowseTarget.New)
        h.shot("03-browse")
    }

    @Test fun editorTabs() {
        val p = h.makeProject("hero", "Weekend away")
        h.start()
        h.vm.openProject(p.id)
        val ed = h.vm.editor!!
        h.shot("04-editor-layout")
        ed.selectTab(EditorTab.Shape); h.shot("05-editor-shape")
        ed.selectTab(EditorTab.Style); h.shot("06-editor-style-color")
        ed.styleSub = StyleSub.Texture; ed.setTexture("kraft", 0xFFC8A578); h.shot("07-editor-style-texture")
        ed.styleSub = StyleSub.Spacing; h.shot("08-editor-style-spacing")
        ed.styleSub = StyleSub.Filter; ed.setFilter("vintage"); h.shot("09-editor-style-filter")
        ed.selectTab(EditorTab.Photo); h.shot("10-editor-photo-none")
        ed.selectCell(1); h.shot("11-editor-photo-cell")
    }

    @Test fun editorText() {
        val p = h.makeProject("g2x2", "Birthday")
        h.start()
        h.vm.openProject(p.id)
        val ed = h.vm.editor!!
        ed.selectTab(EditorTab.Text); h.shot("12-editor-text-home")
        ed.addTemplate(com.alchemedia.collage.model.TextTemplates.all[0].make())
        ed.textSub = TextSub.Text; h.shot("13-editor-text-edit")
        ed.textSub = TextSub.Font; h.shot("14-editor-text-font")
        ed.textSub = TextSub.Color; h.shot("15-editor-text-color")
        ed.textSub = TextSub.Style; h.shot("16-editor-text-style")
        ed.textSub = TextSub.Adjust; h.shot("17-editor-text-size")
        ed.selectTab(EditorTab.Sticker); ed.selectedItemId = null; h.shot("18-editor-stickers")
        ed.addItem(CanvasItem.emoji("🎉")); h.shot("19-editor-sticker-selected")
    }

    @Test fun designer() {
        h.start()
        h.vm.openDesigner(BrowseTarget.Home)
        h.shot("20-designer")
        val d = h.vm.overlays.last() as Overlay.Designer
        check(d.editId == null)
    }

    @Test fun historyAndSheets() {
        val p = h.makeProject("hearts4", "Summer trip")
        h.run {
            h.app.store.recordHistory(p, 2048, 2048, true)
            h.app.store.recordHistory(p.copy(name = "Weekend"), 2048, 2048, false)
        }
        h.start()
        h.vm.openHistory(); h.shot("21-history")
        h.vm.openSheet(Sheet.HistoryItem(h.vm.store.history.value.first().id)); h.shot("22-history-item")
        h.vm.closeSheets(); h.vm.closeOverlay()
        h.vm.openSheet(Sheet.Pro); h.shot("23-pro")
        h.vm.closeSheets()
        h.vm.openSheet(Sheet.Card(p.id)); h.shot("24-card")
        h.vm.closeSheets()
        h.vm.openProject(p.id)
        h.vm.openSheet(Sheet.Export); h.shot("25-export")
    }

    @Test fun designerShapes() {
        h.start()
        h.vm.openDesigner(BrowseTarget.Home, baseId = "hearts4")
        h.settle()
        h.shot("30-designer-hearts")
        val d = h.vm.overlays.last() as Overlay.Designer
        check(d.baseId == "hearts4")
    }

    @Test fun shapeCellSelectedInEditor() {
        val p = h.makeProject("clover", "Clover")
        h.start()
        h.vm.openProject(p.id)
        h.vm.editor!!.selectCell(1)
        h.vm.editor!!.selectTab(EditorTab.Photo)
        h.shot("31-editor-clover-selected")
    }

    @Test fun darkMode() {
        val dark = UiHarness(rule, dark = true)
        val p = dark.makeProject("hero", "Night")
        dark.start()
        dark.shot("40-dark-home")
        dark.vm.openProject(p.id)
        dark.vm.editor!!.selectTab(EditorTab.Style)
        dark.shot("41-dark-editor")
        dark.vm.editor!!.addItem(CanvasItem.text("Dark"))
        dark.shot("42-dark-text")
        dark.vm.openBrowse(BrowseTarget.Editor)
        dark.shot("43-dark-browse")
        dark.vm.closeOverlay()
        dark.vm.openSheet(Sheet.Pro)
        dark.shot("44-dark-pro")
    }
}
