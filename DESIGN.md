# DESIGN.md — Vocabulary Companion (MTG / TOEIC) for Android

> Goal: Read MTG story text smoothly (stay in English as much as possible) while minimizing stop-to-lookup moments.
> Secondary goal: Grow decks (MTG story terms, TOEIC business terms, etc.) and keep updating entries over time.
> Distribution: Android APK (manual install). Dictionary content is bundled for offline use in Phase 1.

---

## 0. Product Summary

### What this app is
- Android vocabulary app with:
  - Decks (MTG Story Terms, TOEIC Business, ...)
  - Dictionary (fast lookup, tags, synonyms, canonical translation notes)
  - SRS learning (spaced repetition)
  - Optional Reader mode (future)

### Why not rely on translation sites
- MTG lore terms often drift in nuance when translated.
- Keep English term as primary, with Japanese notes as support.

---

## 1. Requirements

### 1.1 Must-have (MVP)
- Offline-first Android app
- Deck list / search
- Entry detail screen
- SRS quiz (basic)

### 1.2 Should-have
- Sync dictionary updates from a server (content updates without APK update)
- Deck versioning (diff updates)
- Tagging, synonyms, confusables
- Source quote / example sentences
- Seen-in references (where the term appeared)

### 1.3 Nice-to-have
- Reader mode (paste story text, tap to lookup)
- Highlight unknown terms automatically
- Pronunciation (IPA + TTS)
- Multi-language support (EN / JA, later ZH)

---

## 2. Non-Functional Requirements

### 2.1 Security
- Do not embed OpenAI API keys in the Android app.
- Use backend for OpenAI API use.
- Secure endpoints with auth token or signed URLs.
- Rate limit server endpoints.

### 2.2 Privacy
- Personal logs are not uploaded by default.
- Study history is local by default.
- If sync is enabled, user can choose:
  - Sync dictionary only
  - Sync dictionary + study history

### 2.3 Performance
- Search target: < 100ms for 10k entries (local index).
- App remains usable offline for dictionary + quiz.

### 2.4 Maintainability
- Explicit schema migrations
- Versioned import/export formats
- Reproducible content update pipeline

---

## 3. User Stories

### Reading flow
- Lookup a term quickly while reading and return to text.
- See MTG-specific nuance notes without over-translation.

### Learning flow
- Review due cards daily with SRS.
- Add new terms anytime and assign to a deck.

### Growth flow
- Expand decks (MTG, TOEIC, etc.) gradually.
- Update local data without reinstalling APK.

---

## 4. Data Model

### 4.1 Entities (conceptual)
- Deck
- Entry
- Tag
- EntryLink (synonyms/confusables/related)
- Example
- SourceQuote
- SrsState (per entry per deck)
- StudyLog

### 4.2 Core fields (Entry)
- entry_id
- deck_id
- term
- term_normalized
- pos
- meaning_ja
- meaning_en
- lore_note
- canonical_translation
- synonyms
- confusables
- tags
- examples
- source_quotes
- created_at
- updated_at

### 4.3 SRS fields (SrsState)
- deck_id
- entry_id
- ease
- interval_days
- due_date
- last_reviewed_at
- lapse_count
- state (NEW / LEARNING / REVIEW)

### 4.4 DB choice
- Android local: SQLite via Room

---

## 5. Entry ID and Conflict Rules

### 5.1 entry_id generation
- Bundled entries: stable ID provided in deck JSON.
- User-created entries: UUID generated when no conflict target exists.
- Storage unique key: `(deck_id, entry_id)`.

### 5.2 Merge policy (bundled vs user)
- Bundled entries are immutable base data.
- User entries are override layer.
- If `(deck_id, entry_id)` matches, user entry replaces bundled entry for display/search.

### 5.3 Add/Edit conflict policy
- If edit request includes `entry_id`, upsert that ID.
- If add request has no `entry_id` and same normalized term exists in deck:
  - Reuse existing `entry_id` and save as user override.
  - Result: no duplicate term row for the same deck.
- If no term conflict, create new UUID entry.

### 5.4 Search normalization and scope
- Normalize with lowercase and non-alnum stripping.
- Ranking order:
  1. exact term match
  2. prefix match
  3. substring match
- Phase 1 search target fields:
  - term
- Future scope fields:
  - synonyms, tags, meaning_ja, meaning_en

---

## 6. SRS Specification (Phase 1)

- Ratings: AGAIN / GOOD / EASY
- Initial state:
  - phase=NEW
  - ease=2.5
  - interval_days=0
  - due=today
