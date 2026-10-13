# Football Match Planner

A football utility app developed from the lecturer's **MobileTech Utility App** starter for **CP3406 Assessment 1**.

The app helps football fans see when their favourite teams play across different competitions, with kickoff times and dates converted to their chosen timezone. Home presents essential schedule information at a glance; Settings controls its display. The app retains the starter's single Activity and bottom navigation.

## Features

### Home and My teams

- Follow teams through expandable competition lists. A club appearing in more than one list shares the same selection, and its matches are included across all loaded competitions.
- Save followed teams locally with DataStore. Save applies draft selections; Cancel discards them. Previously saved teams outside current coverage can still be removed.
- Switch between **Yesterday**, **Today** and **Upcoming**. Yesterday and Today use calendar dates in the selected timezone. Upcoming shows future fixtures with scheduled or confirmed kickoff status.
- Browse matches grouped by local date and then competition. Dates and competitions follow kickoff order; matches within each competition are sorted by kickoff.
- View competition emblems, team crests and names, kickoff times or available scores, and match status. Unconfirmed kickoff times display **Time TBC**.
- Open the glass **My teams** menu for a competition overview and an **Edit teams** action. Its menu icon animates into a close icon; outside taps and Android Back dismiss it.
- See loading, API-wait, retryable error, first-time selection and empty-schedule states, with actions to recover or select teams. Settings remains accessible when schedule or saved-team loading fails.

### Settings

- Choose **12-hour or 24-hour** kickoff display.
- Choose **Phone timezone**, **Beijing**, **Brisbane**, **London** or **UTC**. Date filtering and grouping use this selection as well as displayed times.
- Toggle **Light/Dark** Material appearance with an animated sun/moon icon, or return to the device setting. The green backdrop and glass-panel styling remain consistent in both modes.
- View the data source, competition coverage, followed teams, active timezone and schedule update timestamp.

Theme, time-format and timezone selections use `rememberSaveable` to survive supported state restoration, including configuration changes. They are not permanently stored. Followed teams are stored separately in DataStore and restored after app restarts.

## Competition coverage

