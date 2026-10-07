# Football Match Planner

Built from the lecturer's MobileTech Utility App starter for CP3406 Assessment 1.
The planned purpose is to bring football fixtures together and display kickoff times
in the user's chosen timezone.

## Current progress

- Real current-season Premier League and UEFA Champions League teams and fixtures from [football-data.org](https://www.football-data.org/).
- Retrofit and Gson fetch and parse four requests per load: teams and matches for each competition. No automatic polling.
- FootballRepository maps API timestamps, match statuses and nullable scores to display models.
- FootballViewModel exposes loading, success and retryable error states. The Activity injects its repository through a factory.
- My teams selects current Premier League and Champions League teams; clubs present in both are listed once under Premier League. Following a club includes its matches in both competitions. Team selections persist locally through DataStore and TeamPreferencesViewModel. Cancel discards draft edits.
- Previously followed teams outside current coverage remain available to remove. Their matches are not yet fetched.
- Yesterday, Today and Upcoming filter followed teams' matches. Upcoming includes future scheduled fixtures, grouped and ordered by kickoff.
- Home offers a competition filter and equally sized period controls. Cards show opposing teams, kickoff, status and available scores.
- Filter offers All competitions, Premier League and UEFA Champions League.
- Settings controls 12/24-hour time and timezone (Device, Beijing, Brisbane, London, UTC), and shows source, coverage and update time.
- All date filtering and grouping use the selected timezone. Settings survive configuration changes, but are not permanently stored.
- Free-plan scores may be delayed; schedules can change. Data is fetched when the ViewModel is created, with Retry on failure. There is no offline cache yet.
- Planned later coverage: Serie A, Ligue 1, La Liga, Bundesliga and World Cup. These are not yet integrated.

## API setup

Add your own token to the project-root `local.properties` alongside the existing SDK path:

```properties
FOOTBALL_API_TOKEN=your_token_here
```

Sync Gradle and rebuild. `local.properties` is ignored by Git; do not commit API credentials.
The build injects the token into BuildConfig; a compiled APK contains the credential, so this classroom setup is not suitable for protecting a production secret.
An internet connection and an account with Premier League and Champions League access are required.

## Validation

Run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest`.
Tests cover UTC/local-date conversion, daylight-saving ordering, date filters, saved team restoration, authenticated Retrofit requests, score/status parsing, merged competitions, shared-team selection and denied API access.

## Original starter documentation

This is a basic Android app template for **Assessment 1: Utility App**.
It provides the structure for a simple tabular UI using **Jetpack Compose** and **Material Design 3**.

---

## Getting Started

### How to Run
1. Clone or download this repo  
2. Open in Android Studio  
3. Run on an emulator or physical device (API 26+ recommended)  

---

## Composables

### UtilityApp()
- Contains the screen layout using a Scaffold
- Toggles content between Utility and Settings

### UtilityScreen()
- Displays a simple counter (replace with your utility logic)  
- Includes a button to increment the counter

### SettingsScreen()
- Placeholder for user preferences or configuration  
- Can be extended to modify main screen behavior (e.g., theme, units, limits)  

---

## Key Concepts Covered

| Week | Concept                        | Used In                          |
|------|--------------------------------|----------------------------------|
| 1    | Kotlin + Android Studio         | MainActivity.kt |
| 2    | Jetpack Compose Layouts         | UtilityApp(), UtilityScreen(), SettingsScreen()   |
| 3    | Material Design 3               | MobileTechUtilityAppTheme, MaterialTheme.typography |
| 4    | ViewModel | Not included in starter          |
| 5    | Retrofit  | Not included in starter          |

---

## Suggested Extensions
- Replace counter with a real utility (e.g., hydration tracker, timer)  
- Add a ViewModel for state management  
- Use SharedPreferences or DataStore to persist settings  
- Add a simple API call using Retrofit (e.g., fetch weather or quotes)  

---

## 馃摎 License
This template is provided for educational use in CP3406.  
Feel free to modify and extend it for your assessment.
