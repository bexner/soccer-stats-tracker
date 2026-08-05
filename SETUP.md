# Setup — getting this building

Two open items from the initial build, and how each gets closed:

| Issue | Fix |
|---|---|
| I can't compile here (no Android SDK in my sandbox, Google downloads blocked) | GitHub Actions compiles every push on runners that *do* have the SDK. I read the results. |
| `gradle-wrapper.jar` isn't included (it's a binary I can't generate) | Android Studio regenerates it on first open — and CI also builds one and hands it back to you. |

Work through the steps in order. Steps 1–3 take about 30 minutes, most of it downloads.

---

## 1. Move the project out of OneDrive

Gradle writes thousands of files into `build/` and `.gradle/` on every compile. OneDrive tries to sync
all of them, which causes "file in use" build failures and burns bandwidth for no benefit.

Open **PowerShell** and run:

```powershell
New-Item -ItemType Directory -Force -Path C:\dev
Move-Item "$env:USERPROFILE\OneDrive\Documents\Claude\Projects\Soccer Stats Tracker" "C:\dev\SoccerStatsTracker"
```

Then tell me `C:\dev\SoccerStatsTracker` and I'll reconnect to it — I keep full access to the project,
it just isn't sitting in OneDrive anymore.

> The folder name has no spaces on purpose. Spaces in Android project paths occasionally trip up
> NDK and older Gradle plugins. Not worth the risk.

---

## 2. Install Android Studio

Download from **https://developer.android.com/studio** (free, ~1 GB).

Run the installer and accept the defaults. On first launch a setup wizard appears:

1. Choose **Standard** installation
2. Pick a UI theme
3. Review the components list — it installs the Android SDK, an emulator system image, and the
   platform tools. Accept the licenses.
4. Let it download (~3–4 GB). This is the long part.

The bundled JDK is fine — you don't need to install Java separately.

---

## 3. Open the project and generate the wrapper

1. Android Studio → **File → Open** → `C:\dev\SoccerStatsTracker`
2. It will notice `gradle-wrapper.jar` is missing and show a prompt about the Gradle wrapper or the
   Gradle JDK. Choose the option to **use the Gradle wrapper** — Studio creates the missing jar,
   plus `gradlew` and `gradlew.bat`, automatically.
3. If no prompt appears, go to **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**,
   set **Use Gradle from: `'gradle-wrapper.properties' file`**, click OK, then **File → Sync Project with Gradle Files**.
4. Wait for the sync. First run downloads all dependencies — several minutes.

If the sync fails, don't debug it alone. Go to step 6.

**Running it:** click the green ▶ button. To use a physical phone, enable Developer Options
(Settings → About phone → tap Build number seven times) then USB debugging, and plug it in. Otherwise
Studio's Device Manager can create an emulator.

---

## 4. Put it on GitHub

Android Studio bundles Git, but the command line is clearer here. If `git` isn't recognized in
PowerShell, install it from https://git-scm.com/download/win first.

Create an empty repo at **https://github.com/new** — name it `soccer-stats-tracker`, leave
"Add a README" **unchecked**. Then:

```powershell
cd C:\dev\SoccerStatsTracker
git init -b main
git add .
git commit -m "Team and roster creation"
git remote add origin https://github.com/YOUR-USERNAME/soccer-stats-tracker.git
git push -u origin main
```

Private repos get free GitHub Actions minutes too, so either visibility works.

---

## 5. Let CI do the compiling

`.github/workflows/android.yml` is already in the project. The moment you push, GitHub spins up a
Linux runner that has the Android SDK preinstalled and:

- installs Gradle 8.9 (no wrapper jar needed, so this works even before step 3)
- generates a wrapper and **uploads `gradle-wrapper.jar`, `gradlew` and `gradlew.bat` as an artifact** —
  download that zip from the run page, drop the files into the project, and commit them
- compiles the app and **uploads an installable debug APK**
- runs Android Lint and uploads the HTML report

Watch it under the **Actions** tab of your repo. A red X means a compile error; click into the failed
step to see it.

This is the durable fix for issue #1 — every change I make from here gets compiled by a real Android
toolchain within a couple of minutes, instead of relying on my static checks.

---

## 6. Sending me errors

Whichever way the build breaks, I want the raw output, not a summary.

**From CI:** open the failed run under the Actions tab, expand the failed step, click the ⋯ menu →
**View raw logs**, and paste the relevant chunk to me. Or just tell me the run failed and I'll pull it
from the public repo.

**From Android Studio:** open the **Build** tool window (bottom of the screen), click the
"Build Output" tab, right-click → Copy, paste to me.

**Best of all — write it to a file I can read directly.** Once the wrapper exists:

```powershell
cd C:\dev\SoccerStatsTracker
.\gradlew.bat assembleDebug --stacktrace > build-log.txt 2>&1
```

Then just say "check build-log.txt" and I'll read it straight out of the project folder. No copy-paste,
full stack traces, nothing truncated. This is the fastest loop for the two of us.

---

## What I expect might break on first sync

Being honest about where my static checks are weakest — all of these are quick fixes:

- **Dependency version drift.** I pinned AGP 8.5.2 / Kotlin 2.0.20 / Compose BOM 2024.09.02. If any
  aren't resolvable, Gradle says so plainly and I bump them.
- **Compose API signatures.** Things like `AssistChipDefaults.assistChipColors(disabledLabelColor = ...)`
  are the sort of named parameter that shifts between Material 3 releases.
- **Room's annotation processor.** It validates queries at compile time — my `TeamWithPlayerCount`
  subquery is the most likely thing it complains about.
- **The launcher icon.** It's a hand-written vector path. It will build, but it may simply look wrong.
  Easy to replace with Android Studio's Image Asset tool.
