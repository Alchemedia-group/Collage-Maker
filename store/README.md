# Play Store assets

| File | What it is |
| --- | --- |
| `../design/app-icon-512.png` | App icon, 512 x 512, full square (Play rounds the corners) |
| `feature-graphic.png` | Feature graphic, 1024 x 500 |
| `screenshot-1.png` to `screenshot-4.png` | Phone screenshots, 1080 x 1920 |
| `listing/<locale>.txt` | Title, short and full description for en-GB, es-ES, de-DE, fr-FR, it-IT, pt-BR, hi-IN and id |
| `listing/build_listings.py` | Rewrites the listing files and checks Play's length limits (title 30, short 80, full 4000) |
| `privacy-policy.md` | Privacy policy to host publicly. Replace `{{SUPPORT_EMAIL}}` first |
| `play-console-answers.md` | Answers for the Data safety, content rating, target audience and subscription forms |

The screenshots are renders from the app's screenshot tests (`./gradlew :app:testDebugUnitTest --tests "*UiShotTest"`
writes `app/build/shots/ui/`). Swap in real-device captures if you prefer.
