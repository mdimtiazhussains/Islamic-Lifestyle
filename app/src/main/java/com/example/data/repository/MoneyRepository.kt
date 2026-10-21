package com.example.data.repository

import android.content.Context
import com.example.data.database.MoneyDao
import com.example.data.model.AccountCategory
import com.example.data.model.AiFinancialInsight
import com.example.data.model.CashFlowEntry
import com.example.data.model.CashFlowType
import com.example.data.model.DebtCreditRecord
import com.example.data.model.DebtCreditStatus
import com.example.data.model.DebtCreditType
import com.example.data.model.FinancialSummaryMetrics
import com.example.data.model.InvestmentAsset
import com.example.data.model.InvestmentAssetCategory
import com.example.data.model.MoneyAccount
import com.example.data.model.RentCollectionStatus
import com.example.data.model.SavingsVault
import com.example.data.model.SavingsVaultCategory
import com.example.data.model.toDomain
import com.example.data.model.toEntity
import com.example.service.DebtAlarmScheduler
import com.example.service.FinancialAiInsightsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MoneyRepository(
    private val moneyDao: MoneyDao,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Flow of Domain Models
    val accounts: Flow<List<MoneyAccount>> = moneyDao.getAllAccounts().map { list ->
        list.map { it.toDomain() }
    }

    val cashFlowEntries: Flow<List<CashFlowEntry>> = moneyDao.getAllCashFlowEntries().map { list ->
        list.map { it.toDomain() }
    }

    val investments: Flow<List<InvestmentAsset>> = moneyDao.getAllInvestments().map { list ->
        list.map { it.toDomain() }
    }

    val vaults: Flow<List<SavingsVault>> = moneyDao.getAllVaults().map { list ->
        list.map { it.toDomain() }
    }

    val debts: Flow<List<DebtCreditRecord>> = moneyDao.getAllDebtCreditRecords().map { list ->
        list.map { it.toDomain() }
    }

    // AI Financial Insights state flow
    private val _aiInsights = MutableStateFlow<List<AiFinancialInsight>>(emptyList())
    val aiInsights: StateFlow<List<AiFinancialInsight>> = _aiInsights.asStateFlow()

    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    // Aggregated real-time summary metrics combining all accounts, cashflows, and debts
    val financialMetrics: Flow<FinancialSummaryMetrics> = combine(
        accounts,
        cashFlowEntries,
        debts
    ) { accList, flowList, debtList ->
        val pocket = accList.filter { it.category == AccountCategory.POCKET_CASH }.sumOf { it.balance }
        val bank = accList.filter { it.category == AccountCategory.BANK }.sumOf { it.balance }
        val market = accList.filter { it.category == AccountCategory.MARKET_INVESTMENT }.sumOf { it.balance }
        val global = accList.filter { it.category == AccountCategory.GLOBAL_ASSET }.sumOf { it.balance }

        val receivable = debtList
            .filter { it.type == DebtCreditType.LENT_OUT && it.status != DebtCreditStatus.SETTLED }
            .sumOf { it.remainingAmount }

        val payable = debtList
            .filter { it.type == DebtCreditType.BORROWED && it.status != DebtCreditStatus.SETTLED }
            .sumOf { it.remainingAmount }

        val rev = flowList.filter { it.type == CashFlowType.REVENUE }.sumOf { it.amount }
        val purchases = flowList.filter { it.type == CashFlowType.PURCHASE }.sumOf { it.amount }
        val sellingCosts = flowList.filter { it.type == CashFlowType.SELLING_COST }.sumOf { it.amount }
        val personalExp = flowList.filter { it.type == CashFlowType.EXPENSE }.sumOf { it.amount }
        val personalInc = flowList.filter { it.type == CashFlowType.INCOME }.sumOf { it.amount }

        FinancialSummaryMetrics(
            liquidPocketCashTotal = pocket,
            bankAccountsTotal = bank,
            marketInvestmentsTotal = market,
            globalAssetsTotal = global,
            totalDebtReceivable = receivable,
            totalDebtPayable = payable,
            totalRevenue = rev,
            totalPurchases = purchases,
            totalSellingCosts = sellingCosts,
            totalPersonalExpenses = personalExp,
            totalPersonalIncome = personalInc
        )
    }

    init {
        scope.launch {
            checkAndSeedInitialMoneyData()
        }
    }

    suspend fun checkAndSeedInitialMoneyData() {
        val existingAccounts = accounts.first()
        val hasOldMockSeed = existingAccounts.any { it.id == "acc_pocket_1" }
        if (existingAccounts.isEmpty() || hasOldMockSeed) {
            resetToCleanSlate()
        }
    }

    suspend fun resetToCleanSlate() {
        // Purge any pre-populated examples or legacy demo data
        val existingAcc = accounts.first()
        for (a in existingAcc) {
            moneyDao.deleteAccountById(a.id)
        }
        val existingFlows = cashFlowEntries.first()
        for (f in existingFlows) {
            moneyDao.deleteCashFlowEntryById(f.id)
        }
        val existingInv = investments.first()
        for (i in existingInv) {
            moneyDao.deleteInvestmentById(i.id)
        }
        val existingVaults = vaults.first()
        for (v in existingVaults) {
            moneyDao.deleteVaultById(v.id)
        }
        val existingDebts = debts.first()
        for (d in existingDebts) {
            DebtAlarmScheduler.cancelDebtAlarm(context, d.id)
            moneyDao.deleteDebtCreditRecordById(d.id)
        }

        // Initialize 4 core account categories at exactly ₹0.00
        val cleanAccounts = listOf(
            MoneyAccount(
                id = "acc_pocket_clean",
                name = "Liquid Pocket Cash",
                institution = "Cash in Hand",
                category = AccountCategory.POCKET_CASH,
                balance = 0.0,
                currency = "INR",
                notes = "Physical currency"
            ),
            MoneyAccount(
                id = "acc_bank_clean",
                name = "Primary Bank Account",
                institution = "Bank Accounts",
                category = AccountCategory.BANK,
                balance = 0.0,
                currency = "INR",
                notes = "Bank holdings"
            ),
            MoneyAccount(
                id = "acc_invest_clean",
                name = "Market Investments",
                institution = "Demat / Brokerage",
                category = AccountCategory.MARKET_INVESTMENT,
                balance = 0.0,
                currency = "INR",
                notes = "Equities, funds, gold & digital assets"
            ),
            MoneyAccount(
                id = "acc_asset_clean",
                name = "Global Assets & Property",
                institution = "Real Estate & Commercial Equity",
                category = AccountCategory.GLOBAL_ASSET,
                balance = 0.0,
                currency = "INR",
                notes = "Physical property and business equity"
            )
        )
        moneyDao.insertAccounts(cleanAccounts.map { it.toEntity() })
        _aiInsights.value = emptyList()
    }

    // --- Actions ---

    suspend fun addCashFlowEntry(entry: CashFlowEntry) {
        moneyDao.insertCashFlowEntry(entry.toEntity())
        refreshAiInsights()
    }

    suspend fun deleteCashFlowEntry(id: String) {
        moneyDao.deleteCashFlowEntryById(id)
        refreshAiInsights()
    }

    suspend fun updateAccountBalance(accountId: String, newBalance: Double) {
        moneyDao.updateAccountBalance(accountId, newBalance, System.currentTimeMillis())
        refreshAiInsights()
    }

    suspend fun addOrUpdateAccount(account: MoneyAccount) {
        moneyDao.insertAccount(account.toEntity())
        refreshAiInsights()
    }

    suspend fun addOrUpdateInvestment(asset: InvestmentAsset) {
        moneyDao.insertInvestment(asset.toEntity())
        refreshAiInsights()
    }

    suspend fun deleteInvestment(assetId: String) {
        moneyDao.deleteInvestmentById(assetId)
        refreshAiInsights()
    }

    suspend fun updateRentStatus(assetId: String, status: RentCollectionStatus) {
        moneyDao.updateRentStatus(assetId, status.name)
    }

    suspend fun depositToVault(vaultId: String, depositAmount: Double) {
        val vaultList = vaults.first()
        val currentVault = vaultList.find { it.id == vaultId } ?: return
        val newAmount = currentVault.currentAmount + depositAmount
        moneyDao.updateVaultAmount(vaultId, newAmount)
        refreshAiInsights()
    }

    suspend fun withdrawFromVault(vaultId: String, withdrawAmount: Double) {
        val vaultList = vaults.first()
        val currentVault = vaultList.find { it.id == vaultId } ?: return
        val newAmount = (currentVault.currentAmount - withdrawAmount).coerceAtLeast(0.0)
        moneyDao.updateVaultAmount(vaultId, newAmount)
        refreshAiInsights()
    }

    suspend fun addOrUpdateVault(vault: SavingsVault) {
        moneyDao.insertVault(vault.toEntity())
        refreshAiInsights()
    }

    suspend fun deleteVault(vaultId: String) {
        moneyDao.deleteVaultById(vaultId)
        refreshAiInsights()
    }

    suspend fun addDebtCreditRecord(record: DebtCreditRecord) {
        moneyDao.insertDebtCreditRecord(record.toEntity())
        if (record.hasReminderAlarm) {
            DebtAlarmScheduler.scheduleDebtAlarm(context, record)
        }
        refreshAiInsights()
    }

    suspend fun recordDebtRepayment(recordId: String, repaymentAmount: Double) {
        val allDebts = debts.first()
        val record = allDebts.find { it.id == recordId } ?: return
        val newRemaining = (record.remainingAmount - repaymentAmount).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0) DebtCreditStatus.SETTLED else DebtCreditStatus.PARTIALLY_SETTLED
        moneyDao.updateDebtRepayment(recordId, newRemaining, newStatus.name)

        if (newStatus == DebtCreditStatus.SETTLED) {
            DebtAlarmScheduler.cancelDebtAlarm(context, recordId)
        }
        refreshAiInsights()
    }

    suspend fun toggleDebtAlarm(recordId: String, enabled: Boolean) {
        moneyDao.toggleDebtAlarm(recordId, enabled)
        val allDebts = debts.first()
        val record = allDebts.find { it.id == recordId } ?: return
        if (enabled) {
            DebtAlarmScheduler.scheduleDebtAlarm(context, record.copy(hasReminderAlarm = true))
        } else {
            DebtAlarmScheduler.cancelDebtAlarm(context, recordId)
        }
    }

    suspend fun markCalendarSynced(recordId: String) {
        moneyDao.updateCalendarSynced(recordId, true)
    }

    suspend fun deleteDebtCreditRecord(recordId: String) {
        DebtAlarmScheduler.cancelDebtAlarm(context, recordId)
        moneyDao.deleteDebtCreditRecordById(recordId)
        refreshAiInsights()
    }

    suspend fun refreshAiInsights(customQuestion: String? = null) {
        _isGeneratingAi.value = true
        try {
            val acc = accounts.first()
            val flows = cashFlowEntries.first()
            val inv = investments.first()
            val v = vaults.first()
            val d = debts.first()

            val insights = FinancialAiInsightsEngine.generateInsights(
                accounts = acc,
                cashFlows = flows,
                investments = inv,
                vaults = v,
                debts = d,
                customQuestion = customQuestion
            )
            _aiInsights.value = insights
        } catch (e: Exception) {
            // fallback
            _aiInsights.value = FinancialAiInsightsEngine.computeAlgorithmicInsights(
                accounts.first(),
                cashFlowEntries.first(),
                investments.first(),
                vaults.first(),
                debts.first(),
                customQuestion
            )
        } finally {
            _isGeneratingAi.value = false
        }
    }
}
