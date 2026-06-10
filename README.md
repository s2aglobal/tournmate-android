# TournMate Android

Native Android app for TournMate — organize tournaments, join open play sessions, discover courts, and track calories.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Firebase (Auth + Firestore + Messaging + Analytics + Crashlytics)

## Setup

### Prerequisites

- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 35

### Firebase Configuration

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. For **tournmate-dev** project:
   - Add an Android app with package name `com.s2aglobal.tournmate.dev`
   - Download `google-services.json`
   - Place it at `app/src/dev/google-services.json`
3. For **tournmate-prod** project:
   - Add an Android app with package name `com.s2aglobal.tournmate`
   - Download `google-services.json`
   - Place it at `app/src/prod/google-services.json`

### Google Maps (optional for Phase 6)

Add your Maps API key to `local.properties`:

```properties
MAPS_API_KEY=your_api_key_here
```

### Build & Run

```bash
# Dev debug build
./gradlew installDevDebug

# Prod release build
./gradlew assembleProdRelease
```

Or in Android Studio: select the **devDebug** build variant and click Run.

## Build Flavors

| Flavor | Package Name | Firebase Project |
|--------|-------------|-----------------|
| `dev` | `com.s2aglobal.tournmate.dev` | tournmate-dev |
| `prod` | `com.s2aglobal.tournmate` | tournmate-prod |

## Project Structure

```
app/src/main/java/com/s2aglobal/tournmate/
├── di/                 # Hilt dependency injection modules
├── domain/model/       # Data classes + enums (Player, Tournament, Match, etc.)
├── data/
│   ├── repository/     # Repository interfaces + Firestore implementations
│   ├── local/          # DataStore (CurrentUserStore)
│   └── mapper/         # Firestore document <-> domain model mappers
├── service/auth/       # Firebase Auth service (Google + Email/Password)
├── ui/
│   ├── theme/          # Material 3 theme (brand purple, typography)
│   ├── navigation/     # Compose Navigation routes + NavHost
│   ├── component/      # Reusable UI components
│   └── screen/         # Feature screens organized by area
└── util/               # Utilities (deep links, sharing, analytics)
```

## Architecture

Mirrors the iOS app's MVVM + DI pattern:

- **SwiftUI → Jetpack Compose**
- **@Observable → StateFlow**
- **@Environment(\.container) → Hilt injection**
- **Protocol-based repos → Kotlin interface + Firestore impl**
- **Direct Firestore access** (same as iOS — no REST API)

## Phase Status

- [x] Phase 1: Project scaffold + Auth
- [ ] Phase 2: Tournament core
- [ ] Phase 3: Match engine + Brackets
- [ ] Phase 4: Open Play
- [ ] Phase 5: Players + Ratings
- [ ] Phase 6: Courts + Discover
- [ ] Phase 7: Calories + Notifications
- [ ] Phase 8: Polish + Parity
