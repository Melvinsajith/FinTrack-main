package com.priotxroboticsx.fintrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val toAccountId: Int? = null,
    val type: String, // "Income", "Expense", or "Transfer"
    val amount: Double,
    val category: String,
    val date: Date,
    val notes: String?
)
