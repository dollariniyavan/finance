# Finance Android application

This directory is a native Kotlin/Jetpack Compose Android version of the uploaded PHP loan system.

## Storage

The app uses an on-device SQLite database (`finance.db`) through `SQLiteOpenHelper`. No PHP server, MySQL server, login, or internet connection is required. Borrowers, loans, interest rates, payment totals, and statuses remain on the phone.

## Included features

- Dashboard totals for borrowers, active loans, and outstanding balance
- Local borrower creation
- Local loan creation with principal, interest rate, and start date
- Marking a loan fully paid
- Automatic interest and outstanding-balance calculations

Open the repository in Android Studio and run the `app` configuration on an Android device or emulator. The original `loan_system2.zip` is retained as the legacy web source.
