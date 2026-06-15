# Expense Tracker

A modern, privacy-first Android expense tracker that imports real bank SMS locally and turns them into categorized transactions. Built with Jetpack Compose, Material Design 3, Room, Hilt, and WorkManager.

## Features

### Core

- **Local SMS import** — one-tap import of all bank/UPI transaction SMS already on the device.
- **Real-time auto-import** — a dynamically registered SMS receiver parses new bank messages as they arrive.
- **Transaction list** — all transactions grouped by month, with date headers and income/expense subtotals.
- **Dashboard** — greeting header, tap-to-toggle balance/income/expense card, and interactive chart.

### Organization & Search

- **Categories** — automatic categorization of merchants (food, travel, shopping, health, bills, etc.).
- **Aliases** — rename ugly merchant/UPI names once; applies to all matching transactions.
- **Ignored senders** — skip future imports from specific senders.
- **Transaction filters** — search, filter by type, bank, and period (Home defaults to This Month, All Transactions defaults to All Time).
- **Exclude transactions** — hide transactions from totals without deleting them.
- **Manual notes** — add a personal reason or note to any transaction.

### Visuals

- **Liquid glass sheets** — custom bottom sheets with real backdrop blur, scrim, and top highlight.
- **Material 3 + Compose** — dark theme with violet accent, edge-to-edge, no ripple highlights.
- **Interactive graph** — mode-aware income/expense chart with tappable dates and tooltips.
- **Balance card** — tap to cycle Balance / Income / Expense modes; persisted across launches.
- **Slide navigation** — shared-element-style slide transitions between screens.
- **Pull-to-refresh** on the home screen.

### Notifications

- **New-transaction notifications** — a notification appears when a bank SMS is auto-imported.
- **No repeated notifications** — each transaction is tracked so it only notifies once.
- **Daily summary** — at 20:00, a notification compares today's spending to yesterday.

## Tech Stack

- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Single-activity, MVVM, Compose-first
- **DI:** Hilt 2.59.2
- **Database:** Room 2.8.4
- **Background work:** WorkManager 2.9.1
- **Build:** Gradle 9.2.1 + Android Gradle Plugin 9.0.1

## Permissions

- `READ_SMS` / `RECEIVE_SMS` — import and real-time parsing of bank/UPI SMS.
- `POST_NOTIFICATIONS` — new-transaction and daily-summary notifications.

## Getting Started

1. Open the project in Android Studio
2. Sync Gradle
3. Run on an emulator (API 24+) or device

### Debug build

```bash
./gradlew assembleDebug
```

### Release build

A release keystore is configured to read from `keystore.properties` in the project root. To build a signed release APK:

1. Make sure `expensetracker.keystore` and `keystore.properties` exist in the project root.
2. Run:

```bash
./gradlew assembleRelease
```

The signed APK will be at `app/build/outputs/apk/release/app-release.apk`.

> Never commit `*.keystore`, `*.jks`, or `keystore.properties` to version control.

## Privacy

All transaction parsing and storage happens locally on the device. No data is sent to any cloud or third-party service.

## Screenshots

_(coming soon)_
