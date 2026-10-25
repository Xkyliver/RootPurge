# RootPurge ⚡

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![FOSS](https://img.shields.io/badge/FOSS-Free%20%26%20Open%20Source-brightgreen.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![CI/CD](https://img.shields.io/badge/GitHub%20Actions-Build%20%26%20Publish-2088FF?logo=github-actions&logoColor=white)](.github/workflows/build-and-publish.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Footprint](https://img.shields.io/badge/APK%20Size-9.3%20MB-success)](https://github.com)

**RootPurge** is an ultra-lightweight, blazing-fast root cache cleaner for Android. Built with modern Kotlin and Jetpack Compose, it leverages superuser privileges (`su`) to traverse, calculate, and purge app caches, JIT code caches, and system temp files across all applications in a single click.

Designed with a strict **no-bloat philosophy**: plain, distraction-free, dynamically themed to your device wallpaper, with an APK size under **10 MB** and runtime RAM consumption under **15 MB**. Licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

---

```
┌────────────────────────────────────────────────────────┐
│  RootPurge                              [● Root: Active]│
├────────────────────────────────────────────────────────┤
│  Reclaimable Cache                                     │
│  1.42 GB                                               │
│  App: 840 MB • Code: 210 MB • Media: 350 MB            │
│  Traversed 112 directories across /data/user/0 in 48ms │
│                                                        │
│  [================ CLEAN ALL CACHE ================]   │
├────────────────────────────────────────────────────────┤
│  Targets:  [✓ App Cache] [✓ Code Cache] [✓ Media Cache]│
│            [✓ System Temp] [✓ Drop RAM]                │
├────────────────────────────────────────────────────────┤
│  > Shell Console (Copy / Clear)                        │
│  [10:14:02.114] [SCAN] com.android.chrome: 26.8 MB     │
│  [10:14:02.158] [SCAN] com.spotify.music: 312.4 MB     │
│  [10:14:05.412] [EXEC] su -c "rm -rf /data/user/0/*..."│
│  [10:14:05.476] [CLEARED] /data/user/0/*/cache         │
│  [10:14:05.480] [SUCCESS] Completed with exit code 0   │
└────────────────────────────────────────────────────────┘
```

---

## ✨ Features

- **⚡ 1-Click Turbo Purge**: Executes batch cleaning across all target directories in a single `su` subshell session, eliminating process spawning overhead and delivering sub-second execution speeds (<150ms).
- **📂 Accurate Cache Traversal**:
  - Resolves modern multi-user storage paths (`/data/user/0/*/cache`, `/data/user/0/*/code_cache`) and legacy symlinks (`/data/data/*/cache`).
  - Robust parent-directory package extraction (`extractPackageFromPath`) ensures byte-accurate sizes matching Android Settings.
  - Zero synthetic or hardcoded estimations — reports real, physical on-disk measurements.
- **🛡️ Whitelist Protection**: Protect sensitive apps (e.g., offline maps, music players, authenticators) from having their cache cleared during 1-click purges.
- **📱 App-by-App Cache Inspector**: Fast, searchable list of installed applications with size breakdown, system app identification, and per-app cache cleaning.
- **🎨 Wallpaper-Themed Monochrome UI**:
  - Uses Material You dynamic color theming (`dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+) to automatically adapt to your device wallpaper.
  - Neutral monochrome fallback for older Android versions.
  - Zero glowing animations, zero heavy bitmap allocations, and zero continuous render loops.
- **🖥️ Shell Output Console**: Built-in monospace terminal drawer displaying executed shell commands, execution durations, exit statuses, and a 1-tap **"Copy Logs"** button.
- **🔍 Root Diagnostics Center**: Diagnostic inspector that verifies `su` binary existence, detects root managers (**Magisk**, **KernelSU**, **APatch**, **SuperSU**), and tests UID 0 privileges.

---

## 🎯 Clean Targets Breakdown

RootPurge cleans temporary cache directories (`cache` and `code_cache`). It **never** touches user databases, accounts, or persistent app data:

| Target | Paths Cleared | Description |
| :--- | :--- | :--- |
| **App Caches** | `/data/user/0/*/cache/*`<br>`/data/data/*/cache/*`<br>`/data/user/*/*/cache/*` | HTTP cache, glide/coil images, webview storage, and downloaded media thumbnails. |
| **Code Caches** | `/data/user/0/*/code_cache/*`<br>`/data/data/*/code_cache/*` | JIT compilation caches, extracted DEX files, and oat runtime artifacts. |
| **External Media** | `/sdcard/Android/data/*/cache/*`<br>`/storage/emulated/0/Android/data/*/cache/*` | Shared external app caches, camera thumbnails, and video player cache blocks. |
| **System Temp** | `/cache/*`<br>`/data/local/tmp/*`<br>`pm trim-caches 999999999999` | System recovery partition temp files, adb pushes, and Dead Package Manager caches. |
| **Drop RAM** | `sync`<br>`echo 3 > /proc/sys/vm/drop_caches` | Drops clean pagecaches, dentries, and inodes from kernel memory. |

---

## 📱 Root Manager Compatibility

RootPurge supports all major Android root solutions:

| Provider | Detection Method | Status | Notes |
| :--- | :--- | :--- | :--- |
| **KernelSU** | `su -v` -> KernelSU / KSU | ✅ Fully Supported | Kernel-level root; grant SuperUser in KSU app. |
| **Magisk** | `su -v` -> MagiskSU | ✅ Fully Supported | Systemless root (v20 through v28+ supported). |
| **APatch** | `su -v` -> APatch | ✅ Fully Supported | KernelPatch-based root solution. |
| **SuperSU / Lineage** | `/system/xbin/su` / `/system/bin/su` | ✅ Fully Supported | Legacy su binaries supported. |
| **Non-Root / Sandbox** | Fallback mode | ⚠️ Limited | Scans internal cache & external shared storage. |

---

## 🚀 Performance & Footprint

| Metric | Measurement | Notes |
| :--- | :--- | :--- |
| **APK File Size** | **~9.3 MB** | Optimized by stripping `material-icons-extended` (~57,000 unused classes). |
| **Runtime RAM (PSS/Heap)** | **< 15 MB** | Zero Bitmap/Drawable caching for app icons; minimal heap overhead. |
| **Purge Duration** | **< 150 ms** | Executed as a composite single-process shell batch. |
| **Min Android SDK** | Android 7.0 (API Level 24) | Works on Nougat, Oreo, Pie, Q, R, S, T, U, V, and Baklava. |
| **Target Android SDK** | Android 15 / 16 (API Level 36) | Modern edge-to-edge support with `WindowInsets.safeDrawing`. |

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Theming**: Material You Dynamic Wallpaper Theming (`dynamicDarkColorScheme` / `dynamicLightColorScheme`)
- **Architecture**: Clean MVVM (Model-View-ViewModel) + Repository Pattern
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Root Engine**: Single-pass `ProcessBuilder` shell runner with timeout handling
- **Testing**: Robolectric (SDK 34) & JUnit 4 JVM unit tests

---

## 📦 Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── model/
│   │   ├── AppCacheItem.kt             # App cache item with byte formatting
│   │   ├── CleanTarget.kt              # Enum of 5 cleanable cache zones
│   │   ├── CleaningResult.kt           # Purge metrics & execution logs
│   │   ├── ReclaimableStorageReport.kt # Traversal stats & directory entries
│   │   └── RootStatus.kt               # SuperUser connection states
│   ├── repository/
│   │   └── CacheCleanerRepository.kt   # Directory traversal & telemetry repository
│   └── root/
│       └── RootShellExecutor.kt        # High-performance root shell execution engine
├── ui/
│   ├── components/
│   │   ├── AppCacheItemCard.kt         # Lightweight app row with whitelist toggle
│   │   ├── CleanCompleteDialog.kt      # Native summary dialog
│   │   ├── CleanTargetsRow.kt          # Native FilterChip targets selector
│   │   ├── RootDiagnosticBottomSheet.kt# Root subsystem diagnostics
│   │   ├── RootStatusChip.kt           # Header root indicator
│   │   ├── TerminalLogDrawer.kt        # Monospace shell output viewer with Copy/Clear
│   │   └── TurboGaugeCard.kt           # Main reclaimable cache card & 1-click button
│   ├── screens/
│   │   └── CleanerHomeScreen.kt        # Primary scrollable dashboard screen
│   ├── theme/
│   │   ├── Color.kt                    # Neutral monochrome tokens
│   │   ├── Theme.kt                    # Dynamic wallpaper + monochrome theme
│   │   └── Type.kt                     # Clean typography styles
│   └── viewmodel/
│       └── CleanerViewModel.kt         # Reactive UI state management
└── MainActivity.kt                     # ComponentActivity with edge-to-edge
```

---

## 🔨 Building from Source

### Prerequisites
- JDK 21 (Temurin / OpenJDK)
- Android SDK Platform 36 & Build-Tools
- Gradle 8.13+ (or use bundled `./gradlew`)

### Build Commands

```bash
# 1. Clone the repository
git clone https://github.com/your-username/root-purge.git
cd root-purge

# 2. Make wrapper executable
chmod +x gradlew

# 3. Run unit and Robolectric tests
./gradlew testDebugUnitTest

# 4. Build standalone debug APK
./gradlew assembleDebug

# Output APK: app/build/outputs/apk/debug/app-debug.apk (~9.3 MB)
```

---

## 🤖 Continuous Integration & Publishing

A production-ready GitHub Actions workflow is provided at [`.github/workflows/build-and-publish.yml`](.github/workflows/build-and-publish.yml):

- **Automated Verification**: Automatically runs unit & Robolectric tests on every push to `main`.
- **Manual Trigger (`workflow_dispatch`)**: Go to **Actions** -> **Build & Publish Release** -> **Run workflow** to compile and create a new GitHub Release with the standalone APK attached.
- **Tag-based Release**: Pushing any tag starting with `v*` (e.g., `git tag v1.0.0 && git push --tags`) automatically compiles the app, generates release notes, and publishes the standalone APK release.
- **Zero Configuration Runner**: Automatically restores debug keystore and `.env` files on GitHub virtual environments.

---

## ❓ Frequently Asked Questions (FAQ)

#### Q: Does RootPurge delete my login sessions or photos?
**No.** RootPurge specifically targets cache directories (`cache` and `code_cache`). It never touches `/databases`, `/shared_prefs`, `/files`, or personal pictures/documents in external storage.

#### Q: Why does Chrome show 26.8 MB instead of 250 MB?
RootPurge reads the actual physical on-disk directory sizes (`/data/user/0/com.android.chrome/cache`) rather than using artificial estimates, accurately reflecting the real cache usage shown in Android Settings.

#### Q: What if my device is unrooted?
The app runs in internal sandbox mode: it will display storage metrics and can clean its own cache and accessible public external cache folders. For third-party private caches (`/data/user/0/*`), root access is required by Android sandbox security rules.

---

## 🔑 Permissions

- `android.permission.QUERY_ALL_PACKAGES`: Required to query installed application packages and calculate per-app cache sizes.
- `android.permission.VIBRATE`: Provides subtle haptic confirmation upon triggering the purge action.
- `Root (SuperUser)`: Requested at runtime via `su` to clear cache directories of third-party and system applications in `/data/user/0/*`.

---

## ⚠️ Disclaimer

RootPurge cleans temporary cache directories (`cache` and `code_cache`). It **never** touches user databases, accounts, or persistent app data. Root access grants low-level device control; use responsibly. Tested with Magisk, KernelSU, and APatch.

---

## 📄 License

This project is Free and Open Source Software (FOSS) licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for the full license text.
