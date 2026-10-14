# Design preview

`photo-collage-demo.html` is a single-file, interactive preview of the planned Photo Collage Android app.
Open it in any browser. It needs no build step and no network except Google Fonts (it falls back to system fonts offline).

It is the design reference for the app. The Android project in `app/` now implements it natively (Jetpack Compose):
the home screen, New collage, the layout browser, the editor, text and stickers, filters and textures, the layout
designer, History and the Pro sheet. The preview shows how it is meant to look and behave:

- 110 layouts (grids, feature, mosaic, shapes, letters) plus a layout designer that saves to "My layouts"
- text with 60+ fonts, colours, outlines, shadows and curves; emoji and shape stickers
- 20 photo filters with strength, 16 textured backgrounds
- history of saved collages, delete with undo
- Pro: £2.99 a month, removes the watermark (the preview takes no payment; the app uses Google Play Billing with the
  subscription id `photo_collage_pro_monthly_v2`, which has to be created in Play Console)

Hero photos on the home screen are from Unsplash (free to use under the Unsplash License). All other sample photos are
generated in the browser.
