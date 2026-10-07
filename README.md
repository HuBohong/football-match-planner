# Football Match Planner

Built from the lecturer's MobileTech Utility App starter for CP3406 Assessment 1.
The planned purpose is to bring football fixtures together and display kickoff times
in the user's chosen timezone.

## Current progress

- The Utility screen displays three fictional fixtures in a scrollable list of Material 3 cards.
- Each card shows the competition, teams, and a sample kickoff time.
- Cards show time and competition above two equally sized team columns with a central VS label.
- Today, Tomorrow, and +2 days chips filter fixtures by date; a message appears on empty days.
- The default Upcoming view groups demo fixtures under date headings, ordered by date and sample time.
- Date headings include the weekday and Today/Tomorrow labels and are marked as accessibility headings.
- Demo dates are relative to the device's current date (two fixtures today, one tomorrow).
- A Filter button on the schedule opens a competition dialog (All competitions, Premier League, La Liga, Champions League).
- Selecting an option immediately updates the schedule and closes the dialog; Close, Back, or outside dismissal leaves the selection unchanged.
- The filter works together with the date filter and survives tab switches and configuration changes.
- My teams opens a multi-select dialog with six sample teams grouped by domestic league.
- Done applies the selected teams; Cancel, Back, or outside dismissal discards draft changes.
- Fixtures match when either the home or away team is followed, combined with date and competition filters.
- With no followed teams, the home screen shows a Choose my teams prompt instead of the fixture list.
- Completing team selection opens the schedule; cancelling keeps the current view. Removing every followed team returns to the prompt.
- Followed teams are stored locally with Preferences DataStore and restored when the app restarts.
- TeamPreferencesRepository handles disk reads/writes; TeamPreferencesViewModel exposes loading, saving, and error state to Compose.
- The Activity provides the repository through a ViewModel factory (manual dependency injection); loading does not briefly show the first-use prompt.
- Radio options support tapping the whole row, with a minimum 48dp height and grouped selection semantics.
- Settings is a placeholder for future timezone and time format preferences.
- Real API data and timezone conversion are not implemented yet. Competition and date filters are not stored permanently.

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

## 📚 License
This template is provided for educational use in CP3406.  
Feel free to modify and extend it for your assessment.
