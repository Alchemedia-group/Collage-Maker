# Design preview

`photo-collage-demo.html` is a single-file, interactive preview of the planned Photo Collage Android app.
Open it in any browser. It needs no build step and no network except Google Fonts (it falls back to system fonts offline).

It is a design reference, not the app. The Android project in `app/` is at milestone 1 (project model, photo import,
storage, home and editor shells). Everything else in the preview is how the app is meant to look and behave:

- 110 layouts (grids, feature, mosaic, shapes, letters) plus a layout designer that saves to "My layouts"
- text with 60+ fonts, colours, outlines, shadows and curves; emoji and shape stickers
- 20 photo filters with strength, 16 textured backgrounds
- history of saved collages, delete with undo
- Pro: £2.99 a month, removes the watermark (demo only, no payment is taken)

Hero photos on the home screen are from Unsplash (free to use under the Unsplash License). All other sample photos are
generated in the browser.
