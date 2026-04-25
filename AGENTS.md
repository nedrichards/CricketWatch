# Repository Guidelines

## Project Structure & Module Organization
This is a single-module Android Wear OS app. The root Gradle project is `CricketWatch`, with application code in `app/`.

- `app/src/main/java/com/nedrichards/cricketwatch/`: Kotlin source, including `MainActivity`, Compose screens, repository, API interface, models, and view model.
- `app/src/main/res/`: Android resources such as launcher assets and styles.
- `app/src/test/java/com/nedrichards/cricketwatch/`: JVM unit tests.
- `app/src/test/resources/`: JSON fixtures used by repository tests and offline development.
- `gradle/` and `gradlew`: Gradle wrapper files; use the wrapper rather than a system Gradle install.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: builds a debug APK for local installation or emulator testing.
- `./gradlew :app:testDebugUnitTest`: runs JVM unit tests for the app module.
- `./gradlew :app:compileDebugKotlin`: checks Kotlin compilation without producing a full APK.
- `./gradlew clean`: removes generated build outputs when stale artifacts cause confusing results.

Before running the app, create `local.properties` in the repository root and add `CRICKET_API_KEY=your_key_here`.

## Coding Style & Naming Conventions
Use Kotlin with 4-space indentation and the existing Android/Kotlin style. Prefer small, focused classes and functions matching current names such as `CricketRepository`, `CricketViewModel`, and `MatchListScreen`.

Compose UI functions should use PascalCase names and live near related UI code in `Screens.kt` unless a split becomes clearly useful. Data models should remain in `Models.kt` unless they grow into a distinct domain area.

## Testing Guidelines
Tests use JUnit 4. Place unit tests under `app/src/test/java/...` and name test files after the class or behavior under test, for example `CricketRepositoryTest.kt`. Keep API examples as JSON fixtures under `app/src/test/resources/` instead of hard-coding large payloads in tests.

Run `./gradlew :app:testDebugUnitTest` before opening a pull request. Add focused tests for filtering, parsing, sorting, and API-response edge cases.

## Commit & Pull Request Guidelines
Recent commits use short, imperative summaries such as `Render team-first score rows` and `Bump Android Gradle plugin`. Keep subject lines concise and describe the user-visible or technical change.

Pull requests should include a brief summary, test results, and screenshots or emulator notes for UI changes on Wear OS. Link related issues when available, and call out any changes to API-key handling, network behavior, or fixture data.

## Security & Configuration Tips
Do not commit `local.properties`, API keys, generated APKs, or build outputs. Keep CricAPI credentials injected through `BuildConfig.CRICKET_API_KEY` only, and prefer fixture-backed tests over live API calls.
