package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountCategory
import com.example.data.model.AiFinancialInsight
import com.example.data.model.CashFlowEntry
import com.example.data.model.CashFlowType
import com.example.data.model.DebtCreditRecord
import com.example.data.model.DebtCreditStatus
import com.example.data.model.DebtCreditType
import com.example.data.model.FinancialSummaryMetrics
import com.example.data.model.InvestmentAsset
import com.example.data.model.MoneyAccount
import com.example.data.model.RentCollectionStatus
import com.example.data.model.SavingsVault
import com.example.data.model.formatCurrency
import com.example.service.DebtAlarmScheduler
import com.example.ui.components.money.AddAccountDialog
import com.example.ui.components.money.AddCashFlowDialog
import com.example.ui.components.money.AddDebtCreditDialog
import com.example.ui.components.money.AddInvestmentAssetDialog
import com.example.ui.components.money.AdjustAccountBalanceDialog
import com.example.ui.components.money.AiAdvisoryBottomSheet
import com.example.ui.components.money.AiInsightCard
import com.example.ui.components.money.CreateVaultDialog
import com.example.ui.components.money.ManageVaultDialog
import com.example.ui.viewmodel.LifestyleViewModel

enum class MoneySubSection(val title: String, val icon: String) {
    OVERVIEW("Overview", "📊"),
    LEDGER("Ledger & P&L", "💼"),
    INVESTMENTS("Investments", "📈"),
    VAULTS("Vaults", "🏦"),
    DEBTS("Debt Tracker", "🤝"),
    AI_ADVISOR("AI Advisor", "✨")
}

