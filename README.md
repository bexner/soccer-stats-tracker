# Soccer Stats Tracker — Android

Youth soccer stat tracking. **Built so far: teams, rosters, and the formation library.**

Kotlin · Jetpack Compose (Material 3) · Room (local-only, works with no signal) · minSdk 26

---

## Opening the project

1. Open **Android Studio** → *File → Open* → select this folder.
2. Android Studio will prompt to set up the Gradle wrapper the first time (the binary
   `gradle-wrapper.jar` isn't checked in). Accept the prompt, or run `gradle wrapper` if you have
   Gradle installed. Everything else syncs automatically.
3. Let it download the SDK/dependencies, then **Run** on a device or emulator (API 26+).

There is no `local.properties` in the repo — Android Studio writes it with your SDK path on first sync.

---

## What works right now

**Teams**

- Team list with roster size on each card, empty state on first launch
- Create / edit a team: name (required), age group, season
- Delete a team — confirmation dialog, cascades to its players

**Rosters**

- Per-team roster sorted by jersey number, keepers and numbered players first
- Add / edit a player: first name (required), last name, jersey number, primary position
- Duplicate jersey numbers on the same team are rejected with an inline error
- Mark a player active/inactive — inactive players stay on the roster but will be excluded from
  game lineups later; they're dimmed in the list
- Remove a player, with confirmation
- Keyboard **Next** walks down the form; **Save & add another** keeps it open for the next player,
  so a full roster can be typed in one sitting

**Formations**

- Shared library across all your teams, filtered by 4v4 / 7v7 / 9v9 / 11v11
- 19 built-in formations seeded on first launch, including keeperless 4v4 shapes
- Each formation holds **two shapes** — Defending and Attacking — for the same players, toggled in
  the editor. One lineup will later serve both.
- Drag markers on a pitch to build or adjust a shape; tap a marker to change its role or give it a
  custom label like "LB", "CDM", or a shirt number
- "Start from the other shape" copies one phase onto the other so you only move the players who
  actually change
- Free-text transition notes per formation
- Duplicate any formation (editing a built-in one silently forks it, so presets stay intact)
- Presets reappear if you ever delete every formation in the library

---

## Project layout

```
app/src/main/java/com/bexner/soccerstats/
├── SoccerStatsApplication.kt      Manual DI container (holds the repository)
├── MainActivity.kt                Compose entry point
├── data/
│   ├── entity/                    Team, Player, Position, MatchFormat,
│   │                              Formation, FormationSlot, + relation POJOs
│   ├── dao/                       TeamDao, PlayerDao, FormationDao
│   ├── FormationPresets.kt        The 19 built-in shapes
│   ├── DevSeed.kt                 Debug-only real team/roster/systems
│   ├── SoccerDatabase.kt          Room database, converters, migrations
│   └── SoccerRepository.kt        Single data entry point for the UI
└── ui/
    ├── AppViewModelProvider.kt    ViewModel factory
    ├── theme/                     Material 3 pitch-green theme
    ├── navigation/                Routes + NavHost
    ├── components/PitchView.kt    Reusable draggable pitch canvas
    ├── teams/                     TeamListScreen, TeamEditScreen (+ ViewModels)
    ├── roster/                    RosterScreen, PlayerEditScreen (+ ViewModels)
    └── formations/                FormationListScreen, FormationEditScreen (+ ViewModels)
```

**Why it's shaped this way:** screens only ever talk to `SoccerRepository`, never to DAOs. Schedules
and live game stats each become new entities + DAO methods behind that same repository, so none of
the existing screens need to change.

`PitchView` is deliberately generic — it takes normalized markers and reports drags back the same
way. Lineups and the live substitution screen render on that same component rather than each
growing their own pitch.

---

## Data model

**`teams`** — `id`, `name`, `ageGroup`, `season`, `createdAt`

**`players`** — `id`, `teamId` (FK → teams, `ON DELETE CASCADE`), `firstName`, `lastName`,
`jerseyNumber` (nullable), `position`, `isActive`, `createdAt`

**`formations`** — `id`, `name`, `format`, `hasKeeper`, `isPreset`, `notes`, `createdAt`

**`formation_slots`** — `id`, `formationId` (FK → formations, `ON DELETE CASCADE`), `phase`,
`slotIndex`, `role`, `x`, `y`, `label`

`phase` is `DEFENDING` or `ATTACKING`. A formation's slots are the union of both shapes; filter by
phase to get one. `slotIndex` restarts at 0 per phase.

`Position` is an enum stored as text: `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`, `UNASSIGNED`.
`MatchFormat` likewise: `FOUR_V_FOUR`, `SEVEN_V_SEVEN`, `NINE_V_NINE`, `ELEVEN_V_ELEVEN`.

### Pitch coordinates

Slot `x` / `y` are normalized `0f..1f` so a shape renders identically on any screen. **`y = 1f` is
your own goal line and `y = 0f` is the opponent's** — a keeper sits near `y = 0.93f`, strikers near
`y = 0.18f`. Anything reading or writing slot positions must respect that, including lineups later.

### Migrations

Database is at **version 3**.

- `MIGRATION_1_2` adds the two formation tables, leaving teams and rosters untouched.
- `MIGRATION_2_3` adds `phase` to `formation_slots`, defaulting existing rows to `DEFENDING` —
  which is what a single-shape formation always meant.

Each future feature needs a version bump and a migration in `SoccerDatabase.kt`. Note that Room
validates migrations at runtime, not compile time — CI going green does **not** prove a migration is
correct. Test upgrades by installing over an existing build, not just a fresh install.

---

## Seed data

Two separate mechanisms, deliberately:

**`FormationPresets`** — the 19 generic shapes. Ships in every build, inserted when the formation
library is empty, so deleting them all brings them back.

**`DevSeed`** — Blackhawks Bronze (U11), its 11-player roster, and the Base / Aggressive /
Conservative systems. **Debug builds only**, gated on `BuildConfig.DEBUG` in
`SoccerStatsApplication`, so a release build never ships a real roster. It's idempotent — keyed on
the team name — so hand-edits to seeded data survive the next launch instead of being overwritten.

To change the seeded team or systems, edit `DevSeed.kt`. To get a clean slate, uninstall and
reinstall. Both `FormationPresets.validate()` and `DevSeed.validate()` run on debug launch and log
any malformed shape to Logcat under the `SoccerStats` tag.

---

## Next steps, in build order

1. **Lineups** — bind real players to a formation's slots, saved per team so a starting XI can be
   reused week to week. This is the payoff for the formation library.
2. **Schedules** — `Game` entity (opponent, kickoff time, home/away, location) tied to a team, with a
   lineup attached to each game
3. **Live stat tracking** — the big one: an in-game screen with large tap targets for goals, assists,
   shots, saves, fouls, cards, plus substitution and minutes-played timing
4. **Excel export** — Apache POI or a CSV writer, sharing via `FileProvider`

The hardest design decision is #3 — how many taps it takes to record an event while you're actually
coaching. Worth sketching that screen before writing it.
