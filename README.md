# RootPurge ⚡

**RootPurge** is an ultra-lightweight, blazing-fast root cache cleaner for Android. Built with modern Kotlin and Jetpack Compose, it leverages superuser privileges (`su`) to traverse, calculate, and purge app caches, JIT code caches, and system temp files across all applications in a single click.

Designed with a strict **no-bloat philosophy**: plain, distraction-free, dynamically themed to your device wallpaper, with an APK size under **10 MB** and runtime RAM consumption under **15 MB**.

---

## ✨ Features

- **⚡ 1-Click Turbo Purge**: Executes batch cleaning across all target directories in a single `su` subshell session, eliminating process spawning overhead and delivering sub-second execution speeds.
- **📂 Deep Cache Directory Traversal**:
  - **App Caches**: `/data/data/*/cache`, `/data/user/*/*/cache`, `/data/user_de/*/*/cache`
  - **Code Caches**: `/data/data/*/code_cache` (JIT / oat compiled dex caches)
  - **External Media Caches**: `/sdcard/Android/data/*/cache` (shared app media & thumbnails)
  - **System Temp**: `/cache/*`, `/data/local/tmp/*`, and package manager dead cache optimization (`pm trim-caches`)
  - **Kernel RAM Buffer**: Synchronizes and drops pagecache, dentries, and inodes (`sync; echo 3 > /proc/sys/vm/drop_caches`)
- **🛡️ Whitelist Protection**: Protect critical apps (e.g., offline maps, audio players) from having their cached data cleared during 1-click purges.
- **📱 App-by-App Cache Inspector**: Fast, searchable list of installed applications with size breakdown, system app identification, and per-app cache cleaning.
- **🎨 Wallpaper-Themed Monochrome UI**:
  - Uses Material You dynamic color theming (`dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+) to automatically adapt to your device wallpaper.
  - Neutral monochrome fallback for older Android versions.
  - Zero glowing animations, zero heavy bitmap allocations, and zero continuous render loops.
- **🖥️ Shell Output Console**: Built-in monospace terminal drawer displaying executed shell commands, execution durations, and exit statuses.
- **🔍 Root Diagnostics Center**: Diagnostic inspector that verifies `su` binary existence, detects root managers (**Magisk**, **KernelSU**, **APatch**, **SuperSU**), and tests UID 0 privileges.

---

## 🚀 Performance & Footprint

| Metric | Measurement |
| :--- | :--- |
| **APK File Size** | **~9.3 MB** (down from 25 MB after tree-shaking & eliminating icon bloat) |
| **Runtime RAM (PSS/Heap)** | **< 15 MB** (zero Bitmap caching for app icons) |
| **Purge Duration** | **< 150 ms** for full batch root execution |
| **Min Android SDK** | Android 7.0 (API Level 24) |
| **Target Android SDK** | Android 15 / 16 (API Level 36) |

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Root Engine**: Buffered `ProcessBuilder` single-session shell runner with timeout handling
- **Testing**: JUnit 4 & Robolectric JVM unit tests

---

## 📦 Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── model/
│   │   ├── AppCacheItem.kt             # Lightweight app cache model
│   │   ├── CleanTarget.kt              # Cache category targets
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
│   │   ├── TerminalLogDrawer.kt        # Monospace shell output viewer
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
- JDK 17 or higher
- Android SDK with Platform 36 installed
- Gradle 8.13+ (or use wrapper)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/your-username/root-purge.git
cd root-purge

# Run unit and Robolectric tests
gradle :app:testDebugUnitTest

# Build debug APK
gradle :app:assembleDebug

# The compiled APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk (~9.3 MB)
```

---

## 🔑 Permissions

- `android.permission.QUERY_ALL_PACKAGES`: Required to query installed application packages and calculate per-app cache sizes.
- `android.permission.VIBRATE`: Provides subtle haptic confirmation upon triggering the purge action.
- `Root (SuperUser)`: Requested at runtime via `su` to clear cache directories of third-party and system applications in `/data/data/*`.

---

## ⚠️ Disclaimer

RootPurge cleans temporary cache directories (`cache` and `code_cache`). It **never** touches user databases, accounts, or persistent app data. Root access grants low-level device control; use responsibly. Tested with Magisk, KernelSU, and APatch.

---

## 📄 License

This project is licensed under the MIT License.