@Composable
fun MoneyScreen(
    viewModel: LifestyleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics by viewModel.financialMetrics.collectAsStateWithLifecycle()
    val accounts by viewModel.moneyAccounts.collectAsStateWithLifecycle()
    val cashFlows by viewModel.cashFlowEntries.collectAsStateWithLifecycle()
    val investments by viewModel.investmentAssets.collectAsStateWithLifecycle()
    val vaults by viewModel.savingsVaults.collectAsStateWithLifecycle()
    val debts by viewModel.debtCreditRecords.collectAsStateWithLifecycle()
    val insights by viewModel.aiFinancialInsights.collectAsStateWithLifecycle()
    val isGeneratingAi by viewModel.isGeneratingAiFinancials.collectAsStateWithLifecycle()
    val flowFilter by viewModel.cashFlowFilter.collectAsStateWithLifecycle()

    var activeSubSection by remember { mutableStateOf(MoneySubSection.OVERVIEW) }

    // Dialog States
    var showAddCashFlowDialog by remember { mutableStateOf(false) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var showCreateVaultDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<MoneyAccount?>(null) }
    var vaultToManage by remember { mutableStateOf<SavingsVault?>(null) }
    var showAiBottomSheet by remember { mutableStateOf(false) }
    var repaymentDebtRecord by remember { mutableStateOf<DebtCreditRecord?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeSubSection) {
                        MoneySubSection.OVERVIEW -> showAddCashFlowDialog = true
                        MoneySubSection.LEDGER -> showAddCashFlowDialog = true
                        MoneySubSection.INVESTMENTS -> showAddInvestmentDialog = true
                        MoneySubSection.VAULTS -> showCreateVaultDialog = true
                        MoneySubSection.DEBTS -> showAddDebtDialog = true
                        MoneySubSection.AI_ADVISOR -> viewModel.refreshAiInsights(null)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_money_action")
            ) {
                Icon(
                    imageVector = when (activeSubSection) {
                        MoneySubSection.AI_ADVISOR -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Add
                    },
                    contentDescription = "Add Financial Record"
                )
            }
        },
        modifier = modifier.testTag("screen_money")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Unified Net Worth Command Center Hero Banner
            NetWorthHeroBanner(
                metrics = metrics,
                onAccountClick = { account -> accountToAdjust = account },
                onAddAccount = { showAddAccountDialog = true },
                accounts = accounts,
                onOpenAi = { showAiBottomSheet = true }
            )

            // Sub-Section Navigation Tabs
            SubSectionTabBar(
                selected = activeSubSection,
                onSelect = { activeSubSection = it }
            )

            // Content Area
            Box(modifier = Modifier.fillMaxSize()) {
                when (activeSubSection) {
                    MoneySubSection.OVERVIEW -> {
                        OverviewCommandCenterView(
                            metrics = metrics,
                            accounts = accounts,
                            cashFlows = cashFlows,
                            vaults = vaults,
                            debts = debts,
                            insights = insights,
                            onOpenAi = { showAiBottomSheet = true },
                            onLogCashFlow = { showAddCashFlowDialog = true },
                            onAddDebt = { showAddDebtDialog = true },
                            onAddVault = { showCreateVaultDialog = true },
                            onAddInvestment = { showAddInvestmentDialog = true },
                            onSelectAccount = { accountToAdjust = it },
                            onSelectVault = { vaultToManage = it }
                        )
                    }

                    MoneySubSection.LEDGER -> {
                        CashFlowLedgerView(
                            metrics = metrics,
                            entries = cashFlows,
                            filter = flowFilter,
                            onFilterChange = { viewModel.setCashFlowFilter(it) },
                            onAddEntry = { showAddCashFlowDialog = true },
                            onDeleteEntry = { viewModel.deleteCashFlowEntry(it) }
                        )
                    }

                    MoneySubSection.INVESTMENTS -> {
                        InvestmentPortfolioView(
                            investments = investments,
                            onAddAsset = { showAddInvestmentDialog = true },
                            onRentStatusChange = { id, status -> viewModel.updateRentStatus(id, status) },
                            onDeleteAsset = { id -> viewModel.deleteInvestment(id) }
                        )
                    }

                    MoneySubSection.VAULTS -> {
                        SavingsVaultsView(
                            vaults = vaults,
                            onAddVault = { showCreateVaultDialog = true },
                            onManageVault = { vaultToManage = it },
                            onDeleteVault = { id -> viewModel.deleteVault(id) }
                        )
                    }

                    MoneySubSection.DEBTS -> {
                        DebtCreditTrackerView(
                            debts = debts,
                            onAddDebt = { showAddDebtDialog = true },
                            onRecordRepayment = { record ->
                                repaymentDebtRecord = record
                            },
                            onToggleAlarm = { id, enabled ->
                                viewModel.toggleDebtAlarm(id, enabled)
                            },
                            onSyncCalendar = { record ->
                                DebtAlarmScheduler.launchCalendarInsert(context, record)
                                viewModel.markCalendarSynced(record.id)
                            },
                            onDelete = { viewModel.deleteDebtCreditRecord(it) }
                        )
                    }

                    MoneySubSection.AI_ADVISOR -> {
                        AiWealthEngineView(
                            insights = insights,
                            isGenerating = isGeneratingAi,
                            onRefresh = { customQ -> viewModel.refreshAiInsights(customQ) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddCashFlowDialog) {
        AddCashFlowDialog(
            onDismiss = { showAddCashFlowDialog = false },
            onSave = { entry -> viewModel.addCashFlowEntry(entry) }
        )
    }

    if (showAddDebtDialog) {
        AddDebtCreditDialog(
            onDismiss = { showAddDebtDialog = false },
            onSave = { debt -> viewModel.addDebtCreditRecord(debt) }
        )
    }

    if (showCreateVaultDialog) {
        CreateVaultDialog(
            onDismiss = { showCreateVaultDialog = false },
            onSave = { vault ->
                viewModel.addOrUpdateVault(vault)
                showCreateVaultDialog = false
            }
        )
    }

    if (showAddInvestmentDialog) {
        AddInvestmentAssetDialog(
            onDismiss = { showAddInvestmentDialog = false },
            onSave = { asset ->
                viewModel.addOrUpdateInvestment(asset)
                showAddInvestmentDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { acc ->
                viewModel.addOrUpdateAccount(acc)
                showAddAccountDialog = false
            }
        )
    }

    if (accountToAdjust != null) {
        AdjustAccountBalanceDialog(
            account = accountToAdjust!!,
            onDismiss = { accountToAdjust = null },
            onUpdateBalance = { id, newBalance -> viewModel.updateAccountBalance(id, newBalance) }
        )
    }

    if (vaultToManage != null) {
        ManageVaultDialog(
            vault = vaultToManage!!,
            onDismiss = { vaultToManage = null },
            onDeposit = { id, amount -> viewModel.depositToVault(id, amount) },
            onWithdraw = { id, amount -> viewModel.withdrawFromVault(id, amount) }
        )
    }

    if (showAiBottomSheet) {
        AiAdvisoryBottomSheet(
            insights = insights,
            isGenerating = isGeneratingAi,
            onRefresh = { q -> viewModel.refreshAiInsights(q) },
            onDismiss = { showAiBottomSheet = false }
        )
    }

    if (repaymentDebtRecord != null) {
        RepaymentConfirmationDialog(
            record = repaymentDebtRecord!!,
            onDismiss = { repaymentDebtRecord = null },
            onConfirmRepayment = { amount ->
                viewModel.recordDebtRepayment(repaymentDebtRecord!!.id, amount)
                repaymentDebtRecord = null
            }
        )
    }
}

// -------------------------------------------------------------------------
// Top Hero Banner: Unified Net Worth Command Center
// -------------------------------------------------------------------------
@Composable
fun NetWorthHeroBanner(
    metrics: FinancialSummaryMetrics,
    accounts: List<MoneyAccount>,
    onAccountClick: (MoneyAccount) -> Unit,
    onAddAccount: () -> Unit,
    onOpenAi: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("banner_net_worth_hero")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL NET WORTH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metrics.formattedNetWorth,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (metrics.netWorth > 0) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (metrics.netWorth > 0) "Live Portfolio" else "Clean Slate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (metrics.netWorth > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onOpenAi,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF673AB7).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Advisor",
                            tint = Color(0xFF673AB7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment Asset Allocation Bar
            AssetAllocationBar(metrics = metrics)

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Asset Category Chips with Add Account Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssetPill(
                    emoji = "💵",
                    title = "Pocket Cash",
                    amount = metrics.formattedPocketCash,
                    bgColor = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32),
                    onClick = {
                        val pocketAcc = accounts.firstOrNull { it.category == AccountCategory.POCKET_CASH }
                        if (pocketAcc != null) onAccountClick(pocketAcc)
                    }
                )
                AssetPill(
                    emoji = "🏦",
                    title = "Bank Accounts",
                    amount = metrics.formattedBankTotal,
                    bgColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2),
                    onClick = {
                        val bankAcc = accounts.firstOrNull { it.category == AccountCategory.BANK }
                        if (bankAcc != null) onAccountClick(bankAcc)
                    }
                )
                AssetPill(
                    emoji = "📈",
                    title = "Investments",
                    amount = metrics.formattedInvestments,
                    bgColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100),
                    onClick = {
                        val invAcc = accounts.firstOrNull { it.category == AccountCategory.MARKET_INVESTMENT }
                        if (invAcc != null) onAccountClick(invAcc)
                    }
                )
                AssetPill(
                    emoji = "🌍",
                    title = "Global Assets",
                    amount = metrics.formattedGlobalAssets,
                    bgColor = Color(0xFFF3E5F5),
                    textColor = Color(0xFF7B1FA2),
                    onClick = {
                        val assetAcc = accounts.firstOrNull { it.category == AccountCategory.GLOBAL_ASSET }
                        if (assetAcc != null) onAccountClick(assetAcc)
                    }
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable(onClick = onAddAccount)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("➕", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Account",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Add Custom",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetAllocationBar(metrics: FinancialSummaryMetrics) {
    val total = metrics.totalAssets
    if (total <= 0.0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        val pocketWeight = (metrics.liquidPocketCashTotal / total).toFloat().coerceIn(0.01f, 1f)
        val bankWeight = (metrics.bankAccountsTotal / total).toFloat().coerceIn(0.01f, 1f)
        val investWeight = (metrics.marketInvestmentsTotal / total).toFloat().coerceIn(0.01f, 1f)
        val globalWeight = (metrics.globalAssetsTotal / total).toFloat().coerceIn(0.01f, 1f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(modifier = Modifier.weight(pocketWeight).fillMaxSize().background(Color(0xFF43A047)))
            Box(modifier = Modifier.weight(bankWeight).fillMaxSize().background(Color(0xFF1E88E5)))
            Box(modifier = Modifier.weight(investWeight).fillMaxSize().background(Color(0xFFFB8C00)))
            Box(modifier = Modifier.weight(globalWeight).fillMaxSize().background(Color(0xFF8E24AA)))
        }
    }
}

@Composable
fun AssetPill(
    emoji: String,
    title: String,
    amount: String,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

// -------------------------------------------------------------------------
// SubSection Tab Bar
// -------------------------------------------------------------------------
@Composable
fun SubSectionTabBar(
    selected: MoneySubSection,
    onSelect: (MoneySubSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MoneySubSection.entries.forEach { sub ->
            val isSelected = selected == sub
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(sub) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.icon, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = sub.title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------
// View 1: Command Center Overview
// -------------------------------------------------------------------------
@Composable
fun OverviewCommandCenterView(
    metrics: FinancialSummaryMetrics,
    accounts: List<MoneyAccount>,
    cashFlows: List<CashFlowEntry>,
    vaults: List<SavingsVault>,
    debts: List<DebtCreditRecord>,
    insights: List<AiFinancialInsight>,
    onOpenAi: () -> Unit,
    onLogCashFlow: () -> Unit,
    onAddDebt: () -> Unit,
    onAddVault: () -> Unit,
    onAddInvestment: () -> Unit,
    onSelectAccount: (MoneyAccount) -> Unit,
    onSelectVault: (SavingsVault) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLogCashFlow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("+ Log Cashflow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAddVault,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                ) {
                    Text("+ Savings Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAddInvestment,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("+ Investment Asset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onAddDebt,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ Debt / Loan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Commercial Business Snapshot Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("card_commercial_snapshot")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💼", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Commercial Business Ledger",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (metrics.totalRevenue > 0) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (metrics.totalRevenue > 0) "Margin: ${String.format("%.1f", metrics.commercialMarginPercent)}%" else "Margin: 0.0%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (metrics.totalRevenue > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(label = "Revenue", value = metrics.formattedRevenue, color = Color(0xFF2E7D32))
                        MetricBlock(label = "Purchases", value = metrics.formattedPurchases, color = Color(0xFFD81B60))
                        MetricBlock(label = "Selling Costs", value = metrics.formattedSellingCosts, color = Color(0xFFFB8C00))
                        MetricBlock(
                            label = "Net P&L",
                            value = metrics.formattedCommercialNet,
                            color = if (metrics.commercialNetProfitOrLoss >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }

        // AI Advisory Highlight Banner
        item {
            val topInsight = insights.firstOrNull()
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF673AB7).copy(alpha = 0.08f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAi)
                    .testTag("card_ai_advisory_highlight")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (topInsight != null) "AI Financial Insight: ${topInsight.category.label}" else "On-Demand AI Advisor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF673AB7)
                            )
                        }
                        Text("Open Advisor ›", fontSize = 11.sp, color = Color(0xFF673AB7), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = topInsight?.title ?: "AI Engine Ready (Clean Slate)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topInsight?.actionRecommendation ?: "Remains quiet until you input your personal transactions and vaults. Tap to open the on-demand wealth intelligence layer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Goal-Driven Savings Vaults Preview
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Goal-Driven Savings Vaults",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddVault) {
                        Text("+ Add Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (vaults.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onAddVault)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏦", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("No savings vaults created yet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tap to set targets for children, education, parental support, or retirement in ₹", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    vaults.take(3).forEach { vault ->
                        VaultRowItem(vault = vault, onClick = { onSelectVault(vault) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Active Debt & Credit Ledger Preview
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Debt & Credit Schedules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddDebt) {
                        Text("+ Add Loan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (debts.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onAddDebt)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🤝", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("No debt or loan records logged yet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tap to track money lent or borrowed, due dates, and reminders", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    debts.take(3).forEach { debt ->
                        DebtRowItem(debt = debt, onClick = {})
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun MetricBlock(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

// -------------------------------------------------------------------------
// View 2: Detailed Cash Flow & Business Ledger
// -------------------------------------------------------------------------
@Composable
fun CashFlowLedgerView(
    metrics: FinancialSummaryMetrics,
    entries: List<CashFlowEntry>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onAddEntry: () -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var periodFilter by remember { mutableStateOf("ALL_TIME") }
    val todayDateStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
    val currentMonthStr = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date()) }

    val filteredList = entries.filter { entry ->
        val typeMatch = when (filter) {
            "BUSINESS" -> entry.isBusiness
            "PERSONAL" -> !entry.isBusiness
            else -> true
        }
        val periodMatch = when (periodFilter) {
            "TODAY" -> entry.date == todayDateStr
            "MONTH" -> entry.date.startsWith(currentMonthStr)
            else -> true
        }
        typeMatch && periodMatch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Module Header with Prominent Add New Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cash Flow & Business Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personal spending & commercial P&L in ₹",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddEntry,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+ Add Transaction", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Commercial Financial Breakdown Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Commercial P&L Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LedgerBreakdownRow("Overall Revenue & Sales", metrics.formattedRevenue, Color(0xFF2E7D32))
                    LedgerBreakdownRow("Stock & Inventory Purchases", "-${metrics.formattedPurchases}", Color(0xFFD81B60))
                    LedgerBreakdownRow("Selling Costs (Logistics & Fees)", "-${metrics.formattedSellingCosts}", Color(0xFFFB8C00))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LedgerBreakdownRow(
                        "Net Commercial Profit / Loss",
                        metrics.formattedCommercialNet,
                        if (metrics.commercialNetProfitOrLoss >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        isBold = true
                    )
                }
            }
        }

        // Personal Cashflow Summary Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Personal Spending & Income",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LedgerBreakdownRow("Personal Income / Salary", "+${formatCurrency(metrics.totalPersonalIncome)}", Color(0xFF1E88E5))
                    LedgerBreakdownRow("Personal Living Expenses", "-${formatCurrency(metrics.totalPersonalExpenses)}", Color(0xFFE53935))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LedgerBreakdownRow(
                        "Net Personal Savings",
                        formatCurrency(metrics.personalNetSavings),
                        if (metrics.personalNetSavings >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        isBold = true
                    )
                }
            }
        }

        // Filter Chips Row (Type and Time Period)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Scope Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All Types", "BUSINESS" to "💼 Commercial", "PERSONAL" to "👤 Personal").forEach { (key, label) ->
                        FilterChip(
                            selected = filter == key,
                            onClick = { onFilterChange(key) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Time Period Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL_TIME" to "All Time", "TODAY" to "📅 Today", "MONTH" to "🗓️ This Month").forEach { (key, label) ->
                        FilterChip(
                            selected = periodFilter == key,
                            onClick = { periodFilter = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Entries List or Clean Slate Empty Card
        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💼", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Clean Ledger • No Transactions Logged",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Record daily personal expenditures, salaries, or commercial business sales, purchases, and selling costs. All metrics and profit/loss calculate strictly from your entries.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddEntry,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Log Cashflow Transaction", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { entry ->
                CashFlowEntryCard(entry = entry, onDelete = { onDeleteEntry(entry.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun LedgerBreakdownRow(
    title: String,
    amount: String,
    color: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun CashFlowEntryCard(
    entry: CashFlowEntry,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(entry.type.badgeColor).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (entry.isBusiness) Icons.Default.BusinessCenter else Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = Color(entry.type.badgeColor),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${entry.category} • ${entry.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.formattedAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(entry.type.badgeColor)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// View 3: Investment Portfolio & Rentals
// -------------------------------------------------------------------------
@Composable
fun InvestmentPortfolioView(
    investments: List<InvestmentAsset>,
    onAddAsset: () -> Unit,
    onRentStatusChange: (assetId: String, status: RentCollectionStatus) -> Unit,
    onDeleteAsset: (String) -> Unit
) {
    val totalInvested = investments.sumOf { it.investedAmount }
    val totalCurrent = investments.sumOf { it.currentValue }
    val overallGain = totalCurrent - totalInvested
    val totalRentalIncome = investments.sumOf { it.monthlyRentalIncome }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Module Header with Prominent Add New Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Investment Portfolio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Asset classes, valuations & rental flow in ₹",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddAsset,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+ Add Asset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Portfolio Stats Summary
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PORTFOLIO VALUATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(totalCurrent),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Invested", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(formatCurrency(totalInvested), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Capital Growth", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(
                                text = if (overallGain >= 0) "+${formatCurrency(overallGain)}" else formatCurrency(overallGain),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (overallGain >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                        }
                        Column {
                            Text("Monthly Rent Flow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(
                                text = "${formatCurrency(totalRentalIncome)} / mo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Empty State or List of Assets
        if (investments.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📈", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Blank Investment Portfolio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Define your custom asset classes—stocks, mutual funds, gold, real estate rental units, crypto, or business equity. Valuation starts at zero until you enter your holdings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddAsset,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Add First Investment Asset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(investments, key = { it.id }) { asset ->
                InvestmentAssetCard(
                    asset = asset,
                    onRentStatusChange = onRentStatusChange,
                    onDelete = { onDeleteAsset(asset.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun InvestmentAssetCard(
    asset: InvestmentAsset,
    onRentStatusChange: (assetId: String, status: RentCollectionStatus) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(asset.category.iconEmoji, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = asset.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = asset.formattedCurrentValue,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = asset.formattedGainLoss,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (asset.gainOrLoss >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Asset",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Rental property specific controls
            if (asset.category == com.example.data.model.InvestmentAssetCategory.REAL_ESTATE_RENTAL && asset.monthlyRentalIncome > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Rental Income Allocation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${formatCurrency(asset.monthlyRentalIncome)} / month",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Cycle rent status chip
                        FilterChip(
                            selected = asset.rentStatus == RentCollectionStatus.COLLECTED,
                            onClick = {
                                val nextStatus = when (asset.rentStatus) {
                                    RentCollectionStatus.COLLECTED -> RentCollectionStatus.PENDING
                                    RentCollectionStatus.PENDING -> RentCollectionStatus.OVERDUE
                                    RentCollectionStatus.OVERDUE -> RentCollectionStatus.COLLECTED
                                    else -> RentCollectionStatus.COLLECTED
                                }
                                onRentStatusChange(asset.id, nextStatus)
                            },
                            label = { Text(asset.rentStatus.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(asset.rentStatus.colorHex).copy(alpha = 0.2f),
                                selectedLabelColor = Color(asset.rentStatus.colorHex)
                            )
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// View 4: Goal-Driven Savings Vaults
// -------------------------------------------------------------------------
@Composable
fun SavingsVaultsView(
    vaults: List<SavingsVault>,
    onAddVault: () -> Unit,
    onManageVault: (SavingsVault) -> Unit,
    onDeleteVault: (String) -> Unit
) {
    val totalSaved = vaults.sumOf { it.currentAmount }
    val totalTarget = vaults.sumOf { it.targetAmount }
    val overallFraction = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Module Header with Prominent Add New Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Goal-Driven Savings Vaults",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Children's future, education, parents & retirement in ₹",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onAddVault,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("+ Add Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (vaults.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏦", fontSize = 28.sp)
                        }
                        Text(
                            text = "Clean Slate: No Savings Vaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Build dedicated wealth pools for Children's Future, Education Funds, Parental Support, or Retirement. All milestones and targets are tracked in Indian Rupees (₹).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onAddVault,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("+ Create First Savings Vault", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Vaults Header Milestone Summary
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "TOTAL SAVINGS & WEALTH VAULTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatCurrency(totalSaved)} / ${formatCurrency(totalTarget)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(overallFraction * 100).toInt()}% overall goal milestone completed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Milestone Segmented Vaults",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(vaults, key = { it.id }) { vault ->
                VaultCard(
                    vault = vault,
                    onManage = { onManageVault(vault) },
                    onDelete = { onDeleteVault(vault.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun VaultCard(
    vault: SavingsVault,
    onManage: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(vault.category.themeColorHex).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(vault.category.defaultEmoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vault.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = vault.category.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onManage,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(vault.category.themeColorHex)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Vault",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${vault.formattedCurrent} saved",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(vault.category.themeColorHex)
                )
                Text(
                    text = "Goal: ${vault.formattedTarget}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { vault.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(vault.category.themeColorHex),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${vault.progressPercent}% milestone reached",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Target: ${vault.formattedMonthlyTarget}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (vault.milestoneNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = vault.milestoneNotes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun VaultRowItem(vault: SavingsVault, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(vault.category.defaultEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(vault.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { vault.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(vault.category.themeColorHex)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${vault.progressPercent}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(vault.category.themeColorHex)
            )
        }
    }
}

// -------------------------------------------------------------------------
// View 5: Granular Debt & Credit Tracker
// -------------------------------------------------------------------------
@Composable
fun DebtCreditTrackerView(
    debts: List<DebtCreditRecord>,
    onAddDebt: () -> Unit,
    onRecordRepayment: (DebtCreditRecord) -> Unit,
    onToggleAlarm: (recordId: String, enabled: Boolean) -> Unit,
    onSyncCalendar: (DebtCreditRecord) -> Unit,
    onDelete: (String) -> Unit
) {
    val totalReceivable = debts.filter { it.type == DebtCreditType.LENT_OUT && it.status != DebtCreditStatus.SETTLED }.sumOf { it.remainingAmount }
    val totalPayable = debts.filter { it.type == DebtCreditType.BORROWED && it.status != DebtCreditStatus.SETTLED }.sumOf { it.remainingAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Debt Summary Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("🟢 Money Lent (Receivable)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatCurrency(totalReceivable), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("🔴 Money Borrowed (Payable)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatCurrency(totalPayable), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Ledgers & Payment Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddDebt,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+ Add Record", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (debts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No debt or credit records logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(debts, key = { it.id }) { debt ->
                DebtCreditCard(
                    debt = debt,
                    onRecordRepayment = { onRecordRepayment(debt) },
                    onToggleAlarm = { onToggleAlarm(debt.id, !debt.hasReminderAlarm) },
                    onSyncCalendar = { onSyncCalendar(debt) },
                    onDelete = { onDelete(debt.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

@Composable
fun DebtCreditCard(
    debt: DebtCreditRecord,
    onRecordRepayment: () -> Unit,
    onToggleAlarm: () -> Unit,
    onSyncCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val isReceivable = debt.type == DebtCreditType.LENT_OUT
    val accentColor = if (isReceivable) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isReceivable) "🟢 Lent Out" else "🔴 Borrowed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("• Due: ${debt.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(debt.contactName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = debt.formattedRemaining,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Text("of ${debt.formattedOriginal}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payoff Progress Bar
            LinearProgressIndicator(
                progress = { debt.payoffFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${debt.payoffPercentage}% settled (${debt.formattedPaid} paid)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(debt.status.label, fontSize = 11.sp, color = Color(debt.status.colorHex), fontWeight = FontWeight.Bold)
            }

            if (debt.interestOrTerms.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Terms: ${debt.interestOrTerms}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Repayment, Alarm reminder toggle, Calendar integration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Smart alarm reminder toggle
                    IconButton(
                        onClick = onToggleAlarm,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (debt.hasReminderAlarm) Color(0xFFF57C00).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm reminder",
                            tint = if (debt.hasReminderAlarm) Color(0xFFF57C00) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Calendar integration
                    IconButton(
                        onClick = onSyncCalendar,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (debt.calendarSynced) Color(0xFF1976D2).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Sync to calendar",
                            tint = if (debt.calendarSynced) Color(0xFF1976D2) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (debt.status != DebtCreditStatus.SETTLED) {
                    Button(
                        onClick = onRecordRepayment,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Record Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DebtRowItem(debt: DebtCreditRecord, onClick: () -> Unit) {
    val isReceivable = debt.type == DebtCreditType.LENT_OUT
    val accentColor = if (isReceivable) Color(0xFF2E7D32) else Color(0xFFD32F2F)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(debt.contactName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Due: ${debt.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = debt.formattedRemaining,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

// -------------------------------------------------------------------------
// View 6: AI Wealth Engine
// -------------------------------------------------------------------------
@Composable
fun AiWealthEngineView(
    insights: List<AiFinancialInsight>,
    isGenerating: Boolean,
    onRefresh: (String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF673AB7).copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Financial Insights Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF512DA8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Analyzing idle cash, forecasting outflows, and optimizing business margin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onRefresh(null) },
                        enabled = !isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(if (isGenerating) "Analyzing..." else "Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (insights.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF673AB7).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 28.sp)
                        }
                        Text(
                            text = "AI Financial Engine Standing By",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF512DA8)
                        )
                        Text(
                            text = "Clean Slate Mode: The intelligent layer remains quiet until you enter your financial details. Log daily cash flows, business metrics, investment assets, or savings targets to unlock personalized AI recommendations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { onRefresh(null) },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                if (isGenerating) "Analyzing Data..." else "⚡ Analyze My Finances",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            items(insights, key = { it.id }) { insight ->
                AiInsightCard(insight = insight)
            }
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}

// -------------------------------------------------------------------------
// Repayment Dialog
// -------------------------------------------------------------------------
@Composable
fun RepaymentConfirmationDialog(
    record: DebtCreditRecord,
    onDismiss: () -> Unit,
    onConfirmRepayment: (amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf(record.remainingAmount.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Record Repayment: ${record.contactName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Remaining balance: ${record.formattedRemaining}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        val parsed = amountText.toDoubleOrNull() ?: 0.0
                        if (parsed > 0) {
                            onConfirmRepayment(parsed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
