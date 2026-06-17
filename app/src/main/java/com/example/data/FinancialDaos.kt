package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}

@Dao
interface RecurringPaymentDao {
    @Query("SELECT * FROM recurring_payments ORDER BY dueDate ASC")
    fun getAllRecurringPayments(): Flow<List<RecurringPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringPayment(payment: RecurringPayment)

    @Delete
    suspend fun deleteRecurringPayment(payment: RecurringPayment)

    @Query("UPDATE recurring_payments SET isPaid = :isPaid, lastBilledMonth = :month WHERE id = :id")
    suspend fun updatePaidStatus(id: Int, isPaid: Boolean, month: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)
}

