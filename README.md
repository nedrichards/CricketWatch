# CricketWatch for Wear OS

CricketWatch is a lightweight, "glance-focused" Wear OS application designed for cricket fans to track live scores for **England** and **Surrey** senior matches directly from their wrist.

## Features

- **Glanceable Scores**: Team-first score rows with compact abbreviations, aligned runs/wickets, overs, and an active batting marker for fast reading on a watch.
- **Team-Specific Filtering**: Automatically filters for England and Surrey senior matches, excluding youth (U19) and irrelevant fixtures to keep your feed focused.
- **Rotary/Crown Support**: Smooth scrolling through match lists using the physical watch crown or rotary input.
- **Live Updates**: Automatic background refreshing every 60 seconds with "Last Updated" timestamps.
- **Clean UI**: Automatically strips technical codenames (e.g., `[LEICS]`) from team names for a more professional look.
- **Offline Development**: Includes JSON fixtures in test resources for development without active API connectivity.

## Technical Stack

- **Kotlin & Coroutines**: Efficient asynchronous networking and data processing.
- **Jetpack Compose for Wear OS**: Modern, declarative UI designed specifically for circular and square watch faces.
- **Retrofit & OkHttp**: Robust API integration with parallel request handling to minimize latency.
- **CricAPI Integration**: Utilizes the `cricapi.com` v1 endpoints for reliable match data.

## Getting Started

### Prerequisites
- Android Studio Iguana or newer.
- A Wear OS device or emulator (API 30+).

### Configuration
The app requires a `cricapi.com` API key. For security, this is managed via `local.properties`:

1.  Open or create `local.properties` in the project root.
2.  Add your API key: `CRICKET_API_KEY=your_key_here`.
3.  The build system will automatically inject this into the app via `BuildConfig`.

### Building
```bash
./gradlew assembleDebug
```

## Project Structure
- `CricketRepository`: Handles data fetching, parallelization, and exclusionary filtering.
- `CricketViewModel`: Manages the UI state and periodic refresh logic.
- `Screens.kt`: Contains the `MatchListScreen` and `MatchCard` components optimized for Wear OS.

## Licence
This project is licensed under the GNU General Public License v3.0 or later (GPL-3.0-or-later).

---
*Co-authored with a bunch of different AIs*
