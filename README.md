# Cloudstream3 Plugin Repo

Template for a [Cloudstream3](https://github.com/recloudstream) plugin repository.

> ⚠️ Make sure you check **"Include all branches"** when using this template

## Getting started with writing your first plugin

This repo includes 1 example plugin.

1. Open the root `build.gradle.kts`, read the comments and **replace all the placeholders** (especially the `setRepo(...)` URL with your GitHub username)
2. Familiarize yourself with the project structure. Most files are commented
3. Build or deploy your first plugin using:

**Windows:**
```bash
.\gradlew.bat ExampleProvider:make
# or deploy directly to a connected device:
.\gradlew.bat ExampleProvider:deployWithAdb
```

**Linux & Mac:**
```bash
./gradlew ExampleProvider:make
# or deploy directly to a connected device:
./gradlew ExampleProvider:deployWithAdb
```

## Adding a new plugin

1. Create a new directory at the root (e.g., `MyProvider/`)
2. Add a `build.gradle.kts` inside it (copy from `ExampleProvider/build.gradle.kts`)
3. Create the source directory: `MyProvider/src/main/kotlin/com/example/`
4. Add your `MyPlugin.kt` (entry point) and `MyProvider.kt` (content provider)
5. Add `MyProvider/src/main/AndroidManifest.xml` (minimal manifest)
6. Build with: `.\gradlew.bat MyProvider:make`

The `settings.gradle.kts` will **auto-detect** any new directory with a `build.gradle.kts`.

## GitHub Actions Setup

To enable automated builds when you push to GitHub:

1. Create a `builds` branch:
   ```bash
   git checkout --orphan builds
   git rm -rf .
   git commit --allow-empty -m "builds"
   git push origin builds
   git checkout master
   ```
2. Go to your repo's **Settings → Actions → General**
3. Under "Workflow permissions", select **"Read and write permissions"**
4. Push to `master` or `main` to trigger the first build

## Granting All Files Access on Newer Android Devices

For local plugin testing, you need to grant the app "All Files Access" on newer Android devices (Android 11 and above):

### Using ADB
```bash
adb shell appops set --uid PACKAGE_NAME MANAGE_EXTERNAL_STORAGE allow
```

Replace `PACKAGE_NAME` with:
- **debug**: `com.lagradost.cloudstream3.prerelease.debug`
- **prerelease**: `com.lagradost.cloudstream3.prerelease`
- **stable**: `com.lagradost.cloudstream3`

## License

Everything in this repo is released into the public domain. You may use it however you want with no conditions whatsoever.

## Attribution

This template as well as the gradle plugin and the whole plugin system is heavily based on [Aliucord](https://github.com/Aliucord).
