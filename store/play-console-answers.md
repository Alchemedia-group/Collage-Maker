# Play Console: answers to have ready

Everything below reflects what the app does today (version 0.4.x). Check each answer again if features change.

## App access
No login, nothing restricted. All features can be used without any special access. (Pro can be tried by a licence tester.)

## Ads
The app does not contain ads.

## Content rating (IARC questionnaire)
Category: **Utility, productivity, communication or other** (photo editing tool).
- Violence, sexual content, language, controlled substances, gambling: **No** to all.
- User-generated content shared with other users: **No** (collages are saved on the device or shared out through other apps; nothing is hosted).
- Shares the user's location: **No**. Digital purchases: **Yes** (subscription).
Expected result: Everyone / PEGI 3.

## Target audience and content
- Target age groups: **18 and over** is the safest choice; **13+** is also accurate. Do not select under 13, because that triggers the Families policy.
- Appeals to children: **No**.

## Data safety
Declare: **No data collected, no data shared.**
- The app has no account, no analytics, no ads and no crash reporting.
- Photos are processed and kept on the device only.
- Payments go through Google Play, which Play does not count as "collected" by the app.
- Data encrypted in transit: not applicable (nothing is sent).
- Users can request data deletion: not applicable (nothing is held); say so in the form if asked.
Note: the Play Billing Library adds the INTERNET and ACCESS_NETWORK_STATE permissions. That is expected and does not need a declaration beyond the above.

## Permissions declarations
- No photo or video permission is requested (the Android photo picker is used), so no Photo and video permissions form is needed.
- WRITE_EXTERNAL_STORAGE is declared with maxSdkVersion 28 (Android 9 and older) only.

## Privacy policy
Paste the public URL of `store/privacy-policy.md` once it is hosted (Play Console, App content, Privacy policy). The same URL should go in `AppInfo.PRIVACY_URL` in the app. Replace `{{SUPPORT_EMAIL}}` in the policy first.

## Store listing
- App name: **Photo Collage | Maker & Editor** (30 characters). Other languages use the titles in `store/listing/`.
- Default language: English (United Kingdom). Add translations for Spanish, German, French, Italian, Portuguese (Brazil), Hindi and Indonesian from `store/listing/`.
- Category: **Photography** (alternative: Art & Design). Tags: Photo editor, Collage.
- App icon: `design/app-icon-512.png` (512 x 512).
- Feature graphic: `store/feature-graphic.png` (1024 x 500).
- Phone screenshots: `store/screenshot-1.png` to `screenshot-4.png` (1080 x 1920). Replace with real-device captures if you prefer; Play wants at least 2, and 4 or more helps.
- Tablet screenshots (optional, recommended): 7-inch and 10-inch screenshots; the app letterboxes to phone proportions on tablets.
- Contact details: support email and, if you have one, a website. The email is shown publicly on the store page.

## Subscription product
- Product ID: `photo_collage_pro_monthly` (must match exactly).
- Base plan: monthly, auto-renewing, GBP 2.99. Add other countries' prices from Play's suggested conversions.
- Benefits text: "Removes the Photo Collage mark from saved collages."
- Add free trial or introductory offers later without touching the app: the app uses the plain base plan.

## Testing tracks
1. Internal testing: upload the signed `.aab`, add your Google account under Licence testing (Settings, then Licence testing), and test a purchase with the test card.
2. Closed testing: if your developer account was created as a personal account after November 2023, you need at least 12 testers opted in for 14 days before you can apply for production access.
3. Production.

## Release checklist
- versionCode must go up with every upload (currently set in `app/build.gradle.kts`).
- Keep `release-keys/photo-collage-upload.jks` and `keystore.properties` safe and backed up outside the repo. Use Play App Signing (the default); the upload key can be reset by Google if it is ever lost.
