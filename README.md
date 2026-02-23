# English Dictionary App (Android, Offline)

Kotlin + Jetpack Compose dictionary app for MTG/TOEIC style decks.

## Implemented (Phase 1)

- Bundled deck loading from `app/src/main/assets/decks/index.json`
- Deck list
- Search (exact/prefix/substring ranking)
- Entry detail screen
- Add/edit entry (stored locally in Room)
- Basic SRS quiz (`Again` / `Good` / `Easy`)
- Local study state/log storage (Room)
- SAF-based backup export/import (`CreateDocument` / `OpenDocument`)
- Conflict policy: same normalized term in deck updates existing `entry_id` instead of creating duplicates

## Project structure

- `app/src/main/assets/decks/` deck assets
- `app/src/main/java/com/example/englishdictionary/data` data loading/repository
- `app/src/main/java/com/example/englishdictionary/domain` ranking/SRS logic
- `app/src/main/java/com/example/englishdictionary/ui` Compose UI + ViewModel

## Build

1. Install JDK 17 and set `JAVA_HOME`.
2. Install Android SDK (API 35).
3. Run:

```bash
./gradlew assembleDebug
```

## Tests

Unit tests included:

- `SearchRankerTest`
- `SrsSchedulerTest`

Run:

```bash
./gradlew testDebugUnitTest
```
