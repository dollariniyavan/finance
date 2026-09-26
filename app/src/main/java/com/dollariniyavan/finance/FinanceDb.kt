package com.dollariniyavan.finance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DATABASE_NAME = "finance_mobile.db"
private const val DATABASE_VERSION = 3

data class Borrower(val id: Long, val name: String, val phone: String, val address: String, val createdAt: Long)
data class Loan(val id: Long, val borrowerId: Long, val borrowerName: String, val principal: Double, val interestRate: Double, val startDate: String, val status: String, val paid: Double)
data class Payment(val id: Long, val loanId: Long, val amount: Double, val paidAt: Long)

class FinanceDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE borrowers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE loans(id INTEGER PRIMARY KEY AUTOINCREMENT, borrower_id INTEGER NOT NULL, principal REAL NOT NULL CHECK(principal > 0), interest_rate REAL NOT NULL DEFAULT 0 CHECK(interest_rate >= 0), start_date TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'ACTIVE', paid REAL NOT NULL DEFAULT 0 CHECK(paid >= 0), FOREIGN KEY(borrower_id) REFERENCES borrowers(id) ON DELETE CASCADE)")
        db.execSQL("CREATE TABLE loan_payments(id INTEGER PRIMARY KEY AUTOINCREMENT, loan_id INTEGER NOT NULL, amount REAL NOT NULL CHECK(amount > 0), paid_at INTEGER NOT NULL, FOREIGN KEY(loan_id) REFERENCES loans(id) ON DELETE CASCADE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS loan_payments(id INTEGER PRIMARY KEY AUTOINCREMENT, loan_id INTEGER NOT NULL, amount REAL NOT NULL, paid_at INTEGER NOT NULL, FOREIGN KEY(loan_id) REFERENCES loans(id) ON DELETE CASCADE)")
        if (oldVersion < 3) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_loans_borrower ON loans(borrower_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_payments_loan ON loan_payments(loan_id)")
        }
    }

    fun seedDemoDataIfEmpty() {
        val empty = readableDatabase.rawQuery("SELECT COUNT(*) FROM borrowers", null).use { it.moveToFirst() && it.getInt(0) == 0 }
        if (!empty) return
        val john = addBorrower("John Doe", "+123456789", "Main Street")
        addLoan(john, 10000.0, 10.0, "2026-09-01")
        val jane = addBorrower("Jane Smith", "+987654321", "Oak Road")
        addLoan(jane, 25000.0, 12.5, "2026-09-10")
    }

    fun borrowers(): List<Borrower> = readableDatabase.rawQuery("SELECT id,name,phone,address,created_at FROM borrowers ORDER BY name", null).use { c ->
        buildList { while (c.moveToNext()) add(Borrower(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4))) }
    }

    fun addBorrower(name: String, phone: String, address: String): Long {
        require(name.isNotBlank()) { "Borrower name is required" }
        return writableDatabase.insertOrThrow("borrowers", null, ContentValues().apply {
            put("name", name.trim()); put("phone", phone.trim()); put("address", address.trim()); put("created_at", System.currentTimeMillis())
        })
    }

    fun updateBorrower(id: Long, name: String, phone: String, address: String): Boolean {
        require(name.isNotBlank()) { "Borrower name is required" }
        return writableDatabase.update("borrowers", ContentValues().apply {
            put("name", name.trim()); put("phone", phone.trim()); put("address", address.trim())
        }, "id = ?", arrayOf(id.toString())) == 1
    }

    fun loans(): List<Loan> = readableDatabase.rawQuery("SELECT l.id,l.borrower_id,b.name,l.principal,l.interest_rate,l.start_date,l.status,l.paid FROM loans l JOIN borrowers b ON b.id=l.borrower_id ORDER BY l.id DESC", null).use { c ->
        buildList { while (c.moveToNext()) add(Loan(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3), c.getDouble(4), c.getString(5), c.getString(6), c.getDouble(7))) }
    }

    fun addLoan(borrowerId: Long, principal: Double, rate: Double, date: String): Long {
        require(principal > 0) { "Principal must be greater than zero" }
        require(rate >= 0) { "Interest rate cannot be negative" }
        return writableDatabase.insertOrThrow("loans", null, ContentValues().apply {
            put("borrower_id", borrowerId); put("principal", principal); put("interest_rate", rate); put("start_date", date); put("status", "ACTIVE"); put("paid", 0.0)
        })
    }

    fun updateLoan(id: Long, principal: Double, rate: Double, date: String): Boolean {
        require(principal > 0 && rate >= 0)
        return writableDatabase.update("loans", ContentValues().apply {
            put("principal", principal); put("interest_rate", rate); put("start_date", date)
        }, "id = ?", arrayOf(id.toString())) == 1
    }

    fun addPayment(loanId: Long, amount: Double) {
        require(amount > 0) { "Payment must be greater than zero" }
        val loan = loanById(loanId) ?: return
        val totalDue = loan.principal + loan.principal * loan.interestRate / 100.0
        val payment = amount.coerceAtMost((totalDue - loan.paid).coerceAtLeast(0.0))
        if (payment <= 0) return
        writableDatabase.beginTransaction()
        try {
            writableDatabase.update("loans", ContentValues().apply { put("paid", loan.paid + payment); put("status", if (loan.paid + payment >= totalDue) "PAID" else "ACTIVE") }, "id = ?", arrayOf(loanId.toString()))
            writableDatabase.insertOrThrow("loan_payments", null, ContentValues().apply { put("loan_id", loanId); put("amount", payment); put("paid_at", System.currentTimeMillis()) })
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    fun payments(loanId: Long): List<Payment> = readableDatabase.rawQuery("SELECT id,loan_id,amount,paid_at FROM loan_payments WHERE loan_id = ? ORDER BY paid_at DESC", arrayOf(loanId.toString())).use { c ->
        buildList { while (c.moveToNext()) add(Payment(c.getLong(0), c.getLong(1), c.getDouble(2), c.getLong(3))) }
    }

    fun totalOutstanding(): Double = readableDatabase.rawQuery("SELECT COALESCE(SUM(principal + principal * interest_rate / 100.0 - paid), 0) FROM loans WHERE status != 'PAID'", null).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }

    fun loanById(id: Long): Loan? = readableDatabase.rawQuery("SELECT l.id,l.borrower_id,b.name,l.principal,l.interest_rate,l.start_date,l.status,l.paid FROM loans l JOIN borrowers b ON b.id=l.borrower_id WHERE l.id = ?", arrayOf(id.toString())).use { c ->
        if (!c.moveToFirst()) null else Loan(c.getLong(0), c.getLong(1), c.getString(2), c.getDouble(3), c.getDouble(4), c.getString(5), c.getString(6), c.getDouble(7))
    }

    fun deleteBorrower(id: Long) { writableDatabase.delete("borrowers", "id = ?", arrayOf(id.toString())) }
    fun deleteLoan(id: Long) { writableDatabase.delete("loans", "id = ?", arrayOf(id.toString())) }
}
