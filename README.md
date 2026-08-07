# Soccer Stats Tracker — Android

Youth soccer stat tracking. **Feature complete for a season: teams, rosters, formations, games,
lineups, live match tracking, stats and Excel export.**

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

**Games**

- Schedule per team, split into Upcoming and Played
- Opponent, home/away/neutral, date and time pickers, optional location, minutes per half
- Per-player attendance: Yes / Maybe / No, with a "rest are in" shortcut

**Lineups**

- Pick any formation, tap a position, assign from the available (Yes/Maybe) pool
- Auto-fill matches players to slots by their usual position
- Bench is whoever's available and unassigned
- One lineup covers both the defending and attacking shape

**Live match**

- Two-half clock with kick off / stop / resume and end half
- Event buttons for goals, shots on and off, saves, corners, free kicks, tackles, 50/50s, fouls,
  offsides and cards — each taggable as **us** or **them**
- Our events prompt for the player; opponent events are a single tap
- On-pitch list leads with the **position number** from the formation (1–9), then shirt number and
  name — on the sideline you look for "who's at 6", not for a name
- **Saves are credited to the keeper automatically**, with no prompt; falls back to the normal picker
  in keeperless formats where there's nobody in goal
- Substitutions swap a player into the same slot and close out the other's spell
- Live minutes per player, running timeline, undo on any event

**Two ways to log**

- **Quick buttons** — action, then player. Two taps, for when position doesn't matter.
- **Tap the pitch** — position, then action, then player. Three taps, but records *where* it
  happened. Previously logged events stay on the pitch as markers.
- Goals, shots on target and saves then offer a **goal-mouth view**: tap where it finished. Always
  skippable.

**Stats**

- Per-game breakdown and season totals, toggled on one screen
- Team table: goals, shots, on target, corners, fouls, saves — for and against
- Player table: games, minutes, goals, assists, shots, on target, tackles, 50/50s
- Minutes by position across the season
- **Per-player page**: tap any row for a game-by-game log and a trend chart
- Chart any metric as raw totals or **per 60 minutes played**, with bars per game and a running
  average line
- Keeper metrics (saves, goals conceded, save rate) appear only for players who actually kept goal,
  and are measured against **time in goal** rather than total minutes
- Shot map on the pitch, plus **two goal-mouth charts**: their net for our goals and shots on
  target, ours for goals conceded and keeper saves

**Export**

- One-tap share to a multi-sheet **.xlsx** — Summary, Players, Minutes by position, Events
- Season export adds a Games sheet and every event from every game
- Written to the app cache and shared through a `FileProvider`, so no storage permission is needed

---

## Project layout

