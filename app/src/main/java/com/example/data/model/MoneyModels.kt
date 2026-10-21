package com.example.data.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Categories of wealth storage and assets in the unified net worth command center.
 */
enum class AccountCategory(val displayName: String, val iconEmoji: String) {
    POCKET_CASH("Liquid Pocket Cash", "💵"),
    BANK("Bank Accounts", "🏦"),
    MARKET_INVESTMENT("Market Investments", "📈"),
    GLOBAL_ASSET("Global Assets & Equity", "🌍")
}

data class MoneyAccount(
    val id: String,
    val name: String,
    val institution: String,
    val category: AccountCategory,
    val balance: Double,
    val currency: String = "USD",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedBalance: String
        get() = formatCurrency(balance)
}

/**
 * Commercial & Personal Cash Flow types.
 */
enum class CashFlowType(val label: String, val isOutflow: Boolean, val badgeColor: Long) {
    REVENUE("Business Revenue / Sales", false, 0xFF2E7D32),
    EXPENSE("Personal Spending", true, 0xFFE53935),
    PURCHASE("Commercial Purchase / Stock", true, 0xFFD81B60),
    SELLING_COST("Selling Cost / Logistics / Fees", true, 0xFFFB8C00),
    INCOME("Personal Salary / Inflow", false, 0xFF1E88E5)
}

data class CashFlowEntry(
    val id: String,
    val date: String,
    val title: String,
    val amount: Double,
    val type: CashFlowType,
    val category: String,
    val isBusiness: Boolean,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedAmount: String
        get() = "${if (type.isOutflow) "-" else "+"}${formatCurrency(amount)}"
}

/**
 * Investment categories including liquid markets, metals, real estate, and business equity.
 */
enum class InvestmentAssetCategory(val displayName: String, val iconEmoji: String) {
    STOCKS_FUNDS("Stocks & ETFs", "📊"),
    CRYPTO("Crypto & Digital Assets", "⚡"),
    REAL_ESTATE_RENTAL("Real Estate & Rentals", "🏢"),
    GOLD_METALS("Physical Gold & Bullion", "🪙"),
    BUSINESS_EQUITY("Commercial Business Equity", "🏭")
}

enum class RentCollectionStatus(val label: String, val colorHex: Long) {
    NONE("Not Rental Property", 0xFF757575),
    COLLECTED("Rent Collected", 0xFF2E7D32),
    PENDING("Collection Due Soon", 0xFFF57C00),
    OVERDUE("Rent Overdue", 0xFFD32F2F)
}

data class InvestmentAsset(
    val id: String,
    val name: String,
    val category: InvestmentAssetCategory,
    val currentValue: Double,
    val investedAmount: Double,
    val growthPercentage: Double,
    val monthlyRentalIncome: Double = 0.0,
    val rentStatus: RentCollectionStatus = RentCollectionStatus.NONE,
    val notes: String = ""
) {
    val gainOrLoss: Double get() = currentValue - investedAmount
    val formattedCurrentValue: String get() = formatCurrency(currentValue)
    val formattedInvested: String get() = formatCurrency(investedAmount)
    val formattedGainLoss: String
        get() {
            val prefix = if (gainOrLoss >= 0) "+₹" else "-₹"
            val absVal = kotlin.math.abs(gainOrLoss)
            return "$prefix${formatNumber(absVal)} (${String.format(Locale.US, "%.1f", growthPercentage)}%)"
        }
}

/**
 * Explicit savings vault milestones:
 * Children's Future, Education Funds, Parental Support, Long-Term Retirement, Custom.
 */
enum class SavingsVaultCategory(val title: String, val defaultEmoji: String, val themeColorHex: Long) {
    CHILDRENS_FUTURE("Children's Future", "👶", 0xFF3F51B5),
    EDUCATION_FUNDS("Education Funds", "🎓", 0xFF00897B),
    PARENTAL_SUPPORT("Parental Support", "🧓", 0xFFD81B60),
    RETIREMENT("Long-Term Retirement", "🏖️", 0xFFF57C00),
    CUSTOM("Reserve & Emergency Vault", "🛡️", 0xFF43A047)
}

data class SavingsVault(
    val id: String,
    val name: String,
    val category: SavingsVaultCategory,
    val currentAmount: Double,
    val targetAmount: Double,
    val monthlyTargetContribution: Double,
    val targetDate: String,
    val milestoneNotes: String = "",
    val accentColorHex: Long = 0xFF2E7D32
) {
    val progressFraction: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val formattedCurrent: String get() = formatCurrency(currentAmount)
    val formattedTarget: String get() = formatCurrency(targetAmount)
    val formattedMonthlyTarget: String get() = "${formatCurrency(monthlyTargetContribution)} / mo"
}

/**
 * Granular Debt & Credit Tracker.
 * Money Lent (Receivable) vs Money Borrowed (Payable).
 */
enum class DebtCreditType(val label: String, val isReceivable: Boolean) {
    LENT_OUT("Money Lent (I will receive)", true),
    BORROWED("Money Borrowed (I must repay)", false)
}

