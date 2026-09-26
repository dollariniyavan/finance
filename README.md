# Finance Mobile Application

A native Android loan management app converted from the original PHP web system. All data is stored locally on your device using SQLite—no server, PHP backend, or internet connection required.

## Overview

**Finance** is an offline-first mobile app for managing borrowers and loans with automatic interest calculations and payment tracking.

### Key features

- **Borrower management**: Add, view, and manage borrower contacts
- **Loan tracking**: Create loans with principal, interest rate, and start date
- **Payment recording**: Track partial and full loan payments
- **Automatic calculations**: Interest and outstanding balance computed automatically
- **Local storage**: All data stays on your phone via SQLite
- **Status tracking**: Loans marked as `ACTIVE` or `PAID`
- **Demo data**: Sample records pre-loaded on first launch for testing

## Project structure

```
finance/
├── app/                                    # Android app module
│   ├── src/main/
│   │   ├── java/com/dollariniyavan/finance/
│   │   │   ├── MainActivity.kt            # Main UI and Jetpack Compose screens
│   │   │   └── FinanceDb.kt               # SQLite database and data access
│   │   ├── res/values/styles.xml          # Material 3 theme
│   │   └── AndroidManifest.xml            # App manifest
│   └── build.gradle.kts                   # Gradle configuration
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts                    # Gradle settings
├── gradle.properties                      # Gradle properties
├── loan_system2.zip                       # Original PHP source (legacy reference)
├── android/README.md                      # Android app documentation
└── README.md                              # This file
```

## Getting started

### Prerequisites

- Android Studio 2024.1 or later
- JDK 17
- Android SDK API 35 (or install automatically via Android Studio)
- Physical Android device (API 26+) or emulator

### Run the app

1. Clone or open the repository in Android Studio
2. Wait for Gradle to sync dependencies
3. Select the `app` configuration in the run menu
4. Click **Run** to build and launch on your device or emulator

The app will automatically create demo records on the first launch.

## Architecture

### Database (SQLite)

- **borrowers**: Name, phone, address, creation date
- **loans**: Borrower reference, principal, interest rate, start date, status, paid amount
- **loan_payments**: Payment history with amount and timestamp

Features:
- Foreign key constraints
- Database indexes for performance
- Transaction-safe payment recording
- Input validation and constraints
- Automatic status updates

### UI (Jetpack Compose)

Three main tabs:

1. **Overview**: Dashboard showing borrower count, active loans, principal, collected, and outstanding
2. **Borrowers**: Add, view, and delete borrowers
3. **Loans**: Add loans, record payments, and manage loan status

## Data storage

All records are stored in the SQLite database `finance_mobile.db` on your device. The database is located in the app's private storage and is not accessible to other apps.

**Important**: Uninstalling the app or clearing app data will delete the database. Implement export/import or device backup before using with production data.

## Technology stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Database**: SQLite with SQLiteOpenHelper
- **Build System**: Gradle (Kotlin DSL)
- **Min API**: 26 (Android 8.0)
- **Target API**: 35 (Android 15)

## Original source

The `loan_system2.zip` file contains the original PHP web application with MySQL backend. It is retained for reference only and is not used by the Android app.

If you need to migrate data from the PHP system to the Android app, you will need to implement an export/import feature (planned for future updates).

## Future enhancements

- [ ] Edit existing borrowers and loans
- [ ] Search and filter by borrower name
- [ ] Payment history per loan
- [ ] Reports and summaries
- [ ] CSV export/import
- [ ] Dark mode support
- [ ] Cloud backup and sync
- [ ] Multi-currency support

## License

This project is provided as-is. Modify and use freely for your personal finance tracking needs.

## Support

For issues or feature requests, open an issue in this repository.

---

**Version**: 1.0  
**Last updated**: 2026-09-26
