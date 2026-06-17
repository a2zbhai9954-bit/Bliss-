package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val description: String,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String, // "Salary", "Food", "Shopping", "Rent", "Utilities", "Entertainment", "Transport", "Other"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_payments")
data class RecurringPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String, // "Rent", "Subscription", "Utilities", "Insurance", "Other"
    val dueDate: Int, // Day of the month (1-31)
    val isPaid: Boolean,
    val lastBilledMonth: String // Format: "YYYY-MM" to track resets across months
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String, // "Food", "Shopping", "Rent", "Utilities", "Entertainment", "Transport", "Other"
    val limitAmount: Double
)