- AGAIN:
  - phase=LEARNING
  - ease=max(1.3, ease-0.2)
  - interval=1 day
  - lapse_count += 1
- GOOD:
  - NEW/LEARNING -> REVIEW, interval=2
  - REVIEW -> interval=max(interval+1, round(interval*ease))
  - ease += 0.05
- EASY:
  - NEW/LEARNING -> REVIEW, interval=4
  - REVIEW -> interval=max(interval+2, round(interval*(ease+0.30)))
  - ease += 0.15

---

## 7. Backup / Import Policy

- In-app backup/import is out of scope.
- The app does not provide `Export` / `Import Backup` actions.

---

## 8. Error Handling and Empty States

- Deck load failure: show non-crashing error state and keep app usable.
- Search no result: show `0 results`.
- Quiz due empty: show `No due cards`.
- Missing entry target: show `Entry not found` and allow back navigation.

---

## 9. Migration Policy

- Room DB schema changes require explicit migration script.
- Deck schema changes require `schema_version` compatibility checks before load.

---

## 10. Dev Environment Setup (Java / Android SDK)

Phase 1 build baseline:
- JDK: 17
- Android SDK Platform: 35
- Android Build Tools: 35.0.0

### 10.1 Java setup (no root required)

Install JDK 17 into user home (example path):
- `$HOME/.local/jdk-17`

Shell env:
```bash
export JAVA_HOME="$HOME/.local/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
```

Verify:
```bash
java -version
javac -version
```

### 10.2 Android SDK setup (no root required)

SDK root (example):
- `$HOME/.local/android-sdk`

Required packages:
- `platform-tools`
- `platforms;android-35`
- `build-tools;35.0.0`

Shell env:
```bash
export ANDROID_HOME="$HOME/.local/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
```

Verify:
```bash
sdkmanager --list_installed
adb version
```

### 10.3 Project-level local.properties

Each local machine must provide `local.properties` (not shared for secrets, but SDK path is local):
```properties
sdk.dir=/home/ubuntu/.local/android-sdk
```

### 10.4 Build verification

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## 11. Additional Deck: TOEIC Business 900

### 11.1 New deck file
- Add bundled deck file: `assets/decks/toeic_business.v1.json`.
- Target vocabulary level: around TOEIC 900 business terms.
- App must recognize and list this deck from `assets/decks/index.json`.

### 11.2 Entry fields used in this deck
- `term`
- `pronunciation_ipa`
- `pos`
- `meaning_en`
- `meaning_ja`
- `preposition_usages`
- `examples`
- `verb_preposition_usages` (verb + preposition expressions with meaning)
- `common_collocations` (frequent adjective/verb + noun combinations)
- `idioms` (idiomatic expressions)
- `latin_etymology` (Latin origin note when available)
- `related_terms` (strongly associated terms, e.g. `performance + report`)

### 11.3 Display policy for TOEIC enrichments
- These fields are optional per entry, and shown only when present.
- Display targets:
  - Entry Detail table
  - Memory mode card details
- Existing deck files remain compatible; missing fields default to empty.

### 11.4 Additional deck: EIKEN Pre-2 High-frequency Words
- Add bundled deck file: `assets/decks/eiken_pre2.v1.json`.
- Register `eiken_pre2` in `assets/decks/index.json`.
- Purpose:
  - Frequent vocabulary practice for EIKEN Grade Pre-2.
  - Offline repeated review with quiz/matching/memory flows.
- Entry baseline fields:
  - `term`, `pronunciation_ipa`, `pos`, `meaning_en`, `meaning_ja`, `examples`

### 11.5 Verb + preposition detail schema
- Add optional field: `verb_preposition_details` (recommended top 5 items).
- Each item schema:
  - `expression`
  - `meaning_en`
  - `meaning_ja`
  - `example_en`
  - `example_ja`
- Display target:
  - Entry Detail screen (especially for verbs).

### 11.6 Additional decks: Chinese Daily / Chinese Business
- Use `HSK` (Chinese Proficiency Test) and `BCT` (Business Chinese Test) as the score/level basis.
- Add two bundled decks:
  - `assets/decks/chinese_daily_hsk3_4.v1.json`
    - Goal: practical daily conversation (HSK3-HSK4 range)
    - Typical tags: `chinese`, `hsk3`, `hsk4`, `daily`, `frequent`
  - `assets/decks/chinese_business_bct.v1.json`
    - Goal: business communication and work vocabulary (BCT / HSK5 range)
    - Typical tags: `chinese`, `business`, `bct`, `hsk5`, `advanced`
