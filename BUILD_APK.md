# How to export an Android APK

This project is a Kotlin / Jetpack Compose Android starter. **No APK is bundled in the ZIP.**

## Option A — Generate an APK using GitHub Actions (no Android Studio required)

1. Create a new GitHub repository and upload **all files inside this `MonochromeReader` folder**, including the hidden `.github/workflows/build-apk.yml` file.
2. In your repository's **Actions** tab, select **Build installable debug APK**.
3. Choose **Run workflow** (or push the files to `main` / `master`).
4. When the workflow succeeds, open the run and download the **MonochromeReader-debug-apk** artifact.
5. Extract the downloaded artifact ZIP. The file inside is **`app-debug.apk`**, a debug-signed installable APK.

The workflow uses Java 17, Gradle 8.9, Android SDK 35 and the versions in the original Gradle build files. No signing credentials are needed for a **debug** build.

## Option B — Android Studio

1. Install Android Studio and Android SDK Platform 35.
2. Open the `MonochromeReader` folder (the one containing `settings.gradle.kts`).
3. Let Android Studio download/configure Gradle 8.9 and the required dependencies. The uploaded project has no Gradle wrapper, so if prompted select Gradle 8.9 as the local Gradle version, or generate a wrapper using an installed Gradle 8.9 (`gradle wrapper --gradle-version 8.9`).
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The generated file is at `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

- **Debug APK only:** installable on a device for testing, not suitable as a Play Store release. A release build needs your private signing key.
- This starter has placeholders for Fidibo login/Fidiplus and no PDF page renderer. A successful APK build does not add those features.
- This bundle was prepared in an environment without the Android SDK, Gradle, or network access to their download repositories; the Gradle build was **not** executed or verified here.
