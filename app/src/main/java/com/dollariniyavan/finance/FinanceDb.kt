package com.dollariniyavan.finance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Borrower(val id: Long, val name: String, val phone: String, val address: String, val createdAt: Long)
data class Loan(val id: Long, val borrowerId: Long, val borrowerName: String, val principal: Double, val interestRate: Double, val startDate: String, val status: String, val paid: Double)

class FinanceDb(context: Context) : SQLiteOpenHelper(context, "finance.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE borrowers(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, address TEXT, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE loans(id INTEGER PRIMARY KEY AUTOINCREMENT, borrower_id INTEGER NOT NULL, principal REAL NOT NULL, interest_rate REAL NOT NULL, start_date TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'ACTIVE', paid REAL NOT NULL DEFAULT 0, FOREIGN KEY(borrower_id) REFERENCES borrowers(id))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun borrowers(): List<Borrower> = readableDatabase.rawQuery("SELECT id,name,phone,address,created_at FROM borrowers ORDER BY name", null).use { c ->
        buildList { while (c.moveToNext()) add(Borrower(c.getLong(0), c.getString(1), c.getString(2) ?: "", c.getString(3) ?: "", c.getLong(4))) }
    }
    fun addBorrower(name: String, phone: String, address: String) { writableDatabase.execSQL("INSERT INTO borrowers(name,phone,address,created_at) VALUES(?,?,?,?)", arrayOf(name, phone, address, System.currentTimeMillis())) }
    fun loans(): List<Loan> = readableDatabase.rawQuery("SELECT l.id,l.borrower_id,b.name,l.principal,l.interest_rate,l.start_date,l.status,l.paid FROM loans l JOIN borrowers b ON b.id=l.borrower_id ORDER BY l.id DESC", null).use { c ->
        buildList { while (c.moveToNext()) add(Loan(c.getLong(0),c.getLong(1),c.getString(2),c.getDouble(3),c.getDouble(4),c.getString(5),c.getString(6),c.getDouble(7))) }
    }
    fun addLoan(borrowerId: Long, principal: Double, rate: Double, date: String) { writableDatabase.execSQL("INSERT INTO loans(borrower_id,principal,interest_rate,start_date) VALUES(?,?,?,?)", arrayOf(borrowerId, principal, rate, date)) }
    fun addPayment(loanId: Long, amount: Double) { writableDatabase.execSQL("UPDATE loans SET paid=paid+?, status=CASE WHEN paid+? >= principal+(principal*interest_rate/100) THEN 'PAID' ELSE status END WHERE id=?", arrayOf(amount, amount, loanId)) }
}