- Register both in `assets/decks/index.json` so they are selectable in Deck list.
- Data display policy:
  - `term`: Simplified Chinese
  - `pronunciation_ipa`: pinyin string allowed
  - include `meaning_en` + `meaning_ja` + concise examples

---

## 12. Quiz Mode Specification (Flash + Self-check)

### 12.1 Card display
- During question phase, show only:
  - word (`term`)
  - IPA (`pronunciation_ipa`)

### 12.2 Answer interaction
- User chooses one button:
  - `○` (meaning understood)
  - `×` (meaning not understood)
- After tapping, reveal answer (meaning/pos).
- Play jingle:
  - `○` => correct jingle
  - `×` => incorrect jingle

### 12.3 Auto transition
- After answer reveal, automatically move to next card.
- Transition should feel like a swipe animation.
- Auto mode waits 5 seconds before moving to the next card.
- Even when auto mode is paused, users can move to next card immediately with left swipe.

### 12.4 Initial question count selection
- User can choose:
  - `10`, `25`, `50`, `75`, `100`, `ALL`

### 12.4.1 Quiz category selection at start
- Before count selection, user can choose a quiz category:
  - `Random` (from all entries)
  - `Frequent` (high-frequency entries)
  - `Alphabet` (entries starting with selected letter)
  - `Difficult` (hard words / historically missed words)
- When `Alphabet` is selected, user chooses `A-Z` initial letter.
- Categories with 0 candidates must not start (count cards disabled).
- This is available for all decks, with stronger tag-based behavior for TOEIC/EIKEN decks.

### 12.5 End-of-quiz result flow
- Show summary result:
  - total
  - correct
  - incorrect
- Provide replay action for incorrect words only.

### 12.6 Wrong answer persistence
- Persist each `×` answer as historical log.
- Track and display cumulative wrong count per word.

### 12.7 Result detail drill-down
- On result page, tapping a word opens detail page.
- Detail shows:
  - part of speech
  - meanings
  - examples
  - usage with prepositions

### 12.8 AutoDetail in Quiz
- Add `AutoDetail` toggle in Quiz controls.
- If `AutoDetail=ON`, show detailed information automatically when answer is revealed.
- If `AutoDetail=OFF`, keep compact answer view.
- Default state is `ON`.
- `AutoDetail=ON` must render the same table fields and ordering as the entry detail screen.

---

## 13. Memory Mode Specification

### 13.1 Purpose
- Passive flash study mode without right/wrong judgment.

### 13.2 Navigation
- Show `Prev` and `Next` buttons.
- `Next` moves to next word.
- `Prev` moves to previous word.

### 13.3 Display fields
- word
- meaning
- part of speech
- pronunciation (IPA)
- usage examples
- usage with prepositions

---

## 14. UI/UX Update Requirements (Wordbook Style)

### 14.1 App icon
- Use a wordbook-like app icon (book/notebook motif).

### 14.2 Entry detail default visibility
- `Tags` and `Lore note` are hidden by default.

### 14.3 POS on card/list
- Part of speech must be visible in word list cards.

### 14.4 Entry detail layout
- Replace card-style detail blocks with a table layout:
  - Column 1: attribute
  - Column 2: value

### 14.5 Pronunciation playback
- Add a playback button in entry detail.
- Use free API for speech playback.
- Phase 1 implementation policy:
  - Prefer Android `TextToSpeech` API (free, no paid key required).

### 14.6 Edit action
- Remove Edit button from entry detail.

### 14.7 Quiz auto-next pause
- Add pause/resume toggle for auto-next.
- If not paused, cards advance automatically after answer reveal.

### 14.8 Deck-specific visual style
- MTG deck: fantasy-style colors.
- TOEIC deck: business-style colors.

### 14.9 Deck image bundle
- Support bundled deck image metadata and display image on deck list.

### 14.10 Readability-oriented font
- Use a legible typography scale and weight setup for all screens.

### 14.11 Main word emphasis
- Main word in list/detail should be slightly larger and emphasized.

### 14.12 Word list content
- Show:
  - `(E)` English meaning
  - `(J)` Japanese meaning
  - POS
  - pronunciation
  - one example
- Hide source label in list.

### 14.13 No in-app editing
- Remove `Add` and `Edit` actions from normal app flow.
- User cannot edit/create terms in-app in this mode.

---

## 15. UX Refresh and New Interaction Additions

