package com.alchemedia.collage.ui.components

/** Line icons from the design (24 x 24 grid, 2 px strokes). Generated from the demo's SVGs. */
enum class Icon(val parts: List<IconPart>) {
    History(listOf(IconPart("M3 12A9 9 0 1 0 21 12A9 9 0 1 0 3 12Z", false, 1f), IconPart("M12 7v5l3 2", false, 1f))),
    Add(listOf(IconPart("M12 5v14M5 12h14", false, 1f))),
    Back(listOf(IconPart("M19 12H5M11 6l-6 6 6 6", false, 1f))),
    Photo(listOf(IconPart("M6 4H18A3 3 0 0 1 21 7V17A3 3 0 0 1 18 20H6A3 3 0 0 1 3 17V7A3 3 0 0 1 6 4Z", false, 1f), IconPart("M7.4 10A1.6 1.6 0 1 0 10.6 10A1.6 1.6 0 1 0 7.4 10Z", false, 1f), IconPart("M4 18l5-5 4 4 3-3 4 4", false, 1f))),
    Save(listOf(IconPart("M12 4v11M7 10l5 5 5-5M5 20h14", false, 1f))),
    Reset(listOf(IconPart("M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5", false, 1f))),
    Swap(listOf(IconPart("M7 4l-4 4 4 4M3 8h14M17 20l4-4-4-4M21 16H7", false, 1f))),
    Trash(listOf(IconPart("M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3", false, 1f))),
    AlignLeft(listOf(IconPart("M4 6h16M4 12h10M4 18h14", false, 1f))),
    AlignCenter(listOf(IconPart("M4 6h16M7 12h10M5 18h14", false, 1f))),
    AlignRight(listOf(IconPart("M4 6h16M10 12h10M6 18h14", false, 1f))),
    Duplicate(listOf(IconPart("M10 8H18A2 2 0 0 1 20 10V18A2 2 0 0 1 18 20H10A2 2 0 0 1 8 18V10A2 2 0 0 1 10 8Z", false, 1f), IconPart("M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2", false, 1f))),
    ToFront(listOf(IconPart("M4 15V6a2 2 0 0 1 2-2h9", false, 1f), IconPart("M10 8H18A2 2 0 0 1 20 10V18A2 2 0 0 1 18 20H10A2 2 0 0 1 8 18V10A2 2 0 0 1 10 8Z", true, 1f))),
    ToBack(listOf(IconPart("M6 4H14A2 2 0 0 1 16 6V14A2 2 0 0 1 14 16H6A2 2 0 0 1 4 14V6A2 2 0 0 1 6 4Z", true, .4f), IconPart("M10 8H18A2 2 0 0 1 20 10V18A2 2 0 0 1 18 20H10A2 2 0 0 1 8 18V10A2 2 0 0 1 10 8Z", false, 1f))),
    Flip(listOf(IconPart("M12 3v18M8 7L3 12l5 5V7zM16 7l5 5-5 5V7z", false, 1f))),
    FlipV(listOf(IconPart("M3 12h2M9 12h2M13 12h2M19 12h2M6 9l6-5 6 5ZM6 15l6 5 6-5Z", false, 1f))),
    Rotate(listOf(IconPart("M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5", false, 1f))),
    Star(listOf(IconPart("M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 16.9 6.8 19.7l1-5.9L3.5 9.7l5.9-.8Z", false, 1f))),
    StarFilled(listOf(IconPart("M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 16.9 6.8 19.7l1-5.9L3.5 9.7l5.9-.8Z", true, 1f), IconPart("M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 16.9 6.8 19.7l1-5.9L3.5 9.7l5.9-.8Z", false, 1f))),
    LayoutTool(listOf(IconPart("M5 3H9A2 2 0 0 1 11 5V12A2 2 0 0 1 9 14H5A2 2 0 0 1 3 12V5A2 2 0 0 1 5 3Z", false, 1f), IconPart("M15 3H19A2 2 0 0 1 21 5V7A2 2 0 0 1 19 9H15A2 2 0 0 1 13 7V5A2 2 0 0 1 15 3Z", false, 1f), IconPart("M15 11H19A2 2 0 0 1 21 13V19A2 2 0 0 1 19 21H15A2 2 0 0 1 13 19V13A2 2 0 0 1 15 11Z", false, 1f), IconPart("M5 16H9A2 2 0 0 1 11 18V19A2 2 0 0 1 9 21H5A2 2 0 0 1 3 19V18A2 2 0 0 1 5 16Z", false, 1f))),
    ShapeTool(listOf(IconPart("M6 2v14a2 2 0 0 0 2 2h14M2 6h14a2 2 0 0 1 2 2v14", false, 1f))),
    StyleTool(listOf(IconPart("M4 7h10M18 7h2M4 17h2M10 17h10", false, 1f), IconPart("M14 7A2 2 0 1 0 18 7A2 2 0 1 0 14 7Z", false, 1f), IconPart("M6 17A2 2 0 1 0 10 17A2 2 0 1 0 6 17Z", false, 1f))),
    TextTool(listOf(IconPart("M5 7V5h14v2M12 5v14M9 19h6", false, 1f))),
    StickerTool(listOf(IconPart("M3 12A9 9 0 1 0 21 12A9 9 0 1 0 3 12Z", false, 1f), IconPart("M8.5 14a4 4 0 0 0 7 0M9 9.5h.01M15 9.5h.01", false, 1f))),
    Close(listOf(IconPart("M6 6l12 12M18 6L6 18", false, 1f))),
    Search(listOf(IconPart("M4 11A7 7 0 1 0 18 11A7 7 0 1 0 4 11Z", false, 1f), IconPart("M20 20l-4-4", false, 1f))),
    Check(listOf(IconPart("M5 12l5 5 9-10", false, 1f))),
    SplitV(listOf(IconPart("M6 4H18A2 2 0 0 1 20 6V18A2 2 0 0 1 18 20H6A2 2 0 0 1 4 18V6A2 2 0 0 1 6 4Z", false, 1f), IconPart("M12 4v16", false, 1f))),
    SplitH(listOf(IconPart("M6 4H18A2 2 0 0 1 20 6V18A2 2 0 0 1 18 20H6A2 2 0 0 1 4 18V6A2 2 0 0 1 6 4Z", false, 1f), IconPart("M4 12h16", false, 1f))),
    Undo(listOf(IconPart("M9 14L4 9l5-5", false, 1f), IconPart("M4 9h10a6 6 0 0 1 0 12h-3", false, 1f))),
    Redo(listOf(IconPart("M15 14l5-5-5-5", false, 1f), IconPart("M20 9H10a6 6 0 0 0 0 12h3", false, 1f))),
    Stretch(listOf(IconPart("M4 12h16M4 12l3-3M4 12l3 3M20 12l-3-3M20 12l-3 3", false, 1f))),
    ChevronLeft(listOf(IconPart("M15 6l-6 6 6 6", false, 1f))),
    ChevronRight(listOf(IconPart("M9 6l6 6-6 6", false, 1f))),
    AllLayouts(listOf(IconPart("M5.6 4H8.9A1.6 1.6 0 0 1 10.5 5.6V8.9A1.6 1.6 0 0 1 8.9 10.5H5.6A1.6 1.6 0 0 1 4 8.9V5.6A1.6 1.6 0 0 1 5.6 4Z", false, 1f), IconPart("M15.1 4H18.4A1.6 1.6 0 0 1 20 5.6V8.9A1.6 1.6 0 0 1 18.4 10.5H15.1A1.6 1.6 0 0 1 13.5 8.9V5.6A1.6 1.6 0 0 1 15.1 4Z", false, 1f), IconPart("M5.6 13.5H8.9A1.6 1.6 0 0 1 10.5 15.1V18.4A1.6 1.6 0 0 1 8.9 20H5.6A1.6 1.6 0 0 1 4 18.4V15.1A1.6 1.6 0 0 1 5.6 13.5Z", false, 1f), IconPart("M15.1 13.5H18.4A1.6 1.6 0 0 1 20 15.1V18.4A1.6 1.6 0 0 1 18.4 20H15.1A1.6 1.6 0 0 1 13.5 18.4V15.1A1.6 1.6 0 0 1 15.1 13.5Z", false, 1f))),
    Pencil(listOf(IconPart("M4 20h4L19 9l-4-4L4 16v4zM13 7l4 4", false, 1f))),
    More(listOf(IconPart("M3.8 12A1.2 1.2 0 1 0 6.2 12A1.2 1.2 0 1 0 3.8 12Z", true, 1f), IconPart("M10.8 12A1.2 1.2 0 1 0 13.2 12A1.2 1.2 0 1 0 10.8 12Z", true, 1f), IconPart("M17.8 12A1.2 1.2 0 1 0 20.2 12A1.2 1.2 0 1 0 17.8 12Z", true, 1f)));
}

class IconPart(val path: String, val filled: Boolean = false, val alpha: Float = 1f)
