# Expense Tracker

A modern, privacy-first Android expense tracker that imports real bank SMS locally and turns them into categorized transactions. Built with Jetpack Compose, Material Design 3, Room, Hilt, and WorkManager.

## Features

### Core

- **Local SMS import** — one-tap import of all bank/UPI transaction SMS already on the device.
- **Real-time auto-import** — a manifest-declared SMS receiver parses new bank messages as they arrive, even when the app is closed.
- **Manual transactions** — expandable floating action button to add income or expense entries with title, amount, category, date, and note.
- **Transaction list** — all transactions grouped by month, with date headers and income/expense subtotals.
- **Dashboard** — greeting header, tap-to-toggle balance/income/expense card, and interactive chart.

### Organization & Search

- **Categories** — automatic categorization of merchants (food, travel, shopping, health, bills, grocery, etc.).
- **Category rules** — set a category once for a merchant title; all matching transactions (existing and future) are auto-assigned.
- **Aliases** — rename ugly merchant/UPI names once; applies to all matching transactions.
- **Ignored senders** — skip future imports from specific senders.
- **Transaction filters** — search, filter by type, bank, and period (Home defaults to This Month, All Transactions defaults to All Time).
- **Exclude transactions** — hide transactions from totals without deleting them.
- **Manual notes** — add a personal reason or note to any transaction; shown in transaction lists.
- **Promotional SMS filtering** — non-transaction SMS (cashback offers, approval alerts, promo links) are automatically skipped during import.

### Reminders

- **Payment reminders** — set reminders for upcoming subscription or bill payments with custom date and time.
- **Exact-time alarms** — uses `AlarmManager.setAlarmClock()` for precise delivery, even in Doze mode.
- **Smart notification content** — shows "Due today", "Due tomorrow", or "Due on Jun 20" with amount and merchant name.
- **Tap-to-open** — tapping a reminder notification opens the app directly to that transaction's detail card.
- **Auto-reschedule** — all reminders are rescheduled on app startup, surviving device reboots.

### Visuals

- **Liquid glass sheets** — custom bottom sheets with animated backdrop blur, scrim, and top highlight.
- **Material 3 + Compose** — dark theme with violet accent, edge-to-edge, no ripple highlights on any tappable element.
- **Interactive graph** — swipeable pager with Trend (line chart) and Category (donut chart) pages; tap donut arcs to highlight categories.
- **Balance card** — tap to cycle Balance / Income / Expense modes; persisted across launches.
- **Expandable FAB** — gradient transparent floating button that expands to show Income and Expense actions with staggered spring animation.
- **Custom calendar** — glass-themed date picker for transaction dates and reminders.
- **Wheel time picker** — draggable scrollable time picker with 3 visible rows, snap-on-release, and gradient fade.
- **Slide navigation** — shared-element-style slide transitions between screens.
- **Pull-to-refresh** on the home screen.

### Notifications

- **New-transaction notifications** — a notification appears when a bank SMS is auto-imported.
- **No repeated notifications** — each transaction is tracked so it only notifies once.
- **Daily summary** — at 20:00, a notification compares today's spending to yesterday.
- **Payment reminders** — high-priority notifications at the exact scheduled time with tappable action.

## Tech Stack

- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Single-activity, MVVM, Compose-first
- **DI:** Hilt 2.59.2
- **Database:** Room 2.8.4 (DB version 8)
- **Background work:** WorkManager 2.9.1
- **Alarms:** AlarmManager (`setAlarmClock`) for exact-time reminder delivery
- **Build:** Gradle 9.2.1 + Android Gradle Plugin 9.0.1

## Permissions

- `READ_SMS` / `RECEIVE_SMS` — import and real-time parsing of bank/UPI SMS.
- `POST_NOTIFICATIONS` — new-transaction, daily-summary, and payment-reminder notifications.
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — schedule exact-time payment reminder alarms.

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
