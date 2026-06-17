package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val recurringPaymentDao: RecurringPaymentDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allRecurringPayments: Flow<List<RecurringPayment>> = recurringPaymentDao.getAllRecurringPayments()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertRecurringPayment(payment: RecurringPayment) {
        recurringPaymentDao.insertRecurringPayment(payment)
    }

    suspend fun deleteRecurringPayment(payment: RecurringPayment) {
        recurringPaymentDao.deleteRecurringPayment(payment)
    }

    suspend fun updateRecurringPaymentPaidStatus(id: Int, isPaid: Boolean, month: String) {
        recurringPaymentDao.updatePaidStatus(id, isPaid, month)
    }

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun deleteBudgetByCategory(category: String) {
        budgetDao.deleteBudgetByCategory(category)
    }
}