### 15.1 Detail play controls
- In entry detail table:
  - Show play icon button (`▶`) next to `Word`.
  - Show play icon button (`▶`) next to `Examples`.
- Play button style:
  - icon-only (no text label)
  - compact width (do not consume full text-button width).

### 15.2 Swipe-right back navigation
- On entry detail screen, swiping right returns to previous screen.
- The list screen state (scroll position / selected item context) should be kept when returning.

### 15.3 Global back behavior
- Hide visible `Back` button from normal screens.
- Use right-swipe gesture as the primary back action.

### 15.4 Alphabet index on list screen
- Add A-Z index on the list screen.
- Default selected index: `a`.
- Selecting an index letter shows entries starting with that letter.
- Search term filtering applies within the current index subset.

### 15.5 Visual design refresh (iPhone-like)
- Overall UI should be less mechanical and more polished:
  - cleaner spacing
  - rounded cards
  - soft gradients
  - bright, readable visual hierarchy.

### 15.6 Startup logo
- Show app logo on startup splash screen before main navigation appears.

### 15.7 List POS labeling update
- Remove `POS:` prefix text from list items.
- Render compact line as `[IPA] POS`.

### 15.8 Enjoyable interaction tone
- Add playful, friendly visual rhythm and motion where it improves usability.
- Keep readability first; avoid noisy effects.

### 15.9 Matching mode (new, separate from Quiz)
- Add `Matching` study mode.
- Flow:
  1. Select count: `10`, `25`, `50`, `75`, `100`, `ALL`
  2. Left column: words
  3. Right column: translations (shuffled)
  4. User taps left and right to match pair
  5. Correct:
     - play correct jingle
     - matched pair disappears
     - next pair is supplied automatically
  6. Incorrect:
     - play incorrect jingle
  7. After target count is completed, show total result.
- Layout addendum:
  - Left (word) and right (meaning) cards must be shown with aligned row positions and equal heights.
  - Use more of the available width to reduce perceived misalignment.
- Correct sound addendum:
  - Replace beep-like correct feedback with a more "correct-answer" jingle-style sound.
- Completion addendum:
  - After completion, show a list of the words and their meanings under the summary.
- Refill logic addendum (anti-pattern tap prevention):
  - Do not refill immediately after one correct match.
  - Refill only after both columns have at least two empty slots.
  - Place next word/meaning into random empty slots to avoid fixed-position answering.

### 15.10 Top page backup buttons
- Backup/import features are not provided in the app.

### 15.11 Top page deck selection only
- Top page action is only deck selection.
- Remove top-page `Search/Quiz/Memory` action buttons.

### 15.12 Index horizontal scroll guide
- Add a clear UI guide near A-Z index indicating horizontal scroll availability.
- Do not use textual hints like `swipe left/right`.
- Show explicit left/right symbol buttons (`<` / `>`) and slide the index strip when tapped.
- Align the left/right buttons vertically with the alphabet chips.
- Use semi-transparent button backgrounds so letters remain visually readable behind/around controls.

### 15.13 Bottom menu for thumb-friendly actions
- Move mode action buttons to lower area of the screen.
- Use an iOS-like bottom menu style for primary mode switching on list screen:
  - List
  - Quiz
  - Matching
  - Memory
- Use intuitive icons instead of letter shortcuts (`L/Q/M/R`).
- Keep labels minimized (icon-first navigation).
- Icon semantics:
  - List: list icon
  - Quiz: check/achievement icon (avoid `?` help-like icon)
  - Matching: swap/matching icon
  - Memory: book/learning icon

### 15.14 Quiz manual-next while auto is paused
- Keep auto-next pause toggle in Quiz.
- When auto-next is paused, user must still be able to progress to next word manually.
- Manual progression policy:
  - Right-to-left swipe moves to next word while paused.
  - Show a clear next indicator icon on the right edge of the word card to communicate swipe availability.
  - No dedicated `Next` button is required in paused mode.
- After answer reveal, right-to-left swipe can also move to next word immediately even when auto mode is running.

### 15.15 Count selector as full-width card list
- Count selection UI (e.g., Quiz/Matching start size) should use full-width card-list layout.
- Each option should be easy to tap and occupy substantial horizontal space.

### 15.16 Quiz detail-return state integrity
- If user opens Detail during Quiz and navigates back, Quiz must resume prior state.
- Do not reset to `No entries` after returning from Detail.

