package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Budget
import com.example.data.RecurringPayment
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

// Theme Aliases for Clean Minimalism (Globally Scoped to this file)
private val cleanBg = Color(0xFFFEF7FF)
private val textDark = Color(0xFF1D1B20)
private val textSecondary = Color(0xFF49454F)
private val primaryPurple = Color(0xFF6750A4)
private val deepPurpleText = Color(0xFF21005D)
private val cardLavender = Color(0xFFEADDFF)
private val activePillLavender = Color(0xFFE8DEF8)
private val navBarBgLight = Color(0xFFF3EDF7)
private val borderLight = Color(0x4DCAC4D0) // #CAC4D0 with 30% alpha
private val incomeGreen = Color(0xFF1B6F1F)
private val expenseRed = Color(0xFFB3261E)
private val whiteCardBg = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboard(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val txns by viewModel.transactions.collectAsStateWithLifecycle()
    val bills by viewModel.recurringPayments.collectAsStateWithLifecycle()
    val aiReportText by viewModel.aiReport.collectAsStateWithLifecycle()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsStateWithLifecycle()
    val errorText by viewModel.errorMessage.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Transactions, 2: Monthly Bills, 3: Discipline AI

    val metrics = remember(txns, bills) { viewModel.compileFinancialMetrics(txns, bills) }

    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }

    // Palette custom gradients
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(cleanBg, Color(0xFFF3EDF7))
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(cardLavender, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = deepPurpleText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Good morning,",
                                fontSize = 11.sp,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 12.sp
                            )
                            Text(
                                text = "Alex Johnson",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textDark,
                                lineHeight = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = cleanBg
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (activeTab == 2) {
                                showAddBillDialog = true
                            } else {
                                showAddTxDialog = true
                            }
                        },
                        modifier = Modifier
                            .testTag("add_accent_fab")
                            .minimumInteractiveComponentSize()
                    ) {
                        val icon = if (activeTab == 2) Icons.Default.ReceiptLong else Icons.Default.Add
                        Icon(
                            imageVector = icon,
                            contentDescription = "Quick Add",
                            tint = primaryPurple
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = borderLight, thickness = 1.dp)
                NavigationBar(
                    containerColor = navBarBgLight,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    val tabs = listOf(
                        NavigationTabItem("Overview", Icons.Default.Dashboard, Icons.Outlined.Dashboard, "tab_overview"),
                        NavigationTabItem("Ledger", Icons.Default.History, Icons.Outlined.History, "tab_ledger"),
                        NavigationTabItem("Bills", Icons.Default.CalendarToday, Icons.Outlined.CalendarToday, "tab_bills"),
                        NavigationTabItem("AI Report", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_discipline")
                    )
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF1D192B),
                                selectedTextColor = Color(0xFF1D192B),
                                indicatorColor = activePillLavender,
                                unselectedIconColor = textSecondary,
                                unselectedTextColor = textSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab != 3) {
                FloatingActionButton(
                    onClick = {
                        if (activeTab == 2) {
                            showAddBillDialog = true
                        } else {
                            showAddTxDialog = true
                        }
                    },
                    containerColor = primaryPurple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("floating_action_btn")
                ) {
                    val icon = if (activeTab == 2) Icons.Default.ReceiptLong else Icons.Default.Add
                    Icon(icon, contentDescription = "Add Entry")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Error Snackbar Banner
            AnimatedVisibility(
                visible = errorText != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(10f)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = expenseRed),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorText ?: "",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // Main Tab Router
            when (activeTab) {
                0 -> DashboardTab(
                    metrics = metrics,
                    txns = txns,
                    bills = bills,
                    budgets = budgets,
                    onSetBudget = { category, limit -> viewModel.setBudget(category, limit) },
                    onDeleteBudget = { category -> viewModel.deleteBudget(category) }
                )
                1 -> LedgerTab(txns, onDelete = { viewModel.deleteTransaction(it) })
                2 -> MonthlyBillsTab(
                    bills,
                    today = viewModel.getCurrentDayOfMonth(),
                    onPaidToggle = { viewModel.togglePaymentPaid(it) },
                    onDelete = { viewModel.deleteRecurringPayment(it) }
                )
                3 -> AIReportsTab(
                    viewModel,
                    txns,
                    bills,
                    metrics,
                    aiReportText,
                    isGeneratingReport
                )
            }
        }
    }

    // Interactive Dialog: Add Transaction
    if (showAddTxDialog) {
        var amountStr by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("EXPENSE") } // "INCOME" or "EXPENSE"
        var selectedCategory by remember { mutableStateOf("Food") }

        val categories = if (type == "INCOME") {
            listOf("Salary", "Investment", "Refund", "Gifts", "Other")
        } else {
            listOf("Food", "Groceries", "Shopping", "Rent", "Utilities", "Entertainment", "Transport", "Other")
        }

        AlertDialog(
            onDismissRequest = { showAddTxDialog = false },
            title = { Text("Record Transaction", color = textDark, fontWeight = FontWeight.Bold) },
            containerColor = whiteCardBg,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                type = "INCOME"
                                selectedCategory = "Salary"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "INCOME") Color(0xFFD2EBD4) else Color(0xFFE7E0EC)
                            ),
                            modifier = Modifier.weight(1f).testTag("select_income_btn")
                        ) {
                            Text("Income", color = if (type == "INCOME") incomeGreen else textSecondary)
                        }
                        Button(
                            onClick = {
                                type = "EXPENSE"
                                selectedCategory = "Food"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "EXPENSE") Color(0xFFF9DEDC) else Color(0xFFE7E0EC)
                            ),
                            modifier = Modifier.weight(1f).testTag("select_expense_btn")
                        ) {
                            Text("Expense", color = if (type == "EXPENSE") expenseRed else textSecondary)
                        }
                    }

                    // Amount Text Field
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryPurple,
                            focusedLabelColor = primaryPurple,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_amount_input")
                    )

                    // Description Text Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryPurple,
                            focusedLabelColor = primaryPurple,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_desc_input")
                    )

                    // Category Selector Label
                    Text("Select Category", color = textSecondary, fontSize = 12.sp)

                    // LazyRow of categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cardLavender,
                                    selectedLabelColor = deepPurpleText,
                                    containerColor = Color(0xFFE7E0EC),
                                    labelColor = textSecondary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        viewModel.addTransaction(amt, description, type, selectedCategory)
                        showAddTxDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_tx")
                ) {
                    Text("Save", color = primaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTxDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    // Interactive Dialog: Add Recurring Payment (Bill)
    if (showAddBillDialog) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var limitDayStr by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("Rent") }

        val billCategories = listOf("Rent", "Subscription", "Utilities", "Insurance", "Other")

        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text("Add Monthly Recurring Bill", color = textDark, fontWeight = FontWeight.Bold) },
            containerColor = whiteCardBg,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bill Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Bill Name (e.g. rent, Netflix)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryPurple,
                            focusedLabelColor = primaryPurple,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bill_title_input")
                    )

                    // Bill Amount
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Monthly Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryPurple,
                            focusedLabelColor = primaryPurple,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bill_amount_input")
                    )

                    // Bill Limit Due Day
                    OutlinedTextField(
                        value = limitDayStr,
                        onValueChange = { limitDayStr = it },
                        label = { Text("Due Day of Month (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryPurple,
                            focusedLabelColor = primaryPurple,
                            unfocusedBorderColor = borderLight,
                            focusedTextColor = textDark,
                            unfocusedTextColor = textDark
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bill_day_input")
                    )

                    // Category Selector Label
                    Text("Bill Category", color = textSecondary, fontSize = 12.sp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        billCategories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cardLavender,
                                    selectedLabelColor = deepPurpleText,
                                    containerColor = Color(0xFFE7E0EC),
                                    labelColor = textSecondary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        val day = limitDayStr.toIntOrNull() ?: 1
                        viewModel.addRecurringPayment(title, amt, selectedCategory, day)
                        showAddBillDialog = false
                    },
                    modifier = Modifier.testTag("confirm_add_bill")
                ) {
                    Text("Add Bill", color = primaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

// Data class representation of the tabs
data class NavigationTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

// Helper structure for Category Icons mapping
fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Salary" -> Icons.Default.Payments
        "Investment" -> Icons.Default.TrendingUp
        "Refund" -> Icons.Default.Undo
        "Gifts" -> Icons.Default.Redeem
        "Food" -> Icons.Default.Restaurant
        "Groceries" -> Icons.Default.LocalGroceryStore
        "Shopping" -> Icons.Default.LocalMall
        "Rent" -> Icons.Default.Home
        "Utilities" -> Icons.Default.Bolt
        "Entertainment" -> Icons.Default.ConfirmationNumber
        "Transport" -> Icons.Default.DirectionsCar
        "Subscription" -> Icons.Default.Subscriptions
        "Insurance" -> Icons.Default.Shield
        else -> Icons.Default.HelpOutline
    }
}

// Tab 0: OVERVIEW / DASHBOARD
@Composable
fun DashboardTab(
    metrics: FinancialMetrics,
    txns: List<Transaction>,
    bills: List<RecurringPayment>,
    budgets: List<Budget>,
    onSetBudget: (String, Double) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    var showSetBudgetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balances Header Grid
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
            modifier = Modifier.fillMaxWidth().testTag("balance_overview_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL BALANCE", 
                    color = Color(0xFF21005D).copy(alpha = 0.8f), 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold, 
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${String.format("%,.2f", metrics.netBalance)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF21005D),
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF21005D).copy(alpha = 0.15f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF1B6F1F), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Total Income", color = Color(0xFF21005D).copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = "$${String.format("%,.2f", metrics.totalIncome)}",
                            color = Color(0xFF1B6F1F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFB3261E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Total Outlays", color = Color(0xFF21005D).copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = "$${String.format("%,.2f", metrics.totalExpense)}",
                            color = Color(0xFFB3261E),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // SECTION: CATEGORIAL BUDGETS & LIMITS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MONTHLY BUDGET COMPLIANCE",
                style = MaterialTheme.typography.titleMedium,
                color = textDark,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = { showSetBudgetDialog = true },
                modifier = Modifier.testTag("add_budget_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = primaryPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Set Limit", color = primaryPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Alerts computation
        val budgetAlerts = remember(budgets, metrics.categorySpending) {
            budgets.mapNotNull { budget ->
                val spent = metrics.categorySpending[budget.category] ?: 0.0
                val pct = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
                if (pct >= 0.8) {
                    val overflow = spent - budget.limitAmount
                    val message = if (pct > 1.0) {
                        "Exceeded! Spent $${String.format("%,.2f", spent)} on ${budget.category}, which is $${String.format("%,.2f", overflow)} over limit ($${String.format("%,.0f", budget.limitAmount)})."
                    } else {
                        "Nearing limit! Spent $${String.format("%,.2f", spent)} of $${String.format("%,.0f", budget.limitAmount)} limit on ${budget.category} (${String.format("%.0f", pct * 100)}%)."
                    }
                    val isExceeded = pct > 1.0
                    Triple(budget.category, message, isExceeded)
                } else {
                    null
                }
            }
        }

        if (budgetAlerts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                budgetAlerts.forEach { (cat, text, isExceeded) ->
                    val bg = if (isExceeded) Color(0xFFF9DEDC) else Color(0xFFFFF4E5)
                    val stroke = if (isExceeded) Color(0xFFF2B8B5) else Color(0xFFFFCC80)
                    val iconTint = if (isExceeded) expenseRed else Color(0xFFB57000)
                    val labelText = if (isExceeded) "CRITICAL BUDGET BREACH" else "APPROACHING BUDGET THRESHOLD"
                    
                    OutlinedCard(
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(1.dp, stroke),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("budget_alert_$cat")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isExceeded) Icons.Default.Warning else Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = labelText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = iconTint,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    color = textDark,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (budgets.isEmpty()) {
            OutlinedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                border = BorderStroke(1.dp, borderLight),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No category budgets defined",
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Set up monthly thresholds for discretionary expenses to prevent savings leakage.",
                        color = textSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showSetBudgetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("set_budget_empty_btn")
                    ) {
                        Text("Define Category Limit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                budgets.forEach { budget ->
                    val spent = metrics.categorySpending[budget.category] ?: 0.0
                    val pct = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
                    
                    val isExceeded = pct > 1.0
                    val isNearing = pct >= 0.8 && pct <= 1.0
                    
                    val progressColor = if (isExceeded) {
                        expenseRed
                    } else if (isNearing) {
                        Color(0xFFB57000)
                    } else {
                        primaryPurple
                    }
                    
                    OutlinedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                        border = BorderStroke(1.dp, borderLight),
                        modifier = Modifier.fillMaxWidth().testTag("budget_card_${budget.category}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(progressColor.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(budget.category),
                                    contentDescription = null,
                                    tint = progressColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = budget.category,
                                        color = textDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    IconButton(
                                        onClick = { onDeleteBudget(budget.category) },
                                        modifier = Modifier.size(24.dp).testTag("delete_budget_${budget.category}").minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Budget",
                                            tint = textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$${String.format("%,.2f", spent)} spent of $${String.format("%,.0f", budget.limitAmount)}",
                                        color = textSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format("%.0f", pct * 100)}%",
                                        color = progressColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { pct.toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = progressColor,
                                    trackColor = Color(0xFFE7E0EC)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSetBudgetDialog) {
            SetBudgetDialog(
                onDismiss = { showSetBudgetDialog = false },
                onSave = { category, limit ->
                    onSetBudget(category, limit)
                    showSetBudgetDialog = false
                }
            )
        }

        // Section: Spending Distribution & Native Canvas Chart
        Text("SPENDING CHANNELS", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold)


        if (metrics.categorySpending.isEmpty()) {
            OutlinedCard(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                border = BorderStroke(1.dp, borderLight),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No outlays recorded for this cycle.",
                        color = textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use the quick '+' button to add expenditures.",
                        color = textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            OutlinedCard(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                border = BorderStroke(1.dp, borderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DonutChart(metrics.categorySpending)

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0x1ACAC4D0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Spend channel items
                    metrics.categorySpending.forEach { (cat, amt) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    getCategoryIcon(cat),
                                    contentDescription = null,
                                    tint = getChartColorForCategory(cat),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, color = textDark, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$${String.format("%,.2f", amt)}", color = textDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val pct = (amt / metrics.totalExpense) * 100
                                Text("${String.format("%.1f", pct)}%", color = textSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Budget flow chart
        Text("LIQUIDITY BALANCE BAR FLUX", style = MaterialTheme.typography.titleMedium, color = textDark, fontWeight = FontWeight.Bold)
        OutlinedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = whiteCardBg),
            border = BorderStroke(1.dp, borderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                ComparisonBarChart(metrics.totalIncome, metrics.totalExpense)
            }
        }

        // Quick overview of upcoming bills
        Text("ACTIVE SYSTEM BILL HYGIENE", style = MaterialTheme.typography.titleMedium, color = textDark, fontWeight = FontWeight.Bold)
        if (bills.isEmpty()) {
            OutlinedCard(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                border = BorderStroke(1.dp, borderLight),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(
                    "No recurring payments configured. Set up rent or utilities in the Bills tab.",
                    color = textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        } else {
            bills.take(3).forEach { bill ->
                OutlinedCard(
                    colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, borderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (bill.isPaid) incomeGreen.copy(alpha = 0.12f) else expenseRed.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (bill.isPaid) incomeGreen else expenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(bill.title, color = textDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Due Day: ${bill.dueDate} | ${bill.category}", color = textSecondary, fontSize = 11.sp)
                            }
                        }
                        Text(
                            "$${String.format("%,.2f", bill.amount)}",
                            color = if (bill.isPaid) textSecondary else textDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Custom Native Draw Block: Donut Chart
@Composable
fun DonutChart(categorySpending: Map<String, Double>) {
    val entries = categorySpending.toList().sortedByDescending { it.second }
    val total = entries.sumOf { it.second }

    Box(
        modifier = Modifier
            .size(180.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = 270f
            entries.forEach { (cat, amt) ->
                val sweep = ((amt / total) * 360f).toFloat()
                drawArc(
                    color = getChartColorForCategory(cat),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOTAL OUT", color = Color(0xFF49454F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "$${String.format("%,.0f", total)}",
                color = Color(0xFF1D1B20),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

fun getChartColorForCategory(category: String): Color {
    return when (category) {
        "Food" -> Color(0xFFFB923C) // orange
        "Groceries" -> Color(0xFF4ADE80) // bright light green
        "Shopping" -> Color(0xFFF472B6) // pink
        "Rent" -> Color(0xFF6750A4) // elegant purple (CleanPrimary)
        "Utilities" -> Color(0xFFFBBF24) // gold
        "Entertainment" -> Color(0xFFA78BFA) // light purple
        "Transport" -> Color(0xFF34D399) // mint
        "Salary" -> Color(0xFF1B6F1F) // CleanIncomeGreen
        "Investment" -> Color(0xFF14B8A6) // teal
        "Refund" -> Color(0xFF38BDF8) // light blue
        "Gifts" -> Color(0xFFEC4899) // rose
        else -> Color(0xFF9CA3AF) // gray
    }
}

// Custom Native Draw Block: Liquidity Comparison Bar Chart
@Composable
fun ComparisonBarChart(income: Double, expenses: Double) {
    val total = if (income + expenses > 0) income + expenses else 1.0
    val incomePct = (income / total).toFloat()
    val expensePct = (expenses / total).toFloat()

    // Smooth entry transition sizing
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationTriggered = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Income Flow", color = Color(0xFF49454F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(
                    modifier = Modifier
                        .height(140.dp)
                        .width(42.dp)
                ) {
                    val actualHeight = size.height * incomePct * animatedProgress
                    drawRoundRect(
                        color = Color(0xFF1B6F1F),
                        topLeft = Offset(0f, size.height - actualHeight),
                        size = Size(size.width, actualHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("$${String.format("%,.0f", income)}", color = Color(0xFF1B6F1F), fontWeight = FontWeight.Black, fontSize = 13.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Expenditures", color = Color(0xFF49454F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(
                    modifier = Modifier
                        .height(140.dp)
                        .width(42.dp)
                ) {
                    val actualHeight = size.height * expensePct * animatedProgress
                    drawRoundRect(
                        color = Color(0xFFB3261E),
                        topLeft = Offset(0f, size.height - actualHeight),
                        size = Size(size.width, actualHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("$${String.format("%,.0f", expenses)}", color = Color(0xFFB3261E), fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }
    }
}

// Tab 1: SEARCHABLE HISTORICAL TRANSACTIONS LEDGER
@Composable
fun LedgerTab(
    txns: List<Transaction>,
    onDelete: (Transaction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE

    val filteredTxns = remember(txns, searchQuery, selectedTypeFilter) {
        txns.filter { txn ->
            val matchesSearch = txn.description.contains(searchQuery, ignoreCase = true) ||
                    txn.category.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedTypeFilter) {
                "INCOME" -> txn.type == "INCOME"
                "EXPENSE" -> txn.type == "EXPENSE"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search transactions...", color = Color(0xFF49454F), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF49454F)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6750A4),
                unfocusedBorderColor = Color(0x4DCAC4D0),
                focusedTextColor = Color(0xFF1D1B20),
                unfocusedTextColor = Color(0xFF1D1B20)
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("ledger_search_bar")
        )

        // Type Filter Chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "INCOME", "EXPENSE").forEach { filter ->
                val isSelected = selectedTypeFilter == filter
                val selectedBg = if (filter == "INCOME") Color(0xFFD2EBD4) else if (filter == "EXPENSE") Color(0xFFF9DEDC) else Color(0xFFE8DEF8)
                val selectedLabel = if (filter == "INCOME") Color(0xFF1B6F1F) else if (filter == "EXPENSE") Color(0xFFB3261E) else Color(0xFF1D192B)
                
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTypeFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0x4DCAC4D0),
                        selectedBorderColor = Color.Transparent
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedBg,
                        selectedLabelColor = selectedLabel,
                        containerColor = Color(0xFFFFFFFF),
                        labelColor = Color(0xFF49454F)
                    ),
                    modifier = Modifier.weight(1f).testTag("filter_chip_$filter")
                )
            }
        }

        if (filteredTxns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MoneyOff, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No transactions match current filters.", color = Color(0xFF49454F))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTxns, key = { it.id }) { txn ->
                    TransactionItemRow(txn, onDeleteClick = { onDelete(txn) })
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    txn: Transaction,
    onDeleteClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = remember(txn.timestamp) { sdf.format(Date(txn.timestamp)) }

    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x4DCAC4D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (txn.type == "INCOME") Color(0xFF1B6F1F).copy(alpha = 0.12f) else Color(0xFFB3261E).copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(txn.category),
                    contentDescription = null,
                    tint = if (txn.type == "INCOME") Color(0xFF1B6F1F) else Color(0xFFB3261E),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.description,
                    color = Color(0xFF1D1B20),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$formattedDate | ${txn.category}",
                    color = Color(0xFF49454F),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val prefix = if (txn.type == "INCOME") "+" else "-"
            val amountColor = if (txn.type == "INCOME") Color(0xFF1B6F1F) else Color(0xFFB3261E)
            Text(
                text = "$prefix$${String.format("%.2f", txn.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp).minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Tab 2: MONTHLY BILL CHANNELS (RECURRING PAYMENTS)
@Composable
fun MonthlyBillsTab(
    bills: List<RecurringPayment>,
    today: Int,
    onPaidToggle: (RecurringPayment) -> Unit,
    onDelete: (RecurringPayment) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Recurring Payment Resets", color = Color(0xFF21005D), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "All bill flags will reset automatically back to unpaid on the start of any new calendar month.",
                        color = Color(0xFF21005D).copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF49454F), modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No recurring payments found.", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Click the '+' button above to add a bill like Rent or Netflix.", color = Color(0xFF49454F), fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bills, key = { it.id }) { bill ->
                    BillItemRow(bill, today, onPaidToggle, onDeleteClick = { onDelete(bill) })
                }
            }
        }
    }
}

@Composable
fun BillItemRow(
    bill: RecurringPayment,
    today: Int,
    onPaidToggle: (RecurringPayment) -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOverdue = !bill.isPaid && bill.dueDate < today
    val borderStroke = if (isOverdue) BorderStroke(1.5.dp, Color(0xFFB3261E)) else BorderStroke(1.dp, Color(0x4DCAC4D0))

    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (bill.isPaid) Color(0xFF1B6F1F).copy(alpha = 0.12f) else Color(0xFFB3261E).copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(bill.category),
                            contentDescription = null,
                            tint = if (bill.isPaid) Color(0xFF1B6F1F) else Color(0xFFB3261E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(bill.title, color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(bill.category, color = Color(0xFF49454F), fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format("%,.2f", bill.amount)}",
                        color = if (bill.isPaid) Color(0xFF49454F) else Color(0xFF1D1B20),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp).minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x1ACAC4D0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Due text and overdue marker
                Column(modifier = Modifier.weight(1f)) {
                    val statusText = if (bill.isPaid) {
                        "Settled for this cycle"
                    } else if (bill.dueDate == today) {
                        "DUE TODAY!"
                    } else if (bill.dueDate < today) {
                        "OVERDUE BY ${today - bill.dueDate} DAYS"
                    } else {
                        "Due in ${bill.dueDate - today} days"
                    }
                    val statusColor = if (bill.isPaid) Color(0xFF1B6F1F) else if (bill.dueDate <= today) Color(0xFFB3261E) else Color(0xFFB57000)

                    Text(
                        text = "Due Day: ${bill.dueDate} of month",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Check button toggle paid status
                Button(
                    onClick = { onPaidToggle(bill) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bill.isPaid) Color(0xFFE8DEF8) else Color(0xFF6750A4)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("bill_paid_toggle_${bill.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (bill.isPaid) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B6F1F), modifier = Modifier.size(14.dp))
                            Text("Paid", color = Color(0xFF1B6F1F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Mark Paid", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Tab 3: AI DISCIPLINE REPORTS & FEEDBACK
@Composable
fun AIReportsTab(
    viewModel: FinanceViewModel,
    txns: List<Transaction>,
    bills: List<RecurringPayment>,
    metrics: FinancialMetrics,
    aiReportText: String?,
    isGeneratingReport: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-Fi Circular Discipline Gauge Card
        OutlinedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = whiteCardBg),
            border = BorderStroke(1.dp, borderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FINANCIAL DISCIPLINE INDEX",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Circular custom draw gauge
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeWidthPx = 14.dp
                    val indexScore = metrics.disciplineScore
                    val animatedScore by animateFloatAsState(
                        targetValue = indexScore.toFloat(),
                        animationSpec = tween(1200, easing = LinearOutSlowInEasing)
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background track arc
                        drawArc(
                            color = Color(0xFFE7E0EC),
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx.toPx(), cap = StrokeCap.Round)
                        )

                        val sweepAngle = (animatedScore / 100f) * 270f
                        val gaugeColor = if (indexScore >= 80) Color(0xFF1B6F1F) else if (indexScore >= 50) Color(0xFFB57000) else Color(0xFFB3261E)

                        // Progress arc
                        drawArc(
                            color = gaugeColor,
                            startAngle = 135f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${animatedScore.toInt()}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = textDark
                        )
                        val ratingText = if (indexScore >= 80) "EXCELLENT" else if (indexScore >= 50) "MODERATE" else "CRITICAL"
                        val ratingColor = if (indexScore >= 80) Color(0xFF1B6F1F) else if (indexScore >= 50) Color(0xFFB57000) else Color(0xFFB3261E)
                        Text(
                            text = ratingText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ratingColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Score is calculated from saving capacity, payment compliance of recurring bills, and budget overflow channels.",
                    color = textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // Action controls to launch Gemini analysis
        Button(
            onClick = { viewModel.generateAiReport(txns, bills) },
            enabled = !isGeneratingReport,
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryPurple,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE7E0EC),
                disabledContentColor = textSecondary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_report_btn")
        ) {
            if (isGeneratingReport) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textSecondary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Analyzing Ledger...", color = textSecondary, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compile Financial Report", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Markdown Report card display
        if (aiReportText != null) {
            OutlinedCard(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                border = BorderStroke(1.dp, borderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_report_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GENERATED STATEMENT", color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Black)

                        // Visual chip indicator for Gemini vs Local
                        val isLocalFeedback = aiReportText.contains("locally parsed heuristics")
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isLocalFeedback) Color(0xFFE7E0EC) else cardLavender,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLocalFeedback) "Local Analysis" else "Gemini AI Live",
                                color = if (isLocalFeedback) textSecondary else deepPurpleText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Parse basic Markdown headers and render matching Compose blocks beautifully
                    val paragraphs = aiReportText.split("\n\n")
                    paragraphs.forEach { paragraph ->
                        if (paragraph.startsWith("##")) {
                            val headerTitle = paragraph.removePrefix("##").trim()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = headerTitle.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryPurple,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            // Render standard paragraph out containing markdown bolds
                            Text(
                                text = renderSimpleMarkdown(paragraph),
                                fontSize = 14.sp,
                                color = textDark,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Uninstantiated AI state instructions
            OutlinedCard(
                border = BorderStroke(1.dp, borderLight),
                colors = CardDefaults.cardColors(containerColor = whiteCardBg),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = primaryPurple,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ready to audit spending habits?",
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Compute your Financial Discipline report to synthesize monthly cashflows, billing rates, and get highly objective action step suggestions.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Super lightweight helper to parse and highlight basic markdown bold (e.g. **text**)
 * in plain text fields since standard compose text doesn't native parse markdown.
 */
@Composable
fun renderSimpleMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            val parts = text.split("**")
            parts.forEachIndexed { idx, part ->
                if (idx % 2 == 1) {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF6750A4)))
                    append(part)
                    pop()
                } else {
                    append(part)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Food") }
    var amountStr by remember { mutableStateOf("") }
    val categories = listOf("Food", "Groceries", "Shopping", "Rent", "Utilities", "Entertainment", "Transport", "Other")

    // Theme aliases from surrounding file (manually bound since they are file-private)
    val dialogBg = Color(0xFFFFFFFF)
    val textDarkColor = Color(0xFF1D1B20)
    val textSecondaryColor = Color(0xFF49454F)
    val purplePrimary = Color(0xFF6750A4)
    val textPurpleDeep = Color(0xFF21005D)
    val lavenderCard = Color(0xFFEADDFF)
    val lightBorder = Color(0x4DCAC4D0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Define Category Budget", color = textDarkColor, fontWeight = FontWeight.Bold) },
        containerColor = dialogBg,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Define a monthly limits threshold. You'll receive real-time alerts when hovering at or exceeding limits.",
                    color = textSecondaryColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Monthly Limit ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = purplePrimary,
                        focusedLabelColor = purplePrimary,
                        unfocusedBorderColor = lightBorder,
                        focusedTextColor = textDarkColor,
                        unfocusedTextColor = textDarkColor
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_budget_amount_input")
                )

                Text("Select Category", color = textSecondaryColor, fontSize = 12.sp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = lavenderCard,
                                selectedLabelColor = textPurpleDeep,
                                containerColor = Color(0xFFE7E0EC),
                                labelColor = textSecondaryColor
                            ),
                            modifier = Modifier.testTag("budget_chip_$cat")
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = amountStr.toDoubleOrNull() ?: 0.0
                    if (limit > 0) {
                        onSave(selectedCategory, limit)
                    }
                },
                modifier = Modifier.testTag("confirm_save_budget")
            ) {
                Text("Save", color = purplePrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondaryColor)
            }
        }
    )
}

