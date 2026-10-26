# CloudStream Extensions Project — Full Context

> **Purpose**: This document contains the complete context of the `cdProject` CloudStream extension repository.
> It can be used by another AI assistant to understand, modify, or extend this project.

---

## 1. Project Overview

This is a **CloudStream 3** extension repository that provides Live TV plugins.
CloudStream is an open-source Android streaming app that loads extensions (`.cs3` files) from external repos.

- **GitHub Repo**: `https://github.com/Rodro-eng/cdProject`
- **Repo URL (for CloudStream app)**: `https://raw.githubusercontent.com/Rodro-eng/cdProject/builds/repo.json`
- **Local Path**: `d:\CloudeS\TestPlugins`

---

## 2. Architecture

```
cdProject/
├── .github/workflows/build.yml    # CI/CD — builds .cs3 files and pushes to `builds` branch
├── build.gradle.kts               # Root Gradle config (shared dependencies, plugin setup)
├── settings.gradle.kts            # Auto-discovers extension modules
├── repo.json                      # CloudStream repository manifest
├── gradle/                        # Gradle wrapper JAR
├── gradlew / gradlew.bat          # Gradle wrapper scripts
├── gradle.properties              # Gradle JVM settings
│
├── ExampleProvider/               # Template extension (placeholder, not functional)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/
│       ├── ExamplePlugin.kt
│       └── ExampleProvider.kt
│
├── LowSpeed/                      # Live TV extension (Bengali M3U playlist)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/
│       ├── LowSpeedPlugin.kt
│       └── LowSpeedProvider.kt
│
└── SkyStream/                     # Live TV extension (Indian M3U playlist, 500+ channels)
    ├── build.gradle.kts
    └── src/main/kotlin/com/example/
        ├── SkyStreamPlugin.kt
        └── SkyStreamProvider.kt
```

### How CloudStream Extensions Work

Each extension consists of two classes:
1. **Plugin class** (`*Plugin.kt`): Entry point annotated with `@CloudstreamPlugin`. Registers the provider.
2. **Provider class** (`*Provider.kt`): Extends `MainAPI()`. Implements content fetching logic.

The build system uses the `com.lagradost.cloudstream3.gradle` Gradle plugin which:
- Compiles each module into a `.cs3` file (a ZIP containing the compiled extension)
- Generates `plugins.json` (metadata for all extensions)

---

## 3. Key Files

### 3.1 Root `build.gradle.kts`

```kotlin
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    // compileSdk = 36, minSdk = 21, Java 17
    // Kotlin compiler flag: -Xskip-metadata-version-check (required because the
    // CloudStream pre-release JAR is compiled with Kotlin 2.3.0 but we use 2.1.0)

    dependencies {
        val cloudstream by configurations  // NOT "apk" — the plugin registers "cloudstream"
        cloudstream("com.lagradost:cloudstream3:pre-release")
        implementation(kotlin("stdlib"))
        implementation("com.github.Blatzar:NiceHttp:0.4.11")
        implementation("org.jsoup:jsoup:1.18.3")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    }

    cloudstream {
        // Uses GITHUB_REPOSITORY env var in CI, falls back to hardcoded path
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "Rodro-eng/cdProject")
    }
}
```

> [!IMPORTANT]
> - The dependency configuration name is `cloudstream`, **NOT** `apk`. The CloudStream Gradle plugin registers it as `cloudstream`.
> - `setRepo()` expects just the GitHub `user/repo` path (e.g., `"Rodro-eng/cdProject"`). The plugin internally prepends `https://github.com/`.
> - `-Xskip-metadata-version-check` is required to suppress Kotlin metadata version mismatch errors.

### 3.2 `repo.json` (Repository Manifest)

```json
{
    "name": "LowSpeed Repository",
    "description": "Live TV extensions for CloudStream",
    "manifestVersion": 1,
    "pluginLists": [
        "https://raw.githubusercontent.com/Rodro-eng/cdProject/builds/plugins.json"
    ]
}
```

This file tells CloudStream where to find `plugins.json`. Users add the raw URL to this file in CloudStream.

### 3.3 `.github/workflows/build.yml`

The CI/CD pipeline:
1. Checks out source (`master`) and builds branch (`builds`)
2. Sets up JDK 17 + Android SDK
3. Runs `./gradlew make makePluginsJson`
4. Copies `.cs3` files, `plugins.json`, and `repo.json` to the `builds` branch
5. Force-pushes to the `builds` branch

> [!WARNING]
> - The `builds` branch is **auto-managed** — do NOT manually edit it.
> - `actions/checkout@master` is used (not `@v4`) because `@v4` currently targets Node.js 20 which triggers deprecation warnings.
> - `android-actions/setup-android@v3` is used (v2 fails due to deprecated 'tools' package).

### 3.4 `settings.gradle.kts`

Auto-discovers all extension modules. Any directory with a `build.gradle.kts` is automatically included as a subproject.

---

## 4. Extension Details

### 4.1 LowSpeed Extension