### 15.17 Quiz thumb-friendly control placement
- `Known/Unknown` answer buttons are shown as large buttons in the bottom area for thumb operation.
- Other quiz controls (Auto pause/resume, Detail) are also placed in the lower control area.

### 15.18 Depth-rich visual treatment
- Reduce flat appearance by applying stronger elevation/shadows to buttons, cards, and bottom menu.
- Selected items should appear more raised than unselected items.
- Combine rounded corners with subtle outline/border to improve depth and legibility.

### 15.19 Deck wallpaper (default + user photo)
- Each deck has its own default wallpaper.
- Default wallpapers are bundled scenic images generated to match each deck tone.
- Deck list cards display each deck's wallpaper.
- User can choose a photo per deck and set it as wallpaper.
- Use Android document picker with `image/*` for wallpaper selection.
- Wallpaper choice is persisted per deck and restored after app restart.
- Provide `Reset to default` action to remove user-selected wallpaper.
- Apply wallpaper as screen background for Word list / Quiz / Matching / Memory / Detail.
- Do not apply full-screen wallpaper on Deck list and splash.
- Deck list still shows per-deck wallpaper inside each deck card.
- Render wallpaper as full-screen background with crop scaling to fit device/emulator aspect ratio.
- On wallpaper selection, show info including selected image size (px) and current screen size (dp).
- Deck default backgrounds can be placed in `assets/bg`:
  - Naming: `bg_<deck_id>.(png|jpg|jpeg|webp)`
  - Compatible naming: `default-<deck_hint>.(png|jpg|jpeg|webp)` is also accepted.
  - Example: `bg_mtg_story.jpg`, `bg_toeic_business.webp`
  - Global fallback: `bg_default.(png|jpg|jpeg|webp)`
  - If matching file exists, it overrides drawable default wallpaper.
- Full-screen wallpaper is rendered with `fit-center`, so wide images are scaled down and kept centered.
- Deck-card wallpaper in the deck list uses `center-crop` so the image fits the card area vertically.
- Deck cards in deck-list show word count (`Words: N`) for each deck.
- Deck title text uses a white label background to maintain readability over images.
- Deck description and word-count labels also use white backgrounds for readability.
- Deck card corner radius is reduced to a milder rounded shape.

### 15.20 Sound effect replacement via assets
- Place sound files in `assets/se` to replace quiz/matching feedback sounds.
- Naming:
  - Deck-specific correct: `se_correct_<deck_id>.(wav|mp3|ogg|m4a)`
  - Deck-specific incorrect: `se_incorrect_<deck_id>.(wav|mp3|ogg|m4a)`
  - Shared correct: `se_correct.(wav|mp3|ogg|m4a)`
  - Shared incorrect: `se_incorrect.(wav|mp3|ogg|m4a)`
- Priority:
  1. deck-specific SE
  2. shared SE
  3. built-in tone fallback
- Apply the same SE resolution rules to both Quiz and Matching modes.
- Naming aliases `good/notgood/ok/ng/wrong` are also accepted.

### 15.21 Deck-list BGM
- Play BGM only while the deck-list screen is visible.
- Use `assets/bgm/title-bgm.wav` as the preferred title BGM asset.
- Stop playback when leaving deck-list navigation.
- Pause while app is in background and resume when returning to deck-list.

### 15.22 TOEIC deck scope expansion (700-900)
- Expand TOEIC Business deck to include not only 900-level terms, but also 700-level terms.
- Target learning shape: progressive vocabulary coverage from 700 to 900.
- Tag policy:
  - 700-level: `toeic700` + `frequent`
  - 800-level: `toeic800` + `common`
  - 900-level: `toeic900` + `advanced`
- Quiz category linkage:
  - `Frequent` prioritizes `toeic700/frequent`
  - if 700-level pool is insufficient, fallback to `toeic800/common`
  - `Difficult` prioritizes `toeic900/advanced` plus wrong-answer history.

### 15.23 MTG story web-frequency bulk ingestion
- Expand `MTG Story` deck beyond manual collection by ingesting high-frequency words from official MTG story web articles.
- Source:
  - paginated index and article pages under `https://magic.wizards.com/en/news/magic-story`
- Extraction:
  - tokenize article body text
  - frequency-rank words
  - exclude stopwords and very short tokens
  - skip terms already in deck
- Tags:
  - `mtg`, `story`, `webfreq`
- Meaning bootstrap:
  - try free dictionary API for `pos/meaning`
  - if unavailable, add placeholder meaning for later curation
- Operations:
  - keep a generator script in repository and allow re-run for incremental expansion.
