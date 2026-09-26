package com.dollariniyavan.finance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "finance_mobile.db"
private const val DATABASE_VERSION = 2

data class Borrower(
    val id: Long,
    val name: String,
    val phone: String,
    val address: String,
    val createdAt: Long
)

data class Loan(
    val id: Long,
    val borrowerId: Long,
    val borrowerName: String,
    val principal: Double,
    val interestRate: Double,
    val startDate: String,
    val status: String,
    val paid: Double
)

class FinanceDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE borrowers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT,
                address TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                borrower_id INTEGER NOT NULL,
                principal REAL NOT NULL,
                interest_rate REAL NOT NULL,
                start_date TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                paid REAL NOT NULL DEFAULT 0,
                FOREIGN KEY (borrower_id) REFERENCES borrowers(id)
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE loan_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id INTEGER NOT NULL,
                amount REAL NOT NULL,
                paid_at INTEGER NOT NULL,
                FOREIGN KEY (loan_id) REFERENCES loans(id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS loan_payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    loan_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    paid_at INTEGER NOT NULL,
                    FOREIGN KEY (loan_id) REFERENCES loans(id)
                )
                """.trimIndent()
            )
        }
    }

    fun seedDemoDataIfEmpty() {
        val borrowersCount = readableDatabase.rawQuery("SELECT COUNT(*) FROM borrowers", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

        if (borrowersCount > 0) return

        val borrowerId = insertBorrower("John Doe", "+123456789", "Main Street")
        addLoan(borrowerId, 10000.0, 10.0, "2026-09-01")
        val secondBorrowerId = insertBorrower("Jane Smith", "+987654321", "Oak Road")
        addLoan(secondBorrowerId, 25000.0, 12.5, "2026-09-10")
    }

    fun borrowers(): List<Borrower> {
        val list = mutableListOf<Borrower>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, name, phone, address, created_at FROM borrowers ORDER BY name ASC",
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list += Borrower(
                    id = c.getLong(0),
                    name = c.getString(1),
                    phone = c.getString(2) ?: "",
                    address = c.getString(3) ?: "",
                    createdAt = c.getLong(4)
                )
            }
        }
        return list
    }

    fun addBorrower(name: String, phone: String, address: String): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("phone", phone)
            put("address", address)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("borrowers", null, values)
    }

    fun insertBorrower(name: String, phone: String, address: String): Long = addBorrower(name, phone, address)

    fun loans(): List<Loan> {
        val list = mutableListOf<Loan>()
        val cursor = readableDatabase.rawQuery(
            """
            SELECT l.id, l.borrower_id, b.name, l.principal, l.interest_rate, l.start_date, l.status, l.paid
            FROM loans l
            INNER JOIN borrowers b ON b.id = l.borrower_id
            ORDER BY l.id DESC
            """.trimIndent(),
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list += Loan(
                    id = c.getLong(0),
                    borrowerId = c.getLong(1),
                    borrowerName = c.getString(2),
                    principal = c.getDouble(3),
                    interestRate = c.getDouble(4),
                    startDate = c.getString(5),
                    status = c.getString(6),
                    paid = c.getDouble(7)
                )
            }
        }
        return list
    }

    fun addLoan(borrowerId: Long, principal: Double, rate: Double, date: String): Long {
        val values = ContentValues().apply {
            put("borrower_id", borrowerId)
            put("principal", principal)
            put("interest_rate", rate)
            put("start_date", date)
            put("status", "ACTIVE")
            put("paid", 0.0)
        }
        return writableDatabase.insert("loans", null, values)
    }

    fun addPayment(loanId: Long, amount: Double) {
        val loan = loanById(loanId) ?: return
        val totalDue = loan.principal + (loan.principal * loan.interestRate / 100.0)
        val newPaid = (loan.paid + amount).coerceAtMost(totalDue)
        val newStatus = if (newPaid >= totalDue) "PAID" else "ACTIVE"

        val values = ContentValues().apply {
            put("paid", newPaid)
            put("status", newStatus)
        }

        writableDatabase.update("loans", values, "id = ?", arrayOf(loanId.toString()))

        val paymentValues = ContentValues().apply {
            put("loan_id", loanId)
            put("amount", amount)
            put("paid_at", System.currentTimeMillis())
        }
        writableDatabase.insert("loan_payments", null, paymentValues)
    }

    fun totalOutstanding(): Double {
        val cursor = readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(principal + (principal * interest_rate / 100.0) - paid), 0) FROM loans WHERE status != 'PAID'",
            null
        )
        return cursor.use { c ->
            if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    fun loanById(loanId: Long): Loan? {
        val cursor = readableDatabase.rawQuery(
            """
            SELECT l.id, l.borrower_id, b.name, l.principal, l.interest_rate, l.start_date, l.status, l.paid
            FROM loans l
            INNER JOIN borrowers b ON b.id = l.borrower_id
            WHERE l.id = ?
            """.trimIndent(),
            arrayOf(loanId.toString())
        )
        return cursor.use { c ->
            if (c.moveToFirst()) {
                Loan(
                    id = c.getLong(0),
                    borrowerId = c.getLong(1),
                    borrowerName = c.getString(2),
                    principal = c.getDouble(3),
                    interestRate = c.getDouble(4),
                    startDate = c.getString(5),
                    status = c.getString(6),
                    paid = c.getDouble(7)
                )
            } else null
        }
    }

    fun deleteBorrower(borrowerId: Long) {
        writableDatabase.delete("loans", "borrower_id = ?", arrayOf(borrowerId.toString()))
        writableDatabase.delete("borrowers", "id = ?", arrayOf(borrowerId.toString()))
    }

    fun deleteLoan(loanId: Long) {
        writableDatabase.delete("loan_payments", "loan_id = ?", arrayOf(loanId.toString()))
        writableDatabase.delete("loans", "id = ?", arrayOf(loanId.toString()))
    }
}
