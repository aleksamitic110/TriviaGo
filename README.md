# TriviaGo

Android trivia/quiz app (faculty project) built with Kotlin and Jetpack Compose.

## Repository layout

- `TriviaGo/` - Android (Gradle) project
- `Dokumentacija/` - project documentation (DOCX)

## What the app does

Based on the code in `TriviaGo/app/src/main/java/com/ogaivirt/triviago`:

- Authentication (Firebase Auth)
  - Email/password sign-up + email verification
  - Google Sign-In
- Quizzes (Firestore-backed)
  - Create quizzes and add/edit questions
  - Public/private quizzes
  - Subscribe/unsubscribe to quizzes
  - Rating, reporting, and statistics
- Role-based areas (e.g. admin/support dashboards)
- Profiles (Firestore + Firebase Storage for profile images)
- Location/maps
  - Location permissions in `AndroidManifest.xml`
  - Google Maps key wired via `@string/google_maps_api_key`

## Tech stack

- Kotlin + Jetpack Compose
- Navigation Compose (single-activity)
- Hilt (DI)
- Firebase: Auth, Firestore, Storage
- Google Identity / Play Services Auth (Google Sign-In)
- Google Maps (Maps Compose + Play Services Location)
- Retrofit (Places API calls)
- DataStore (preferences)
- Gradle wrapper `8.13` (`TriviaGo/gradle/wrapper/gradle-wrapper.properties`)

## Requirements

- Android Studio (recent stable)
- JDK 17 (project uses Android Gradle Plugin `8.11.2` and Kotlin `2.0.21` in `TriviaGo/gradle/libs.versions.toml`)
- Android SDK installed (you'll need the platform for `compileSdk = 36`)
- SDK levels: `minSdk = 24`, `targetSdk = 36` (see `TriviaGo/app/build.gradle.kts`)

## Local configuration

Gradle typically reads your Android SDK location from `TriviaGo/local.properties`:

```properties
sdk.dir=C:\\Path\\To\\Android\\Sdk
```

This file is machine-specific and should not be committed.

## Quick start (Android Studio)

1. Android Studio -> **Open** -> select `TriviaGo/`.
2. Let Gradle sync and download dependencies.
3. Run the `app` configuration on an emulator or connected device.

## Build from the command line

From the repo root:

```powershell
cd .\TriviaGo\
.\gradlew.bat :app:assembleDebug
```

Run tests:

```powershell
cd .\TriviaGo\
.\gradlew.bat test
```

## Firebase / Google configuration

This project expects:

- Firebase config: `TriviaGo/app/google-services.json`
- Google Sign-In client id: `TriviaGo/app/src/main/res/values/strings.xml` (`web_client_id`)
- Google Maps API key: `TriviaGo/app/src/main/res/values/strings.xml` (`google_maps_api_key`)

To run on your own Firebase project:

1. Create a Firebase project and add an Android app with applicationId `com.ogaivirt.triviago`.
2. Download `google-services.json` into `TriviaGo/app/`.
3. Enable Firebase Auth, Firestore, and Storage.
4. Configure Google Sign-In and update `web_client_id`.
5. Create a Google Maps API key and update `google_maps_api_key`.

## Firestore collections referenced in code

Repository code references (at least) these collections:

- `users`
- `quizzes`
- `statistics`
- `ratings`
- `reports`
- `tickets`
- `notifications`
- `verificationRequests`

## Code organization (high level)

`TriviaGo/app/src/main/java/com/ogaivirt/triviago/`:

- `ui/` - Compose screens/components/theme
- `domain/` - models + repository interfaces
- `data/` - repository implementations + location/places helpers
- `di/` - Hilt modules (providers)
- `MainActivity.kt` - navigation graph
- `TriviaGoApp.kt` - application class (`@HiltAndroidApp`)

## Documentation

- `Dokumentacija/D02_Vizija_sistema - TriviaGO.docx`
- `Dokumentacija/D04_Spec_Zahteva - TriviaGO.docx`

## Notes / security

API keys and service configs are currently present in-repo (e.g. `google-services.json` and keys under `strings.xml`). If this repository is public or shared outside the team, rotate/revoke keys and move secrets to a safer mechanism (CI secrets / local-only configs).
