# Android Structure

A Jetpack Compose Android starter with three environment flavors (**dev**, **uat**, **prp**) and a
clean layered architecture (`presentation → domain ← data`) wired end-to-end through a single
manual DI container. The sample feature loads todos from the real
`https://jsonplaceholder.typicode.com/todos` REST API with Retrofit.

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
| Networking | Retrofit `3.0.0` + `converter-kotlinx-serialization` |
| JSON | kotlinx.serialization `1.9.0` (Kotlin serialization compiler plugin) |
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
| `dev` | Development | `com.thoen.androidstructure.dev` | `1.0-dev` | `https://jsonplaceholder.typicode.com/` | Android Structure Dev | [app/src/dev](./app/src/dev) |
| `uat` | User acceptance testing | `com.thoen.androidstructure.uat` | `1.0-uat` | `https://jsonplaceholder.typicode.com/` | Android Structure UAT | [app/src/uat](./app/src/uat) |
| `prp` | Production | `com.thoen.androidstructure` | `1.0` | `https://jsonplaceholder.typicode.com/` | Android Structure | inherits [app/src/main](./app/src/main) |

All three flavors currently point at the same public demo API so every variant runs out of the box;
replace the `uat`/`prp` hosts with your real backends when you have them — nothing else changes.

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
│   ├── di/AppContainer.kt                # Manual DI graph: Retrofit, repository, use cases
│   ├── domain/
│   │   ├── model/Todo.kt                 # Pure Kotlin model
│   │   ├── repository/TodoRepository.kt  # Repository interface
│   │   └── usecase/GetTodosUseCase.kt    # One class per use case
│   ├── data/
│   │   ├── remote/TodoApiService.kt      # Retrofit interface (@GET("todos"))
│   │   ├── remote/dto/TodoDto.kt         # @Serializable wire model + toDomain() mapper
│   │   └── repository/TodoRepositoryImpl.kt
│   └── presentation/
│       ├── main/                         # MainScreen + MainViewModel + MainUiState
│       └── theme/                        # Color.kt, Theme.kt, Type.kt
├── dev/res/values/strings.xml            # dev app name override
├── uat/res/values/strings.xml            # uat app name override
├── test/…                                # JVM unit tests (use case + mapper tests)
└── androidTest/…                         # Instrumented UI tests
```

### Layer rules

| Layer | May depend on | Must not depend on |
|---|---|---|
| `presentation` | `domain` | `data`, Android framework details beyond Compose |
| `domain` | nothing (pure Kotlin) | `data`, `presentation`, `android.*` |
| `data` | `domain` | `presentation` |
| `di` | everything (composition root) | — |

`MainViewModel` never sees Retrofit, and `TodoRepositoryImpl` never sees Compose — the domain
interfaces in the middle keep both sides swappable. The next two sections show how the
presentation layer and the network layer are put together.

## Presentation layer: Screen, UiState and ViewModel

Every screen is split into three small pieces, each with one job. `main` is the worked example:

| Piece | File | Responsibility |
|---|---|---|
| `MainUiState` | [MainUiState.kt](./app/src/main/java/com/thoen/androidstructure/presentation/main/MainUiState.kt) | Immutable snapshot of everything the screen renders |
| `MainViewModel` | [MainViewModel.kt](./app/src/main/java/com/thoen/androidstructure/presentation/main/MainViewModel.kt) | Runs use cases and turns results/errors into state |
| `MainScreen` | [MainScreen.kt](./app/src/main/java/com/thoen/androidstructure/presentation/main/MainScreen.kt) | Renders state and emits user events — no business logic |

### UiState — one immutable object per screen

```kotlin
data class MainUiState(
    val isLoading: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val errorMessage: String? = null,
) {
    val completedCount: Int = todos.count { it.completed }
    val progress: Float = if (todos.isEmpty()) 0f else completedCount.toFloat() / todos.size
}
```

- Holds **only data the UI needs** — no `Context`, no `MutableState`, no Android types.
- Derived values (`completedCount`, `progress`) live here, so the composable never recomputes
  business rules and the same numbers are testable in a plain JVM test.
- `isLoading`, `errorMessage` and the data describe the four visual states the screen switches
  between: loading, error, empty, content.
- Defaults (`= false`, `= emptyList()`) make previews and test fixtures one-liners.

### ViewModel — state holder and use-case executor

```kotlin
class MainViewModel(private val getTodos: GetTodosUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init { loadTodos() }

    fun loadTodos() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { getTodos() }
                .onSuccess { todos -> _uiState.update { it.copy(isLoading = false, todos = todos) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    companion object {
        fun factory(getTodos: GetTodosUseCase): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(getTodos) }
        }
    }
}
```

- Exposes a **read-only** `StateFlow`; `_uiState` stays private, so only the ViewModel can mutate it.
- Survives configuration changes (rotation, theme switch) — the request is not re-fired and the list
  is not lost.
- Depends on `domain` only (`GetTodosUseCase`); it never imports Retrofit or the repository impl.
- Failures become **state** (`errorMessage`), never an exception thrown at the UI.
- Work is launched in `viewModelScope`, so in-flight requests are cancelled when the screen dies.
- `factory(...)` + `viewModel(factory = ...)` in `MainActivity` provides constructor injection
  without a DI framework, using the `AppContainer` graph.

### Screen — two overloads: wiring and rendering

```kotlin
// 1) Stateful overload: binds the ViewModel. Used by MainActivity.
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreen(uiState = uiState, onRefresh = viewModel::loadTodos, modifier = modifier)
}

