# RoomCheck

An Android app for rating how clean and tidy each room of your home is, using
red / yellow / green statuses, tracking it over time, and turning the worst
spots into a to-do list.

## Features

- **Rooms with starter checklists** – Kitchen, Bathroom, Bedroom and Living Room are
  created on first launch. Add more from templates (Dining Room, Home Office, Laundry
  Room, Entryway, Garage, Kids' Room) or start a Custom room.
- **Editable categories and items** – each room has categories such as *Clutter*
  (countertops, floor…) and *Cleanliness* (mirror, toilet, countertop…). Add, rename,
  reorder or delete categories and items, or reset to the default list.
- **Date-stamped assessments** – rate each item red, yellow or green. Shortcuts:
  "All good" per category and "Start from last assessment's ratings". Optional notes.
- **Scores** – red = 0, yellow = 50, green = 100. Room and category scores are the
  average; 75+ is green, 40–74 yellow, under 40 red.
- **Trends** – each room shows an overall trend chart, a sparkline per category, a
  "improving / slipping / steady" indicator and full history.
- **Top priorities** – after saving, items are ranked by severity (red before yellow),
  how much of their category is struggling, and whether they were also a problem last
  time or got worse. The top 3 are pre-selected; add any of them to the to-do list.
- **To-Do list** – grouped by room, check off, delete, clear completed, or add your own.

All data is stored on the device (Room / SQLite). No account or internet needed.

## Building

Requirements: Android Studio (Ladybug or newer) or JDK 17 + Android SDK 35.

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit tests for priorities, trends & templates
```

Or open this folder in Android Studio and press Run.

Every push also builds the APK on GitHub Actions (**Actions → Android build →
RoomCheck-debug-apk** artifact), which you can download and install on a phone.

## Code layout

```
app/src/main/java/com/roomcheck/app/
  domain/   Pure Kotlin: Rating, default templates, priority ranking, trends
  data/     Room database entities, DAOs, Repository
  ui/       Jetpack Compose screens
    rooms/    Home list of rooms
    room/     Room detail: status, trends, history
    assess/   Rate items red / yellow / green
    result/   Results + top priorities → to-do
    edit/     Edit categories & items
    todo/     To-do list
```
