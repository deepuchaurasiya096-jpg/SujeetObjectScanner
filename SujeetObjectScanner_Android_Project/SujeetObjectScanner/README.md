# Sujeet Object Scanner (Android)

A standalone Android **Object Scanner** app. This project does not include a 3D scanner, QR scanner, or document scanner.

## Features

- Live rear/front camera preview using CameraX.
- On-device ML Kit image labels with confidence percentages (bundled model; no image upload by this app).
- Scan an existing image from the phone gallery.
- Pause/resume live scanning, switch camera, and toggle flash where supported.
- Keeps the five most recent recognized labels locally on the device.
- Minimum Android version: Android 6.0 (API 23).

## Important accuracy note

The ML Kit default image-labeling model supports 400+ broad labels. This is image classification, not a guaranteed identification system: it may say "vehicle", "car", "plant", "tool", etc., and it may be uncertain or wrong. The confidence percentage is a model confidence score, not a guarantee. It does not draw object bounding boxes or identify exact make/model/serial numbers.

## Build with Android Studio

1. Install Android Studio and its Android SDK (API 36 recommended), with JDK 17 or newer.
2. Extract this ZIP.
3. In Android Studio choose **File → Open**, then select the `SujeetObjectScanner` folder (the folder containing `settings.gradle`).
4. Allow Gradle to sync. The first sync needs internet to download Android build dependencies and the bundled ML Kit model library.
5. Connect your Android phone with USB debugging enabled, or use an Android emulator with a camera.
6. Press **Run**. On first launch, grant Camera permission.

## Build an APK

In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Android Studio will show the APK location after a successful build. You can then copy the APK to your phone and install it.

## Project details

- Package: `com.sujeet.objectscanner`
- Version: `1.0`
- CameraX: `1.6.2`
- ML Kit Image Labeling (bundled): `17.0.9`
- `INTERNET` permission is not requested. Camera frames are analysed on-device by the bundled model.

## Troubleshooting

- If the camera does not open, check Android Settings → Apps → Sujeet Object Scanner → Permissions → Camera.
- If the preview is black, close other camera apps and restart the app.
- For better recognition, use good lighting, hold the phone steady, and keep one main object clearly visible.
- Some labels need the subject to occupy more of the frame. Results vary by device and image quality.

This ZIP includes a small Gradle bootstrap wrapper pinned to Gradle 8.11.1. On first command-line build it downloads the official Gradle distribution into the user's Gradle cache. Android Studio may also use its normal Gradle sync workflow.
