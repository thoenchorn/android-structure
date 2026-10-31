# Android Structure

A Jetpack Compose Android starter with three environment flavors (**dev**, **uat**, **prp**) and a
clean layered architecture (`presentation → domain ← data`) wired end-to-end through a single
manual DI container.

## Tech stack

| Area | Version / choice |
|---|---|
| Kotlin | 2.2.10 (Compose compiler plugin, AGP built-in Kotlin) |
| Android Gradle Plugin | 9.4.1 |
| Gradle | 9.6.0 (wrapper) |
| JDK | 25 (Gradle daemon toolchain) |
| compileSdk / minSdk / targetSdk | 37 / 37 / 37 |
| UI | Jetpack Compose + Material 3 (BOM `2026.02.01`) |
| Lifecycle | 2.11.0 (`lifecycle-runtime-ktx`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`) |
| Tests | JUnit 4 (unit), Espresso + Compose UI Test (instrumented) |

## Requirements

- **Android Studio Rabbit 3 (2026.2.3) or newer** — AGP 9 requires a matching IDE.
- Android SDK Platform 37 installed.
- No manual Gradle/JDK install needed: the wrapper and `gradle/gradle-daemon-jvm.properties` handle it.

## Getting started

1. Open the project folder in Android Studio and let **Gradle sync** finish.
2. Open **Build Variants** (View → Tool Windows → Build Variants) and select `devDebug`.
3. Press ▶ to run, or use the command line:

```bash
./gradlew :app:installDevDebug          # build + install the dev flavor
./gradlew :app:assembleUatDebug         # build the uat APK
./gradlew :app:testDevDebugUnitTest     # unit tests
```

## Build flavors

Defined in [app/build.gradle.kts](./app/build.gradle.kts) under `productFlavors` (dimension `env`).

| Flavor | Meaning | applicationId | versionName | `API_BASE_URL` | App name | Flavor source set |
|---|---|---|---|---|---|---|
| `dev` | Development | `com.thoen.androidstructure.dev` | `1.0-dev` | `https://api-dev.example.com/` | Android Structure Dev | [app/src/dev](./app/src/dev) |
| `uat` | User acceptance testing | `com.thoen.androidstructure.uat` | `1.0-uat` | `https://api-uat.example.com/` | Android Structure UAT | [app/src/uat](./app/src/uat) |
| `prp` | Production | `com.thoen.androidstructure` | `1.0` | `https://api.example.com/` | Android Structure | inherits [app/src/main](./app/src/main) |

Because `dev` and `uat` add an `applicationIdSuffix`, all three builds can be installed on the same
device side by side.

### Where flavor configuration comes from

- **API endpoints / flags** → `buildConfigField` per flavor, exposed as `BuildConfig.API_BASE_URL`.
- **App label** → flavor resource overrides in `app/src/<flavor>/res/values/strings.xml`.
- **Generated `BuildConfig`** → `app/build/generated/source/buildConfig/<flavor>/<buildType>/…`
  (enabled with `buildFeatures { buildConfig = true }`).

Values are read once in [AndroidStructureApp](./app/src/main/java/com/thoen/androidstructure/AndroidStructureApp.kt)
and injected downwards — no layer below `di` touches `BuildConfig`.

## Project structure

```text
app/src/
├── main/java/com/thoen/androidstructure/
│   ├── AndroidStructureApp.kt            # Application: creates the AppContainer
│   ├── MainActivity.kt                   # Single activity, hosts Compose content
│   ├── di/AppContainer.kt                # Manual DI graph (swap for Hilt/Koin when it grows)
│   ├── domain/
│   │   ├── model/                        # Pure Kotlin models (Greeting)
│   │   ├── repository/                   # Repository interfaces
│   │   └── usecase/                      # One class per use case (GetGreetingUseCase)
│   ├── data/repository/                  # Repository implementations (network/db go here)
│   └── presentation/
│       ├── main/                         # MainScreen + MainViewModel + MainUiState
│       └── theme/                        # Color.kt, Theme.kt, Type.kt
├── dev/res/values/strings.xml            # dev app name override
├── uat/res/values/strings.xml            # uat app name override
├── test/…                                # JVM unit tests (use-case tests)
└── androidTest/…                         # Instrumented UI tests
```

### Layer rules

| Layer | May depend on | Must not depend on |
|---|---|---|
| `presentation` | `domain` | `data`, Android framework details beyond Compose |
| `domain` | nothing (pure Kotlin) | `data`, `presentation`, `android.*` |
| `data` | `domain` | `presentation` |
| `di` | everything (composition root) | — |

Screens are stateless: `MainScreen(uiState, onRetry)` renders, `MainViewModel` exposes
`StateFlow<MainUiState>`, and events flow back as lambdas.

## Common tasks

| Goal | Command |
|---|---|
| Build **all** debug APKs | `./gradlew :app:assembleDebug` |
| Build all release APKs | `./gradlew :app:assembleRelease` |
| Build everything | `./gradlew :app:assemble` |
| Compile one variant | `./gradlew :app:compileDevDebugSources` |
| Build one variant | `./gradlew :app:assembleUatDebug` |
| Install one variant | `./gradlew :app:installDevDebug` |
| Unit tests | `./gradlew :app:testDevDebugUnitTest` |
| Lint | `./gradlew :app:lintDevDebug` |
| Clean | `./gradlew clean` |
| List tasks | `./gradlew :app:tasks --all` |

> Only the `assemble*` family has flavor-agnostic aggregates: `assemble`, `assembleDebug`,
> `assembleRelease` and `assembleDev` / `assembleUat` / `assemblePrp`. Tasks such as
> `compileDebugSources`, `installDebug`, `testDebugUnitTest` and `lintDebug` do **not** exist —
> use the variant-qualified names (`compileDevDebugSources`, `installDevDebug`,
> `testDevDebugUnitTest`, `lintDevDebug`).

## Building APKs for all flavors

`assembleDebug` and `assembleRelease` are aggregate tasks: they build **every flavor** for that
build type in a single run.

```bash
# 3 debug APKs (dev + uat + prp) — debug-signed and installable
./gradlew :app:assembleDebug

# 3 release APKs — R8-minified; unsigned until a signing config is added
./gradlew :app:assembleRelease

# everything: 3 flavors × 2 build types = 6 APKs
./gradlew :app:assemble

# one flavor, both build types (devDebug + devRelease)
./gradlew :app:assembleDev          # :app:assembleUat / :app:assemblePrp

# one variant only
./gradlew :app:assembleUatDebug
```

To run several variants while keeping earlier results even if one fails:

```bash
./gradlew :app:assembleDevDebug :app:assembleUatDebug :app:assemblePrpDebug --continue
```

### Building only the `dev` flavor

Every variant of other flavors is skipped — only `dev` tasks run.

| Goal | Command | Output |
|---|---|---|
| dev debug APK | `./gradlew :app:assembleDevDebug` | `app/build/outputs/apk/dev/debug/app-dev-debug.apk` |
| dev release APK | `./gradlew :app:assembleDevRelease` | `app/build/outputs/apk/dev/release/app-dev-release-unsigned.apk` |
| both dev build types | `./gradlew :app:assembleDev` | the two files above |
| dev AAB (Play bundle) | `./gradlew :app:bundleDevDebug` | `app/build/outputs/bundle/devDebug/app-dev-debug.aab` |
| build + install on device | `./gradlew :app:installDevDebug` | installed as `com.thoen.androidstructure.dev` |
| unit tests | `./gradlew :app:testDevDebugUnitTest` | report in `app/build/reports/tests/testDevDebugUnitTest/` |
| lint | `./gradlew :app:lintDevDebug` | `app/build/reports/lint-results-devDebug.html` |

Swap `Dev` for `Uat` or `Prp` to target another environment (e.g. `:app:assembleUatDebug`,
`:app:installPrpDebug`).

### Where the APKs land

```text
app/build/outputs/apk/
├── dev/
│   ├── debug/app-dev-debug.apk
│   └── release/app-dev-release-unsigned.apk
├── uat/
│   ├── debug/app-uat-debug.apk
│   └── release/app-uat-release-unsigned.apk
└── prp/
    ├── debug/app-prp-debug.apk
    └── release/app-prp-release-unsigned.apk
```

Each folder also contains `output-metadata.json` with the `applicationId`, `versionCode` and
`versionName` that went into that APK — handy when you need to know which flavor produced it.

In Android Studio, the same tasks are available under **Gradle tool window → app → Tasks → build**
(e.g. `assembleDebug`), or use **Build → Build Bundle(s) / APK(s) → Build APK(s)** to build only the
currently selected variant.

### Release signing

Release builds are currently named `*-unsigned.apk` because the project declares no signing config.
To ship installable release APKs, add a `signingConfigs` block to
[app/build.gradle.kts](./app/build.gradle.kts), assign it to `buildTypes.release.signingConfig`, and
keep the keystore path/passwords out of version control (environment variables or
`~/.gradle/gradle.properties`). Once signed, the file name loses the `-unsigned` suffix.

## Adding a feature

1. **domain** – add the model, the repository interface, and a use case in `domain/`.
2. **data** – implement the repository in `data/repository/`.
3. **di** – expose the use case from `AppContainer`.
4. **presentation** – add `XxxUiState`, `XxxViewModel` (with a `factory { }`) and `XxxScreen`
   (stateless version + `@Preview`).
5. **wire** – navigate to the screen from `MainActivity`/another screen and pass the use case from
   `container`.
6. **test** – cover the use case with a fake repository in `app/src/test`.

## Configuration notes

- [gradle/libs.versions.toml](./gradle/libs.versions.toml) is the single source of dependency versions.
- `gradle.properties` enables the configuration cache, parallel builds and the build cache.
- Release builds use AGP 9's `optimization { }` block instead of the legacy `minifyEnabled`/
  `shrinkResources` pair.
- The XML theme in `app/src/main/res/values/themes.xml` only supplies
  `android:Theme.Material.Light.NoActionBar`; colors and typography come from
  `AndroidStructureTheme` in Compose.

## Troubleshooting

**“Cannot locate tasks that match ':app:compileDebugSources' … is ambiguous”**
The unqualified task no longer exists; use `:app:compileDevDebugSources` (or `Uat`/`Prp`). If an
IDE action produced it, the IDE is using a stale Gradle model — sync the project.

**“Unresolved reference 'BuildConfig'” / “Unresolved reference 'viewmodel' on receiver of type
`LibrariesForLibs.AndroidxLifecycleLibraryAccessors`”**
Both are symptoms of a stale Gradle model in Android Studio: `BuildConfig` and the version-catalog
accessors are generated during import. Run **File → Sync Project with Gradle Files**. If that does
not help: **File → Invalidate Caches… → Invalidate and Restart**. The command-line build is the
source of truth and should stay green.

**False errors in VS Code (“Unresolved reference: ComponentActivity”, etc.)**
The Kotlin Language Server (`fwcd.kotlin`) cannot resolve this project's classpath: its Gradle init
script uses the legacy `getBootClasspath()`/`applicationVariants` APIs that AGP 9 removed, and its
tasks conflict with the configuration cache. [.vscode/settings.json](./.vscode/settings.json)
disables its diagnostics (reload the window after changing it); use Android Studio for Kotlin
analysis or the code keeps compiling fine.