// 2) Stateless overload: pure (state) -> UI. Used by @Preview and UI tests.
@Composable
fun MainScreen(uiState: MainUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) { … }
```

- `collectAsStateWithLifecycle()` only collects while the screen is visible — no wasted work in the
  background.
- User intents travel **up as lambdas** (`onRefresh`), never as a ViewModel passed into children.
- The stateless overload owns a single `when` that maps each state to exactly one visual:
  `LoadingState`, `ErrorState`, `EmptyState` or `TodoList`.
- The `@Preview` feeds a hand-made `MainUiState` — no emulator, no network.

### Data flow (unidirectional)

```text
User taps refresh ──▶ MainScreen(onRefresh) ──▶ MainViewModel.loadTodos()
                                                        │ calls
                                                        ▼
                                                 GetTodosUseCase
                                                        │
                                                        ▼
                                    TodoRepository ◀── TodoRepositoryImpl
                                                        │
                                          Retrofit + kotlinx.serialization
                                                        │
                                    _uiState (StateFlow<MainUiState>)
                                                        ▼
                                   MainScreen recomposes with the new state
```

### Rules of thumb

| Do | Don't |
|---|---|
| Put every value the UI needs into `UiState` | Add a parameter to the Screen for each new value |
| Pass events up as lambdas | Pass the ViewModel into child composables |
| Expose `StateFlow<UiState>` | Expose `MutableStateFlow`, `LiveData`, or raw model lists |
| Keep Compose imports out of the ViewModel | Import `androidx.compose.*` in a ViewModel |
| Keep derived/formatting logic in `UiState`/ViewModel | Compute business rules inside composables |
| Model errors as state (`errorMessage`) | Throw exceptions out of `viewModelScope.launch` |

## Networking

The app loads `https://jsonplaceholder.typicode.com/todos`; the base URL comes from the flavor's
`BuildConfig.API_BASE_URL`.

| Piece | File | Notes |
|---|---|---|
| API contract | [TodoApiService.kt](./app/src/main/java/com/thoen/androidstructure/data/remote/TodoApiService.kt) | `@GET("todos") suspend fun getTodos(): List<TodoDto>` |
| Wire model | [TodoDto.kt](./app/src/main/java/com/thoen/androidstructure/data/remote/dto/TodoDto.kt) | `@Serializable data class` |
| Mapping | [TodoDtoMapper.kt](./app/src/main/java/com/thoen/androidstructure/data/remote/dto/TodoDtoMapper.kt) | `TodoDto.toDomain(): Todo` |
| Repository | [TodoRepositoryImpl.kt](./app/src/main/java/com/thoen/androidstructure/data/repository/TodoRepositoryImpl.kt) | Implements the domain interface |
| Retrofit setup | [AppContainer.kt](./app/src/main/java/com/thoen/androidstructure/di/AppContainer.kt) | `baseUrl`, `Json { ignoreUnknownKeys = true }` |

- `android.permission.INTERNET` is declared in [AndroidManifest.xml](./app/src/main/AndroidManifest.xml)
  — without it every call fails with `UnknownHostException`/`SecurityException`.
