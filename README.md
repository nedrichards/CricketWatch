# CricketWatch for Wear OS

CricketWatch is a lightweight, "glance-focused" Wear OS application designed for cricket fans to track live scores for **England** and **Surrey** senior matches directly from their wrist.

## Features

- **Glanceable Scores**: Optimized for small screens, providing immediate access to current scores, wickets, and overs.
- **Team-Specific Filtering**: Automatically filters for England and Surrey senior matches, excluding youth (U19) and irrelevant fixtures to keep your feed focused.
- **Batting Indicators**: Highlights the current batting team in **Cyan** with a `*` indicator for easy status checking at a glance.
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
The app requires a `cricapi.com` API key. Currently, this is hardcoded in `MainActivity.kt` for development purposes.

### Building
```bash
./gradlew assembleDebug
```

## Performance Optimizations

- **Parallel Fetching**: Hits multiple API endpoints (`currentMatches` and `cricScore`) concurrently to reduce wait times.
- **Thread Safety**: All networking and parsing operations are strictly offloaded to `Dispatchers.IO`.
- **Memory Efficient**: Streamlined to a single-screen experience to minimize resource usage on wearable hardware.

## Project Structure
- `CricketRepository`: Handles data fetching, parallelization, and exclusionary filtering.
- `CricketViewModel`: Manages the UI state and periodic refresh logic.
- `Screens.kt`: Contains the `MatchListScreen` and `MatchCard` components optimized for Wear OS.

---
*Developed by Ned Richards*
