# 2025-ITCS383-Aaaaaaa-MobileApps

This project is a Mobile Application (Android) developed using **Kotlin** and **Jetpack Compose**.

## 🛠 Prerequisites

To open, edit, and run this project, you need the following tools installed:

1. **Android Studio**: The primary IDE used for Android app development. You can download it for free at [developer.android.com/studio](https://developer.android.com/studio).
2. **Java Development Kit (JDK 17)**: Typically bundled and installed automatically with Android Studio.
3. **Android SDK**: Upon opening Android Studio for the first time, it will prompt you to download the necessary SDKs and tools (Build Tools, Platform Tools). Follow the initial setup instructions.

## 🚀 How to Open and Run the Project

### Method 1: Running via Android Studio (Recommended and Easiest)

1. Open **Android Studio**.
2. Select **Open** (or File > Open).
3. Navigate to the project directory and **select the `mobile` folder** (ensure it is the `mobile` folder that contains the `build.gradle.kts` file), then click **Open**.
4. Wait for Android Studio to load the project and finish the **Gradle Sync** (this might take a few minutes to download the initial dependencies).
5. **Set up a device to run the app**:
   - **Using an Emulator**: Go to the **Device Manager** (phone icon on the top right) -> Click **Create Device**. Select your preferred hardware profile (e.g., Pixel), download a System Image (API 34 or 35 recommended), and finish creating the device.
   - **Using a Physical Android Device**: Connect your phone via USB to your computer. On your phone, navigate to Settings, enable **Developer Options**, and turn on **USB Debugging**.
6. Once the device is ready, click the **Run** button (the green ▶ icon on the top toolbar). Android Studio will automatically build and install the app on your device.

### Method 2: Running via Command Line (Terminal)

If you prefer not to use Android Studio (e.g., for CI/CD environments) and already have an emulator running or a physical device connected, you can build and run the app via the terminal:

1. Open your Terminal and `cd` into the `mobile` directory:
   ```bash
   cd "path/to/2025-ITCS383-Aaaaaaa-MobileApps/mobile"
   ```
2. *(Crucial)* Ensure that a `local.properties` file exists in the `mobile` folder, specifying the path to your Android SDK. For example (on macOS):
   ```properties
   sdk.dir=/Users/your_username/Library/Android/sdk
   ```
   *(Note: If you have already opened the project in Android Studio, this file is generated automatically.)*
3. Build and install the app onto the connected device by running:
   ```bash
   # For macOS/Linux
   ./gradlew installDebug

   # For Windows
   gradlew.bat installDebug
   ```
4. The application (`Booth Organizer`) will be installed on your device, and you can launch it directly from the app drawer or home screen.

## 📊 Code Quality Setup (SonarCloud)

This project is already configured for code scanning via **SonarCloud** (see the `mobile/sonar-project.properties` file). The configuration is set to scan only the Kotlin logic codebase. UI-related files (such as Activities and Jetpack Compose screens) are explicitly **excluded** from the Code Coverage metrics calculations.

# BoothOrganizer Mobile

Native Android/Kotlin mobile client for the Booth Organizer system.

## Features

- Login with username/password plus quick demo login for Booth Manager, Merchant, and General User.
- Registration for General User and Merchant, including citizen ID and seller details for merchants.
- Event browsing, manager event create/edit/delete.
- Booth list, manager booth create/edit/delete, merchant booth reservation.
- Reservations, payment submission by Credit Card, TrueMoney, or Bank Transfer, and bank slip upload.
- Booth Manager merchant approval, payment approval, and event reports with CSV sharing.
- Profile editing, merchant seller info editing, notifications, and TH/EN language toggle.
- Web color theme matched to the React app: primary `#4f46e5`, secondary `#06b6d4`, light background `#f8fafc`.

## Backend

The app uses the same deployed Supabase Edge Function API as the web app:

```text
https://uaoufhdysqcivheauwyf.supabase.co/functions/v1/api
```

## Demo Accounts

| Role | Username | Password |
|---|---|---|
| Booth Manager | `boothManager` | `boothManager123` |
| Merchant | `demoMerchant` | `merchant123` |
| General User | `demoUser` | `user123` |

## SDK Setup

Create or update `implementations/mobile/local.properties` with your Android SDK path:

```powershell
# Example (Windows)
@"
sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk
"@ | Out-File -FilePath "implementations/mobile/local.properties" -Encoding UTF8
```

The `local.properties` file is local-only and is ignored by git. Do not commit it.

## Build APK

```powershell
cd implementations/mobile
./gradlew.bat --no-daemon assembleDebug
```

The APK outputs to `app/build/outputs/apk/debug/app-debug.apk`.

## Create and Launch Emulator

If no AVD exists yet, create one via command line:

```powershell
# Install a system image (example: API 35)
sdkmanager "system-images;android-35;google_apis;x86_64"

# Create an AVD
echo y | avdmanager create avd -n "BoothOrganizer_API35" -k "system-images;android-35;google_apis;x86_64"

# Start the emulator (runs in background)
emulator -avd BoothOrganizer_API35 -no-boot-anim -no-window &

# Wait for device to be ready
adb wait-for-device

# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch MainActivity
adb shell am start -n "com.kkaovsp.boothorganizer/.MainActivity"
```

To list existing AVDs:

```powershell
avdmanager list avd
```

To stop a running emulator:

```powershell
adb emu kill
```

## Runtime Test Checklist

Manual verification on emulator or device:

- [ ] Home screen loads
- [ ] Login with `boothManager` / `boothManager123`
- [ ] Browse events
- [ ] View booths
- [ ] Make or view reservations
- [ ] Check profile page
- [ ] Generate a report (Reports section)
- [ ] Confirm manager-only navigation items are visible
- [ ] Toggle language (TH/EN)
- [ ] Check for crash: scan logcat for `FATAL EXCEPTION` — none should appear

No screenshots are required for this project delivery.

## Android Test Coverage

Unit and UI tests are not required for the current phase. Current evidence is the APK build passing and emulator runtime verification. Adding unit tests or UI tests is an optional future improvement if the instructor requests it.
