# Finance Android application

This directory contains the native Kotlin/Jetpack Compose Android version of the uploaded PHP loan system.

## Local storage

The app is offline-first. It does not require PHP, MySQL, a server, or an internet connection. All records are stored on the phone in SQLite database `finance_mobile.db` through `SQLiteOpenHelper`.

Stored data includes:

- Borrowers
- Loans and interest rates
- Payment records and payment dates
- Loan status (`ACTIVE` or `PAID`)

## Current features

- Dashboard showing borrower count, active loans, principal, collected payments, and outstanding balance
- Add borrowers with name, phone, and address
- Add loans with borrower, principal, interest rate, and date
- Record partial payments or pay a loan in full
- Automatic interest and outstanding-balance calculations
- Delete borrowers and loans with dependent records cleaned up
- SQLite foreign keys, indexes, validation, and transaction-safe payment updates
- Database upgrade path for future schema changes
- Demo records automatically created on the first launch when the database is empty

## Open and run

1. Open the repository root in Android Studio.
2. Allow Gradle to sync and install the Android SDK for API 35 if requested.
3. Select the `app` run configuration.
4. Run on an Android emulator or physical device running Android 8.0 (API 26) or newer.

The original `loan_system2.zip` remains in the repository as the legacy PHP source and reference. It is not required by the Android application.

## Important backup note

Because records are stored locally, uninstalling the app or clearing its storage removes the database. Add export/import before using the app for production records or make regular Android device backups.
