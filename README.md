# Velo Downloader

Velo Downloader is a high-speed video and music downloader for Android built with Kotlin and Jetpack Compose.

## 📱 How to Download APK from GitHub (GitHub Actions)

This repository includes a ready-to-use **GitHub Actions Workflow** that automatically builds the APK for you!

1. **Push or Fork this repository to GitHub.**
2. Go to your GitHub repository in your browser.
3. Click on the **Actions** tab at the top.
4. Select the **Build Android APK** workflow (runs automatically on push, or click **Run workflow**).
5. Once the build completes with a green checkmark (~2 minutes), click on the completed run.
6. Scroll down to the **Artifacts** section at the bottom.
7. Click on **app-debug-apk** to download the ZIP file containing `app-debug.apk`.
8. Unzip and install `app-debug.apk` directly on your Android phone!

---

## 💻 How to Build APK Locally

If you are cloning this repository to your computer:

### Prerequisites
- JDK 17 or higher
- Android SDK (API 34/36)

### Build Command
On Linux / macOS:
```bash
./gradlew assembleDebug
```

On Windows:
```cmd
gradlew.bat assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
