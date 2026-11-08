# Monochrome Reader — Android starter project

A lightweight Kotlin + Jetpack Compose starter for a monochrome Persian/English ebook reader.

## Open and build
1. Install Android Studio (current stable).
2. Open this folder in Android Studio.
3. Allow Gradle sync and install Android SDK 35 if prompted.
4. Run the `app` configuration on an Android device or emulator.

## Included in this starter
- Monochrome light/dark themes.
- English and Persian UI direction toggle (RTL/LTR).
- Local library with document picker and persisted URI permissions.
- EPUB text extraction for unencrypted EPUB files.
- Reader controls, font family and font size preferences.
- Basic highlight action that saves a short excerpt from the current reader page.
- A library-only app (no bookstore or subscription screens).
- An account information page with a link to the official Fidibo website; it does not claim to authenticate or synchronize your account.

## Important limitations
- Your existing purchased Fidibo books **cannot yet be shown in this app**. Official account authentication and access to a user's private library require an authorized API/SDK or partnership. We did not find a documented public integration for this. The app never collects Fidibo passwords or bypasses DRM.
- PDF files can be imported and listed, but PDF page rendering is not implemented in this starter.
- EPUB navigation is a simple text extraction prototype, not a full standards-compliant EPUB engine. It does not yet support chapter navigation, embedded fonts, images, CSS layout, annotations anchored to exact text ranges, or DRM-protected files.
- The orientation setting is stored as a preference; system auto-rotation currently controls the actual device orientation.
- This is source code, not a signed APK. Build a debug APK from Android Studio using Build > Build APK(s).

## Next development milestones
1. Integrate a full EPUB engine and Android PdfRenderer.
2. Implement robust bookmarks, page/position persistence, and text-range highlights.
3. Add secure Fidibo account sign-in and owned-book synchronization only when authorized APIs/SDKs are available.
4. Polish Persian translations and RTL navigation throughout.
5. Add tests, app icon, release signing, and APK optimization.