enum class DebtCreditStatus(val label: String, val colorHex: Long) {
    ACTIVE("Active / In Progress", 0xFF1976D2),
    PARTIALLY_SETTLED("Partially Settled", 0xFFF57C00),
    SETTLED("Fully Paid & Settled", 0xFF388E3C),
    OVERDUE("Due Date Passed", 0xFFD32F2F)
}

data class DebtCreditRecord(
    val id: String,
    val contactName: String,
    val contactPhone: String = "",
    val type: DebtCreditType,
    val originalAmount: Double,
    val remainingAmount: Double,
    val dueDate: String,
    val interestOrTerms: String,
    val hasReminderAlarm: Boolean = true,
    val status: DebtCreditStatus = DebtCreditStatus.ACTIVE,
    val calendarSynced: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val amountPaid: Double get() = (originalAmount - remainingAmount).coerceAtLeast(0.0)
    val payoffFraction: Float
        get() = if (originalAmount > 0) (amountPaid / originalAmount).toFloat().coerceIn(0f, 1f) else 1f
    val payoffPercentage: Int get() = (payoffFraction * 100).toInt()

    val formattedOriginal: String get() = formatCurrency(originalAmount)
    val formattedRemaining: String get() = formatCurrency(remainingAmount)
    val formattedPaid: String get() = formatCurrency(amountPaid)
}

/**
 * AI Financial Insight recommendations.
 */
enum class InsightCategory(val label: String, val iconEmoji: String, val badgeColor: Long) {
    IDLE_CASH("Idle Cash Conversion", "⚡", 0xFF43A047),
    EXPENSE_FORECAST("Expense Forecast", "🔮", 0xFF8E24AA),
    BUDGET_OPTIMIZATION("Budget & Cost Reduction", "✂️", 0xFFFB8C00),
    WEALTH_GROWTH("Wealth Acceleration", "🚀", 0xFF1E88E5),
    DEBT_RISK("Credit & Debt Watchdog", "⚠️", 0xFFE53935)
}

data class AiFinancialInsight(
    val id: String,
    val title: String,
    val summary: String,
    val actionRecommendation: String,
    val category: InsightCategory,
    val impactEstimate: String = "",
    val isActionable: Boolean = true
)

/**
 * Comprehensive Financial Metrics across personal and commercial business sectors.
 */
data class FinancialSummaryMetrics(
    val liquidPocketCashTotal: Double = 0.0,
    val bankAccountsTotal: Double = 0.0,
    val marketInvestmentsTotal: Double = 0.0,
    val globalAssetsTotal: Double = 0.0,
    val totalDebtReceivable: Double = 0.0,
    val totalDebtPayable: Double = 0.0,

    // Commercial & Cashflow Metrics
    val totalRevenue: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalSellingCosts: Double = 0.0,
    val totalPersonalExpenses: Double = 0.0,
    val totalPersonalIncome: Double = 0.0
) {
    val totalAssets: Double
        get() = liquidPocketCashTotal + bankAccountsTotal + marketInvestmentsTotal + globalAssetsTotal + totalDebtReceivable

    val totalLiabilities: Double
        get() = totalDebtPayable

    val netWorth: Double
        get() = totalAssets - totalLiabilities

    // Commercial Net Profit/Loss: Revenue - (Purchases + Selling Costs)
    val commercialGrossProfit: Double
        get() = totalRevenue - totalPurchases

    val commercialNetProfitOrLoss: Double
        get() = totalRevenue - (totalPurchases + totalSellingCosts)

    val commercialMarginPercent: Double
        get() = if (totalRevenue > 0) (commercialNetProfitOrLoss / totalRevenue) * 100 else 0.0

    // Personal Net Cashflow
    val personalNetSavings: Double
        get() = totalPersonalIncome - totalPersonalExpenses

    val formattedNetWorth: String get() = formatCurrency(netWorth)
    val formattedAssets: String get() = formatCurrency(totalAssets)
    val formattedLiabilities: String get() = formatCurrency(totalLiabilities)
    val formattedPocketCash: String get() = formatCurrency(liquidPocketCashTotal)
    val formattedBankTotal: String get() = formatCurrency(bankAccountsTotal)
    val formattedInvestments: String get() = formatCurrency(marketInvestmentsTotal)
    val formattedGlobalAssets: String get() = formatCurrency(globalAssetsTotal)
    val formattedRevenue: String get() = formatCurrency(totalRevenue)
    val formattedPurchases: String get() = formatCurrency(totalPurchases)
    val formattedSellingCosts: String get() = formatCurrency(totalSellingCosts)
    val formattedCommercialNet: String get() = formatCurrency(commercialNetProfitOrLoss)
}

fun formatCurrency(amount: Double): String {
    val isNegative = amount < 0
    val absVal = kotlin.math.abs(amount)
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    val formattedStr = formatter.format(absVal)
    return if (isNegative) "-₹$formattedStr" else "₹$formattedStr"
}

fun formatNumber(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
