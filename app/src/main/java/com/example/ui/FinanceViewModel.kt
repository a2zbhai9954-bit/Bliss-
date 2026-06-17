package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.network.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(
        database.transactionDao(),
        database.recurringPaymentDao(),
        database.budgetDao()
    )

    // Flow lists
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringPayments: StateFlow<List<RecurringPayment>> = repository.allRecurringPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // UI state
    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Automatically check and reset monthly payment flags if we enter a new month
        viewModelScope.launch {
            recurringPayments.collect { payments ->
                val currentMonth = getCurrentYearMonth()
                payments.forEach { payment ->
                    if (payment.lastBilledMonth != currentMonth) {
                        // Reset isPaid flag on new month
                        repository.insertRecurringPayment(
                            payment.copy(isPaid = false, lastBilledMonth = currentMonth)
                        )
                    }
                }
            }
        }
    }

    private fun getCurrentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Calendar.getInstance().time)
    }

    fun getCurrentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    // Transactions API
    fun addTransaction(amount: Double, description: String, type: String, category: String) {
        viewModelScope.launch {
            if (amount <= 0) {
                _errorMessage.value = "Amount must be greater than zero."
                return@launch
            }
            if (description.isBlank()) {
                _errorMessage.value = "Description cannot be empty."
                return@launch
            }
            val txn = Transaction(
                amount = amount,
                description = description,
                type = type,
                category = category
            )
            repository.insertTransaction(txn)
            _errorMessage.value = null
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Recurring Payments API
    fun addRecurringPayment(title: String, amount: Double, category: String, dueDate: Int) {
        viewModelScope.launch {
            if (amount <= 0) {
                _errorMessage.value = "Amount must be greater than zero."
                return@launch
            }
            if (title.isBlank()) {
                _errorMessage.value = "Title cannot be empty."
                return@launch
            }
            if (dueDate !in 1..31) {
                _errorMessage.value = "Due Date must be between 1 and 31."
                return@launch
            }
            val payment = RecurringPayment(
                title = title,
                amount = amount,
                category = category,
                dueDate = dueDate,
                isPaid = false,
                lastBilledMonth = getCurrentYearMonth()
            )
            repository.insertRecurringPayment(payment)
            _errorMessage.value = null
        }
    }

    fun togglePaymentPaid(payment: RecurringPayment) {
        viewModelScope.launch {
            repository.updateRecurringPaymentPaidStatus(
                id = payment.id,
                isPaid = !payment.isPaid,
                month = getCurrentYearMonth()
            )
        }
    }

    fun deleteRecurringPayment(payment: RecurringPayment) {
        viewModelScope.launch {
            repository.deleteRecurringPayment(payment)
        }
    }

    // Budget Methods
    fun setBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            if (limitAmount <= 0) {
                _errorMessage.value = "Budget limit must be greater than zero."
                return@launch
            }
            if (category.isBlank()) {
                _errorMessage.value = "Please choose a valid category."
                return@launch
            }
            val b = Budget(category = category, limitAmount = limitAmount)
            repository.insertBudget(b)
            _errorMessage.value = null
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(category)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Metrics compilation & Local heuristics Fallback
    fun compileFinancialMetrics(
        txns: List<Transaction>,
        bills: List<RecurringPayment>
    ): FinancialMetrics {
        val totalIncome = txns.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = txns.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense

        val categorySpending = txns.filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val totalBills = bills.sumOf { it.amount }
        val paidBills = bills.filter { it.isPaid }.sumOf { it.amount }
        val unpaidBills = bills.filter { !it.isPaid }.sumOf { it.amount }

        val today = getCurrentDayOfMonth()
        val overdueBillsCount = bills.count { !it.isPaid && it.dueDate < today }

        // Local Discipline Logic
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100 else 0.0

        var disciplineScore = 80 // Base default score
        val deductions = mutableListOf<String>()

        if (savingsRate < 0) {
            disciplineScore -= 30
            deductions.add("Overspending (saving rate is negative: ${String.format("%.1f", savingsRate)}%)")
        } else if (savingsRate < 15.0) {
            disciplineScore -= 15
            deductions.add("Low saving rate (${String.format("%.1f", savingsRate)}%). Target is at least 20%.")
        } else {
            disciplineScore += 10
        }

        if (overdueBillsCount > 0) {
            disciplineScore -= (overdueBillsCount * 12)
            deductions.add("Have $overdueBillsCount overdue recurring bills unpaid.")
        }

        // Category overspend check: Dining/Shopping > 30% of income
        val shoppingMealSpend = (categorySpending["Shopping"] ?: 0.0) + (categorySpending["Food"] ?: 0.0)
        if (totalIncome > 0 && (shoppingMealSpend / totalIncome) > 0.35) {
            disciplineScore -= 10
            deductions.add("Discretionary spending (Shopping & Food) is over 35% of income.")
        }

        disciplineScore = disciplineScore.coerceIn(0, 100)

        // Compile advice tips
        val tips = mutableListOf<String>()
        if (savingsRate < 0) {
            tips.add("Immediately halt non-essential shopping. Build an emergency buffer of 3 months expenses.")
        }
        if (overdueBillsCount > 0) {
            tips.add("Prioritize unpaid recurring bills immediately. Overdue bills damage credit scores and trigger late fees.")
        }
        if (totalIncome > 0 && spendingRatio(totalExpense, totalIncome) > 0.85) {
            tips.add("Review subscriptions and automated bills. Keep mandatory living costs below 50% of your net income.")
        }
        if (tips.isEmpty()) {
            tips.add("You are maintaining exceptional discipline! Consider moving extra savings into mutual funds or index bonds.")
            tips.add("Keep up the diligent tracking habits to unlock deeper long-term compounded wealth.")
        }

        return FinancialMetrics(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            categorySpending = categorySpending,
            totalBills = totalBills,
            paidBills = paidBills,
            unpaidBills = unpaidBills,
            overdueBillsCount = overdueBillsCount,
            savingsRate = savingsRate,
            disciplineScore = disciplineScore,
            deductions = deductions,
            recommendedActionTips = tips
        )
    }

    private fun spendingRatio(expense: Double, income: Double): Double = expense / income

    fun generateAiReport(txns: List<Transaction>, bills: List<RecurringPayment>) {
        _isGeneratingReport.value = true
        _aiReport.value = null

        viewModelScope.launch {
            val metrics = compileFinancialMetrics(txns, bills)
            val currentDay = getCurrentDayOfMonth()

            // Build detailed prompt for Gemini
            val prompt = """
                You are an elite, direct, and encouraging Financial Discipline AI Advisor.
                Analyze the user's monthly ledger and render a financial situation and discipline report card.
                
                MONTHLY FINANCIAL SNAPSHOT:
                - Net Income: $${metrics.totalIncome}
                - Total Expenditures: $${metrics.totalExpense}
                - Net Monthly Balance: $${metrics.netBalance}
                - Saving Rate: ${String.format("%.1f", metrics.savingsRate)}%
                
                SPENDING BY CATEGORY:
                ${metrics.categorySpending.entries.joinToString("\n") { "  * ${it.key}: $${it.value}" }}
                
                RECURRING MONTHLY BILLS STATUS (Day of Month is $currentDay):
                - Total Monthly Bills: $${metrics.totalBills}
                - Paid: $${metrics.paidBills}
                - Unpaid Outstanding Bills: $${metrics.unpaidBills}
                - Overdue Bills: ${metrics.overdueBillsCount} 
                
                Please structure your report exactly with these sections (using Markdown format):
                
                ## Financial Discipline Score
                Give a strict numeric score from 0 to 100 (e.g., **Discipline Score: X/100**) based on savings balance, unpaid bills, and spending allocations. Explain briefly what this score means.
                
                ## Situation Assessment
                Explain the current financial health. Are they overleveraged? Are subscription traps dragging down liquidity? Be specific, realistic, and direct.
                
                ## Discipline Report Card
                Rate their spending behavior (A to F) on areas like Categorical Outlays and Recurring bills compliance. Mention major warning zones if any.
                
                ## Recommended Action Steps
                Provide exactly 3 bullet points with bold structural headings of direct, tactical actions they can execute this week to optimize status.
                
                Keep your tone professional, motivational, and extremely direct. Avoid high-level generic advice. Speak only about the actual data provided.
            """.trimIndent()

            val aiResult = GeminiClient.generateDisciplinedAdvice(prompt)
            if (aiResult != null) {
                _aiReport.value = aiResult
            } else {
                // If Gemini client returns null (API key missing or net issue), we build an exceptional local report!
                _aiReport.value = buildLocalHeuristicsReport(metrics)
            }
            _isGeneratingReport.value = false
        }
    }

    private fun buildLocalHeuristicsReport(metrics: FinancialMetrics): String {
        return buildString {
            append("## Financial Discipline Score\n")
            append("**Discipline Score: ${metrics.disciplineScore}/100**\n\n")
            if (metrics.disciplineScore >= 80) {
                append("Excellent! You show top-tier financial control. High saving rate and on-time monthly payments demonstrate strong future wealth security.")
            } else if (metrics.disciplineScore >= 50) {
                append("Moderate. Your financial discipline is stable but there are leakages. Overspending on entertainment/shopping or delayed bill payments is preventing optimal wealth compounding.")
            } else {
                append("Critical Attention Required! Low or negative saving rates combined with overdue recurring bills represent active risk. Immediate intervention is required.")
            }
            append("\n\n")

            append("## Situation Assessment\n")
            append("Your total income is **$${String.format("%.2f", metrics.totalIncome)}** compared to expenditures of **$${String.format("%.2f", metrics.totalExpense)}**. ")
            append("This results in a net balance of **$${String.format("%.2f", metrics.netBalance)}** ")
            append("and an activesaving rate of **${String.format("%.1f", metrics.savingsRate)}%**. ")
            append("A safe saving target is 20%+ of net monthly income. ")
            if (metrics.unpaidBills > 0) {
                append("You have **$${String.format("%.2f", metrics.unpaidBills)}** in outstanding monthly bills yet to be toggled, with **${metrics.overdueBillsCount}** items currently overdue. ")
            } else {
                append("All registered recurring bills for the current cycle are fully paid! This is stellar and ensures zero late fees. ")
            }
            if (metrics.deductions.isNotEmpty()) {
                append("\n\n**Discipline Warnings detected:**\n")
                metrics.deductions.forEach { warning ->
                    append("- $warning\n")
                }
            }
            append("\n\n")

            append("## Discipline Report Card\n")
            append("- **Saving & Liquidity Index:** ${if (metrics.savingsRate >= 20) "Grade A" else if (metrics.savingsRate >= 10) "Grade B" else if (metrics.savingsRate >= 0) "Grade C" else "Grade F"}\n")
            append("- **Bill Payment Compliance:** ${if (metrics.overdueBillsCount == 0) "Grade A (100% on-time)" else "Grade D (Overdue items present)"}\n")
            append("- **Discretionary Outlays Control:** ${if ((metrics.categorySpending["Shopping"] ?: 0.0) + (metrics.categorySpending["Food"] ?: 0.0) < metrics.totalIncome * 0.25) "Grade A" else "Grade C"}\n")
            append("\n\n")

            append("## Recommended Action Steps\n")
            metrics.recommendedActionTips.forEachIndexed { idx, tip ->
                val boldHeading = when (idx) {
                    0 -> "**Address Core Leak**: "
                    1 -> "**Billing Hygiene**: "
                    else -> "**Compound Optimization**: "
                }
                append("- $boldHeading$tip\n")
            }
            append("\n*Note: Showing locally parsed heuristics. Configure a Gemini API Key in the AI Studio Secrets panel to activate full AI feedback.*")
        }
    }
}

data class FinancialMetrics(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val categorySpending: Map<String, Double>,
    val totalBills: Double,
    val paidBills: Double,
    val unpaidBills: Double,
    val overdueBillsCount: Int,
    val savingsRate: Double,
    val disciplineScore: Int,
    val deductions: List<String>,
    val recommendedActionTips: List<String>
)
