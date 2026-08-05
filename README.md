# Soccer Stats Tracker — Android

Youth soccer stat tracking. **Step 1 of the build: team and roster creation.**

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

---

## Project layout

```
app/src/main/java/com/bexner/soccerstats/
├── SoccerStatsApplication.kt      Manual DI container (holds the repository)
├── MainActivity.kt                Compose entry point
├── data/
│   ├── entity/                    Team, Player, Position, TeamWithPlayerCount
│   ├── dao/                       TeamDao, PlayerDao
│   ├── SoccerDatabase.kt          Room database + type converters
│   └── SoccerRepository.kt        Single data entry point for the UI
└── ui/
    ├── AppViewModelProvider.kt    ViewModel factory
    ├── theme/                     Material 3 pitch-green theme
    ├── navigation/                Routes + NavHost
    ├── teams/                     TeamListScreen, TeamEditScreen (+ ViewModels)
    └── roster/                    RosterScreen, PlayerEditScreen (+ ViewModels)
```

**Why it's shaped this way:** screens only ever talk to `SoccerRepository`, never to DAOs. Schedules,
formations and live game stats each become new entities + DAO methods behind that same repository,
so none of the existing screens need to change.

---

## Data model

**`teams`** — `id`, `name`, `ageGroup`, `season`, `createdAt`

**`players`** — `id`, `teamId` (FK → teams, `ON DELETE CASCADE`), `firstName`, `lastName`,
`jerseyNumber` (nullable), `position`, `isActive`, `createdAt`

`Position` is an enum stored as text: `GOALKEEPER`, `DEFENDER`, `MIDFIELDER`, `FORWARD`, `UNASSIGNED`.

Database is at **version 1**. Adding schedules/formations/stats will need a version bump plus a
migration (or `fallbackToDestructiveMigration()` while you're still developing — worth adding to
`SoccerDatabase` if you don't mind wiping test data between builds).

---

## Next steps, in build order

1. **Schedules** — `Game` entity (opponent, kickoff time, home/away, location) tied to a team
2. **Formations** — formation templates (4-4-2, 3-5-2, …) and per-game lineup assignments
3. **Live stat tracking** — the big one: an in-game screen with large tap targets for goals, assists,
   shots, saves, fouls, cards, plus substitution and minutes-played timing
4. **Excel export** — Apache POI or a CSV writer, sharing via `FileProvider`

The hardest design decision is #3 — how many taps it takes to record an event while you're actually
coaching. Worth sketching that screen before writing it.