```
app/src/main/java/com/bexner/soccerstats/
├── SoccerStatsApplication.kt      Manual DI container (holds the repository)
├── MainActivity.kt                Compose entry point
├── data/
│   ├── entity/                    Team, Player, Position, MatchFormat, Formation,
│   │                              FormationSlot, Game, GameAttendance, LineupSlot,
│   │                              PlayerStint, GameEvent, + relation POJOs
│   ├── dao/                       TeamDao, PlayerDao, FormationDao, GameDao
│   ├── FormationPresets.kt        The 19 built-in shapes
│   ├── DevSeed.kt                 Debug-only real team/roster/systems
│   ├── SoccerDatabase.kt          Room database, converters, migrations
│   └── SoccerRepository.kt        Single data entry point for the UI
├── stats/
│   ├── StatsModels.kt             PlayerStats, TeamTotals, GameStats, SeasonStats
│   │                              and the pure StatsCalculator
│   ├── XlsxWriter.kt              Dependency-free multi-sheet .xlsx writer
│   └── StatsExporter.kt           Workbook assembly + share sheet
└── ui/
    ├── AppViewModelProvider.kt    ViewModel factory
    ├── theme/                     Material 3 pitch-green theme
    ├── navigation/                Routes + NavHost
    ├── components/PitchView.kt    Reusable draggable pitch canvas
    ├── teams/                     TeamListScreen, TeamEditScreen (+ ViewModels)
    ├── roster/                    RosterScreen, PlayerEditScreen (+ ViewModels)
    ├── formations/                FormationListScreen, FormationEditScreen (+ ViewModels)
    ├── games/                     GameList, GameEdit, GameDetail, Attendance,
    │                              Lineup, LiveGame (+ ViewModels)
    └── stats/                     StatsScreen (+ ViewModel)
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
`y = 0.18f`. Anything reading or writing slot positions must respect that.

### Event coordinates

`game_events` carries two optional coordinate pairs, both normalized `0f..1f`:

- **`pitchX` / `pitchY`** — where on the field. Always stored with **your attacking direction
  upward**, regardless of which end you're actually defending, so positions stay comparable across
  halves and across games. Derived third is exposed as `GameEvent.pitchThird`.
- **`goalX` / `goalY`** — where it finished in the net. Normalized against the **goal frame**, not
  the view: `x = 0f` is the left post, `y = 0f` the crossbar. `GameEvent.goalZone` turns that into a
  nine-box name like "Top left".

The nine-box guides drawn in `GoalMouthView` sit at exactly the thresholds `goalZone` uses, so what
you tap and what gets labelled can't drift apart. All four columns are nullable — events logged from
the quick buttons simply have none, and every stat over them must tolerate that.

### Goals conceded

Opponent goals are charged to whoever held the goalkeeper slot **at that match time**, by testing the
goal's clock against each keeper stint's half-open interval `[onAtMs, offAtMs)`. Half-open matters: a
goal landing exactly on a substitution instant is counted once, against the keeper coming on, rather
than by both or neither.

That means a mid-game keeper swap splits concessions correctly, and a keeper who moves outfield isn't
charged for what happens while they're in midfield.

### Rates are folded, not averaged

A season per-60 figure is computed from summed totals — `total goals × 60 / total minutes` — not by
averaging the per-game rates. Averaging rates lets a ten-minute cameo count as much as a full game:
one goal in 10 minutes plus zero in 60 reads as **3.0 per 60** if you average the rates, but **0.86**
folded. The second number is the true one.

### Which net an event belongs to

`GameEvent.goalTarget` decides this in one place, so the screens and the export can't disagree. The
non-obvious case is a save: it's logged with `side = US` because **our** keeper made it, but the ball
was heading into **our** goal — so it belongs on the defending net, not the attacking one. The
mirror case (their keeper saving from us) lands on the attacking net.

| Event | Ours | Theirs |
|---|---|---|
| Goal | Attacking net | Defending net |
| Shot on target | Attacking net | Defending net |
| Save | **Defending** net | **Attacking** net |
| Everything else | — | — |

### Slot indexes are the contract

A lineup binds a player to a **`slotIndex`**, not to a marker label. That is what lets one lineup
serve both shapes: slot 4 must be the same player whether you're looking at the defending or
attacking view. Both phases of a formation therefore have to list the same positions in the same
order — `DevSeed.validate()` enforces it and logs a loud error if a hand-written shape breaks it.

### Match time

Two clocks, deliberately kept apart:

- **Wall clock** — `clockRunningSince` holds the real timestamp of the last start.
- **Match time** — `clockElapsedMs` is running time already banked. Live match time is
  `clockElapsedMs + (now - clockRunningSince)`.

Both live on the `games` row, so locking the phone, rotating, or having the app killed mid-match
loses nothing. Every event timestamp and every substitution is recorded in **match** time, so a long
halftime or an injury stoppage never inflates anyone's minutes.

`player_stints` is what makes minutes computable: one row per continuous spell, holding the player,
the slot, and on/off match times. Substituting closes the outgoing row and opens one for the
replacement at the same instant — so total player minutes always equal *positions × elapsed*, and
minutes can be sliced by position after the fact.

### Why the .xlsx is hand-written

Apache POI is the obvious choice and the wrong one here: it's a large dependency that reaches for
`java.awt` classes Android doesn't ship, and it takes real work to make it behave. An `.xlsx` is just
a zip of XML parts, and the subset needed for plain tables is small — so `XlsxWriter` emits that
subset directly. No dependency, no method-count pressure, a few hundred lines.

Strings are written inline rather than through a shared string table: slightly larger files, far less
to get wrong. Numbers are written as numbers so Excel can sum them, and whole numbers lose the
trailing `.0`. Control characters below `0x20` are stripped, since they're illegal in XML 1.0 and one
stray character would make the entire workbook unopenable.

**Verification approach:** the format was checked by rebuilding a workbook from `XlsxWriter`'s own
XML string constants in Python and opening it with `openpyxl`. That confirmed sheet structure, header
styling, int/float typing, blank cells, and escaping of `&`, `<`, `>` and quotes. It does *not*
prove the Kotlin runs — only CI and your phone can do that.

### Migrations

Database is at **version 5**.

- `MIGRATION_1_2` adds the two formation tables, leaving teams and rosters untouched.
- `MIGRATION_2_3` adds `phase` to `formation_slots`, defaulting existing rows to `DEFENDING` —
  which is what a single-shape formation always meant.
- `MIGRATION_3_4` adds `games`, `game_attendance`, `lineup_slots`, `player_stints` and
  `game_events`. Purely additive.
- `MIGRATION_4_5` adds the four nullable coordinate columns to `game_events`.

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

1. **Stats screens** — season and per-game aggregates: minutes by player and by position, goals,
   shots, duel win rates, plus shot maps and goal-placement charts now that coordinates are being
   captured. All the raw material is already being recorded.
2. **Excel export** — Apache POI or a CSV writer, shared via `FileProvider`. The original point of
   the app.
3. **Calendar sync** — push games to the device calendar.
4. **Live screen polish** — the event grid is functional but untested under real conditions. Expect
   to move buttons around after the first game you actually track from the sideline.

The honest risk in #4 is tap count. Logging an opponent corner is one tap; logging a tackle by one of
yours is two, and a positioned shot with placement is four. Whether that holds up while you're also
coaching is something only a real match will tell us.

Everything through the export is now built. The next real information comes from using it at a game,
not from adding features.