- Retrofit 3 + `converter-kotlinx-serialization`; the JSON model is parsed by kotlinx.serialization
  (the `org.jetbrains.kotlin.plugin.serialization` plugin is applied in `app/build.gradle.kts`).
- R8 keep rules for serializers live in [rules.keep](./app/src/main/keepRules/rules.keep); the
  release build runs the same code path as debug.
- **Adding an endpoint:** declare the `suspend fun` in `TodoApiService`, add/beside a DTO and its
  `toDomain()` mapper, add a repository method, then a use case. Nothing above `data` learns that
  Retrofit exists.
- **Errors** (no connectivity, HTTP 4xx/5xx, JSON mismatch) reach `MainUiState.errorMessage` and
  render the retry screen with a *Try again* button.

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

# 3 release APKs — R8-minified and signed with the debug key (see “Release signing”)
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
| dev release APK | `./gradlew :app:assembleDevRelease` | `app/build/outputs/apk/dev/release/app-dev-release.apk` |
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
│   └── release/app-dev-release.apk
├── uat/
│   ├── debug/app-uat-debug.apk
│   └── release/app-uat-release.apk
└── prp/
    ├── debug/app-prp-debug.apk
    └── release/app-prp-release.apk
```

Each folder also contains `output-metadata.json` with the `applicationId`, `versionCode` and
`versionName` that went into that APK — handy when you need to know which flavor produced it.

In Android Studio, the same tasks are available under **Gradle tool window → app → Tasks → build**
(e.g. `assembleDebug`), or use **Build → Build Bundle(s) / APK(s) → Build APK(s)** to build only the
currently selected variant.

### Release signing

Today `buildTypes.release` is wired to the **debug** signing config
(`signingConfig = signingConfigs.getByName("debug")`) so release APKs install locally for testing.
That is why the files are plain `app-<flavor>-release.apk` instead of `-unsigned`.

Before publishing, create a real keystore and replace that line with a `signingConfigs` block that
reads the store file/passwords from environment variables or `~/.gradle/gradle.properties` — never
commit them.

### Release optimization

`optimization { enable = true }` keeps R8 minification/shrinking on for release builds.

Do **not** add `packageScope` (class repackaging) to that block: repackaging moved a class next to
`kotlin.sequences` internals and crashed the R8 build at startup with
`IllegalAccessError: Class kotlin.sequences.d extended by class … is inaccessible`. Plain
`enable = true` was verified to load todos correctly in a release build, including the
kotlinx.serialization keep rules.

## Adding a feature

1. **domain** – add the model, the repository interface, and a use case in `domain/`.
2. **data** – add the DTO + mapper in `data/remote/dto/`, the endpoint in the Retrofit service, and
   the repository implementation in `data/repository/`.
3. **di** – expose the use case from `AppContainer`.
4. **presentation** – add `XxxUiState`, `XxxViewModel` (with a `factory { }`) and `XxxScreen`
   (stateful + stateless overload + `@Preview`).
5. **wire** – navigate to the screen from `MainActivity`/another screen and pass the use case from
   `container`.
6. **test** – cover the use case with a fake repository and the mapper in `app/src/test`.

## Configuration notes

- [gradle/libs.versions.toml](./gradle/libs.versions.toml) is the single source of dependency versions.
- `gradle.properties` enables the configuration cache, parallel builds and the build cache.
- Release builds use AGP 9's `optimization { }` block instead of the legacy `minifyEnabled`/
  `shrinkResources` pair; kotlinx.serialization keep rules live in `app/src/main/keepRules/rules.keep`.
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

**“jlink executable … does not exist” during a command-line build**
The Gradle daemon picked the stripped JRE shipped with the VS Code Java extension (it has no
`jlink`), which AGP 9 needs. Build with a full JDK 25 instead:
`./gradlew … -Dorg.gradle.java.home=$HOME/.sdkman/candidates/java/25-tem` (or export `JAVA_HOME`
to a full JDK). Android Studio is unaffected — it uses its bundled JBR.

**App shows “Couldn't load tasks”**
Check that `android.permission.INTERNET` is present in the merged manifest, that the device has
connectivity, and that `BuildConfig.API_BASE_URL` ends with a `/` (Retrofit requires it).