| Property | Value |
|----------|-------|
| **M3U URL** | `https://go.skym3u.top/ik5m.m3u` |
| **Language** | Bengali (`bn`) |
| **Categories** | Sports, Entertainment, Movies, Music, Cartoon, Bangladesh, News, Documentary |
| **Files** | `LowSpeed/src/main/kotlin/com/example/LowSpeedPlugin.kt`, `LowSpeedProvider.kt` |

### 4.2 SkyStream Extension

| Property | Value |
|----------|-------|
| **M3U URL** | `https://go.skym3u.top/bcbd.m3u` |
| **Language** | Hindi (`hi`) |
| **Categories** | Entertainment, Sports, Movies, News, Kids, Music, Infotainment, Devotional, Lifestyle, Business, Educational |
| **Channels** | 500+ Indian TV channels |
| **Files** | `SkyStream/src/main/kotlin/com/example/SkyStreamPlugin.kt`, `SkyStreamProvider.kt` |

### 4.3 ExampleProvider (Template)

Placeholder extension from the CloudStream template. All methods throw `TODO()`. Not functional but compiles successfully.

### 4.4 skyView Extension

| Property | Value |
|----------|-------|
| **Base URL** | `http://30.30.30.30/` |
| **Language** | Bengali (`bn`) |
| **Categories** | Sports, Bangla, Hindi, English, Kids & Music (Others) |
| **Channels** | Live Sports, T Sports, GTV, Star Sports, Sony Sports, Zee Bangla, Star Jalsha, Independent TV, Somoy TV, Star Plus, Sony Max, Discovery, etc. |
| **Stream Server** | Flussonic HLS Server (`:8088/{streamId}/index.m3u8`) with token resolver |
| **Files** | `skyView/src/main/kotlin/com/example/SkyViewPlugin.kt`, `SkyViewProvider.kt` |


---

## 5. How to Add a New Extension

1. **Create a new directory** at the project root (e.g., `MyExtension/`)
2. **Create `build.gradle.kts`**:
   ```kotlin
   version = 1
   cloudstream {
       description = "Description of your extension"
       authors = listOf("YourName")
       status = 1
       tvTypes = listOf("Live")
       iconUrl = "https://example.com/icon.png"
       language = "en"
   }
   ```
3. **Create `src/main/AndroidManifest.xml`**:
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <manifest xmlns:android="http://schemas.android.com/apk/res/android">
   </manifest>
   ```
4. **Create Plugin class** (`src/main/kotlin/com/example/MyPlugin.kt`):
   ```kotlin
   package com.example
   import android.content.Context
   import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
   import com.lagradost.cloudstream3.plugins.Plugin

   @CloudstreamPlugin
   class MyPlugin : Plugin() {
       override fun load(context: Context) {
           registerMainAPI(MyProvider())
       }
   }
   ```
5. **Create Provider class** (`src/main/kotlin/com/example/MyProvider.kt`) — implement `MainAPI()`
6. **Push to master** — the CI will auto-build and publish

---

## 6. API Gotchas & Lessons Learned

> [!CAUTION]
> These are hard-won lessons from debugging build failures:

| Issue | Solution |
|-------|----------|
| `Configuration with name 'apk' not found` | Use `val cloudstream by configurations` not `val apk` |
| `Kotlin metadata version 2.3.0 vs 2.1.0` | Add `-Xskip-metadata-version-check` to `freeCompilerArgs` |
| `Unresolved reference 'AppCompatActivity'` | Don't import `androidx.appcompat.app.AppCompatActivity` — it's not available |
| `ExtractorLink constructor deprecated` | Use `com.lagradost.cloudstream3.utils.newExtractorLink()` with builder pattern |
| `'val' cannot be reassigned (isM3u8)` | Don't set `isM3u8` — it's auto-calculated from the URL |
| `repositoryUrl malformed in plugins.json` | Pass just `"user/repo"` to `setRepo()`, NOT `"raw.githubusercontent.com/..."` |
| `plugins.json 404` | Repo must be **public** for raw GitHub URLs to work |
| `CloudStream can't find plugins` | Must have `repo.json` manifest pointing to `plugins.json` |
| `gradlew --posix error` | Use a proper Gradle wrapper script, not a custom one |

---

## 7. Build & Deployment

### Local Build (requires JDK 17 + Android SDK)
```bash
./gradlew make makePluginsJson
```

### CI/CD (GitHub Actions)
- Triggers on push to `master`/`main` or manual `workflow_dispatch`
- Built `.cs3` files are force-pushed to the `builds` branch
- Users install via: `https://raw.githubusercontent.com/Rodro-eng/cdProject/builds/repo.json`

### Testing in CloudStream
1. Open CloudStream app → Settings → Extensions → Add Repository
2. Paste: `https://raw.githubusercontent.com/Rodro-eng/cdProject/builds/repo.json`
3. Install the desired extension (LowSpeed, SkyStream)
4. Go to Home to see the Live TV categories