Teams and current-season fixtures are fetched from [football-data.org](https://www.football-data.org/).

| Competition | API code |
| --- | --- |
| Premier League | `PL` |
| Serie A | `SA` |
| Ligue 1 | `FL1` |
| La Liga | `PD` |
| Bundesliga | `BL1` |
| UEFA Champions League | `CL` |

The configured API account must have access to all six competitions. Coverage is limited to the competitions and fixtures returned by these endpoints.

## Running the app

1. Clone or download this repository. Open the **project root containing settings.gradle.kts** in Android Studio, rather than the app folder. Android Studio currently displays the project as **MobileTech_UtilityApp**, its original template name.
2. Use an Android Studio installation compatible with the project's build configuration. Install **Android SDK Platform 36**. The minimum runtime version is **Android 8.0 / API 26**.
3. Allow Gradle sync to resolve dependencies and the **JetBrains JDK 21** requested by `gradle/gradle-daemon-jvm.properties`. The wrapper uses **Gradle 9.6.0**; the project declares **Android Gradle Plugin 9.4.1** and **Kotlin 2.2.10**. These are the checked-in project versions.
4. Create an API account with the required competition access and obtain a token. In the project-root `local.properties`, preserve Android Studio's `sdk.dir` entry and add:

   ```properties
   FOOTBALL_API_TOKEN=your_token_here
   ```

5. Sync Gradle and build. Select the **app** run configuration and an emulator or connected Android device, then Run.
6. On the first successful load, choose **Choose my teams**, expand a competition, select teams and tap **Done**. Use **My teams > Edit teams** to change selections later.

The installed launcher label is currently **MobileTech Utility App**; the Home heading is **Football Schedule**.

`local.properties` is ignored by Git. Each developer or marker supplies their own SDK path and API token. The token is injected into `BuildConfig` at build time, so changing it requires a rebuild. The compiled app contains the token; this classroom configuration does not protect a production credential.

## Implementation and project structure

The app uses **Kotlin, Jetpack Compose and Material 3**, with **Retrofit/Gson** for API requests, **DataStore** for followed teams and **Coil** for PNG/SVG badges.

Production Kotlin files are under `app/src/main/java/au/edu/jcu/assessment/utilityapp/`.

| File or directory | Responsibility |
| --- | --- |
| `MainActivity.kt` | Creates dependencies and ViewModel factories, collects state with collectAsStateWithLifecycle, and hosts UtilityApp with bottom navigation and shared display preferences. |
| `ui/HomeScreen.kt` | Shows followed-team fixtures, switches Yesterday/Today/Upcoming, and coordinates the My teams menu and selection dialog. |
| `ui/FixtureComponents.kt` | Reusable date headings, competition headings and match rows with team badges, names, kickoff times or scores. |
| `ui/MyTeamsDialog.kt` | Edits a draft team selection grouped by competition; Done saves it and Cancel leaves the current selection unchanged. |
| `ui/SettingsScreen.kt` | Theme controls, time format, timezone and schedule information. Receives preferences and change callbacks from UtilityApp. |
| `ui/FootballViewModel.kt` | Loads the schedule in viewModelScope and exposes loading, success, quota-wait and error states through StateFlow. Prevents overlapping load jobs. |
| `data/FootballRepository.kt` | Loads teams and matches for each competition, maps API data to display models, and coordinates caching and quota recovery. |
| `data/remote/FootballApi.kt` | Retrofit endpoints, response DTOs and the authenticated API client. |
| `data/FixtureSchedule.kt` | Match/status models, selected-period filtering and local-date grouping. Kickoffs are stored as absolute Instant values. |
| `data/FootballResponseCache.kt` | In-memory and disk response caching, timestamps and expiry checks. |
| `ui/TeamPreferencesViewModel.kt` | Observes saved teams and manages save/loading errors through StateFlow. |
| `data/TeamPreferencesRepository.kt` | Reads and writes followed teams in Preferences DataStore. |
| `ui/GlassPanel.kt`, `ui/TeamsMenuPanel.kt` | Shared glass surfaces and the expanding My teams menu. |
| `ui/FootballBadge.kt` | Loads public badge images through Coil, with initials as a loading/error fallback. Image requests do not use the authenticated football API client. |
| `ui/MorphingTeamsIcon.kt`, `ui/MorphingThemeIcon.kt` | Native Compose Canvas icon animations. |
| `ui/ScheduleStatus.kt` | Reusable loading, error, onboarding and empty-state presentation. |
| `ui/theme/` | Material colour schemes, typography and theme-mode definitions. |

Dependencies are supplied through constructors and ViewModel factories: **manual dependency injection**, rather than a DI framework. Composables receive state and callbacks; ViewModels coordinate work, and repositories handle network/storage operations.

## Cache, refresh and error handling

- A fully uncached load needs **12 successful API calls**: one teams request and one matches request for each competition. Badge downloads are separate from these API calls.
- Team responses are cached for **24 hours** and match responses for **3 hours** in the app's private files directory. Fresh responses are reused after process restarts.
- Each successful endpoint is saved immediately. If a later request fails, a retry reuses responses already cached instead of repeating the whole load.
- HTTP **429** waits for Retry-After, or **65 seconds** when no usable header is present, then retries the failed endpoint once. The UI explains the wait. Repeated throttling shows a Retry action.
- Missing credentials, denied access, network failures and unreadable responses produce explanatory error states.
- There is **no automatic polling or periodic background refresh**. Cache expiry is checked when the repository loads again, such as after a new app process starts or a failed load is retried. Simply switching Home/Settings does not refresh it.
- Settings shows the **oldest match-response timestamp** across loaded competitions, including reused cached responses. This is a fetch timestamp, not the time the provider last changed a fixture.

## UI and accessibility implementation

Home uses a continuous glass schedule panel, equal-width period controls, competition headings and compact match rows. The green gradient, glass tint and expanding menu were informed by the supplied Apple Sports references.

The implementation includes labelled controls, selected/expanded state semantics, date and competition headings, and live announcements for status messages. Badge descriptions are omitted where adjacent text already identifies the team or competition. Settings rows and main icon controls use at least 48 dp touch areas. Settings, the schedule and status content support scrolling.

On Android 12 and later, glass backdrops use blur; older supported devices retain translucent surfaces. Backdrop and foreground content are drawn separately so text is not blurred. These choices do not establish that every layout or accessibility scenario has been verified.

## Validation

From the project root in PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Use `./gradlew` for the equivalent command on macOS or Linux.

Tests in `app/src/test/java/au/edu/jcu/assessment/utilityapp/data/` cover:

- Local-date period filtering, timezone changes and kickoff ordering at daylight-saving transitions.
- Saved-team restoration and clearing.
- Authenticated Retrofit requests, match status/score mapping, combined competition data and shared-team selection.
- Denied access, quota retry, partial-progress recovery and reuse of cached responses.
- Disk-cache restoration, original timestamps, expiration and corrupt entries.

The network tests use MockWebServer and local fixtures, not a real API token or quota. These unit tests do not verify every visual layout or live-provider response.

`app/src/debug/java/au/edu/jcu/assessment/utilityapp/ui/ScheduleStatePreviews.kt` provides interactive Compose previews for loading/quota wait, connection errors, first-time team selection, empty schedules and saved-team errors. They use local data, make no football API calls and are excluded from release builds.

## Known limitations

- Scores and schedules reflect the fetched snapshot. Free-plan scores may be delayed, kickoff times can change, and there are no live score updates.
- Expired cache entries are not displayed as an offline fallback. A failed required endpoint prevents completion of the full schedule load, although successful responses remain cached.
- Date labels are calculated when Home recomposes; there is no midnight timer to update an untouched screen automatically.
- Team selections are matched by the provider's display names rather than stable team IDs. Provider renaming can therefore affect previously saved selections.
- The interface and date formatting are currently English. Match-detail screens, reminders and calendar export are outside the current implementation.

## Coursework and references

| Course topic | Implementation |
| --- | --- |
| Week 1: Kotlin and Android Studio | Kotlin Android project based on the lecturer's starter. |
| Week 2: Compose layouts | Home/Settings composables, rows, columns, state and user callbacks. |
| Week 3: Material Design 3 | Material components, theme definitions, scrollable lists and accessibility semantics. |
| Week 4: App architecture | ViewModels, repositories, StateFlow, lifecycle-aware collection and manual DI. |
| Week 5: Web APIs | Retrofit/Gson integration with the football data provider. |

- **Starter:** the lecturer-provided MobileTech Utility App template. Its educational-use notice is retained here: provided for CP3406 and permitted to be modified and extended for the assessment.
- **Football data and badge URLs:** [football-data.org](https://www.football-data.org/).
- **Visual references:** user-supplied Apple Sports screenshots/video, used to inform the gradient, glass surfaces, schedule grouping and team-menu interaction.
- **Animation reference:** [Morphicons](https://github.com/guillermolg00/morphicons). The icons are native Compose renditions, not the React package. The menu morph follows smooth spring stiffness 170/damping 26; the sun/moon morph follows snappy stiffness 420/damping 30.

Reference colours were sampled as 5 x 5 pixel medians from the supplied screenshot: top background `#396832` at (744, 140), bottom `#182A15` at (539, 1515), light button `#4B8148` at (1105, 232), and glass tint `#1C2B19` at (780, 1520). The page gradient reaches its bottom colour at 34% of screen height, keeping the lower two thirds deep green.
