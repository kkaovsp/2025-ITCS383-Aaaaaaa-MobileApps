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