package com.example

import com.example.data.model.AccountCategory
import com.example.data.model.CashFlowEntry
import com.example.data.model.CashFlowType
import com.example.data.model.DebtCreditRecord
import com.example.data.model.DebtCreditStatus
import com.example.data.model.DebtCreditType
import com.example.data.model.FinancialSummaryMetrics
import com.example.data.model.InsightCategory
import com.example.data.model.InvestmentAsset
import com.example.data.model.InvestmentAssetCategory
import com.example.data.model.MoneyAccount
import com.example.data.model.RentCollectionStatus
import com.example.data.model.SavingsVault
import com.example.data.model.SavingsVaultCategory
import com.example.service.FinancialAiInsightsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyEcosystemUnitTest {

    @Test
    fun testUnifiedNetWorthCalculation() {
        val metrics = FinancialSummaryMetrics(
            liquidPocketCashTotal = 3250.0,
            bankAccountsTotal = 86900.0,
            marketInvestmentsTotal = 69400.0,
            globalAssetsTotal = 286800.0,
            totalDebtReceivable = 8900.0,
            totalDebtPayable = 3400.0,
            totalRevenue = 29300.0,
            totalPurchases = 10650.0,
            totalSellingCosts = 2750.0,
            totalPersonalExpenses = 780.0,
            totalPersonalIncome = 1200.0
        )

        // Total Assets = Pocket (3250) + Bank (86900) + Investments (69400) + Global (286800) + Receivable (8900) = 455250
        assertEquals(455250.0, metrics.totalAssets, 0.01)

        // Liabilities = Payable (3400)
        assertEquals(3400.0, metrics.totalLiabilities, 0.01)

        // Net Worth = Assets - Liabilities = 451850
        assertEquals(451850.0, metrics.netWorth, 0.01)

        // Commercial Gross Profit = Revenue (29300) - Purchases (10650) = 18650
        assertEquals(18650.0, metrics.commercialGrossProfit, 0.01)

        // Commercial Net Profit/Loss = Revenue (29300) - (Purchases 10650 + Selling Costs 2750) = 15900
        assertEquals(15900.0, metrics.commercialNetProfitOrLoss, 0.01)

        // Commercial Margin % = (15900 / 29300) * 100 = 54.266%
        assertTrue(metrics.commercialMarginPercent > 54.0)
    }

    @Test
    fun testSavingsVaultMilestoneProgression() {
        val childrenVault = SavingsVault(
            id = "v1",
            name = "Children's Future Trust",
            category = SavingsVaultCategory.CHILDRENS_FUTURE,
            currentAmount = 24500.0,
            targetAmount = 60000.0,
            monthlyTargetContribution = 1200.0,
            targetDate = "2032-12-31"
        )

        assertEquals(40, childrenVault.progressPercent)
        assertEquals(24500.0 / 60000.0, childrenVault.progressFraction.toDouble(), 0.01)
    }

    @Test
    fun testDebtCreditPayoffProgress() {
        val record = DebtCreditRecord(
            id = "d1",
            contactName = "Tariq Khan",
            type = DebtCreditType.LENT_OUT,
            originalAmount = 6000.0,
            remainingAmount = 3800.0,
            dueDate = "2026-11-01",
            interestOrTerms = "30-day net"
        )

        // Paid = 6000 - 3800 = 2200
        assertEquals(2200.0, record.amountPaid, 0.01)
        // 2200 / 6000 = 36%
        assertEquals(36, record.payoffPercentage)
    }

    @Test
    fun testFinancialAiInsightsEngineRecommendations() {
        val accounts = listOf(
            MoneyAccount(
                id = "a1",
                name = "Pocket Cash",
                institution = "Wallet",
                category = AccountCategory.POCKET_CASH,
                balance = 3250.0
            ),
            MoneyAccount(
                id = "a2",
                name = "Bank Account",
                institution = "HBL",
                category = AccountCategory.BANK,
                balance = 25000.0
            )
        )

        val cashFlows = listOf(
            CashFlowEntry(
                id = "cf1",
                date = "2026-10-19",
                title = "Sales Volume",
                amount = 20000.0,
                type = CashFlowType.REVENUE,
                category = "Commercial Sales",
                isBusiness = true
            ),
            CashFlowEntry(
                id = "cf2",
                date = "2026-10-19",
                title = "Product Stock",
                amount = 8000.0,
                type = CashFlowType.PURCHASE,
                category = "Inventory",
                isBusiness = true
            ),
            CashFlowEntry(
                id = "cf3",
                date = "2026-10-19",
                title = "Logistics Fees",
                amount = 3000.0,
                type = CashFlowType.SELLING_COST,
                category = "Freight",
                isBusiness = true
            )
        )

        val vaults = listOf(
            SavingsVault(
                id = "v1",
                name = "Children's Future Trust",
                category = SavingsVaultCategory.CHILDRENS_FUTURE,
                currentAmount = 20000.0,
                targetAmount = 50000.0,
                monthlyTargetContribution = 1000.0,
                targetDate = "2030-01-01"
            )
        )

        val debts = listOf(
            DebtCreditRecord(
                id = "d1",
                contactName = "Apex Supplier",
                type = DebtCreditType.BORROWED,
                originalAmount = 5000.0,
                remainingAmount = 2000.0,
                dueDate = "2026-10-25",
                interestOrTerms = "Net-21"
            )
        )

        val insights = FinancialAiInsightsEngine.computeAlgorithmicInsights(
            accounts = accounts,
            cashFlows = cashFlows,
            investments = emptyList(),
            vaults = vaults,
            debts = debts
        )

        assertNotNull(insights)
        assertTrue(insights.isNotEmpty())

        // Should include Idle Cash conversion insight
        val idleCashInsight = insights.find { it.category == InsightCategory.IDLE_CASH }
        assertNotNull(idleCashInsight)
        assertTrue(idleCashInsight!!.actionRecommendation.contains("Children's Future") || idleCashInsight.actionRecommendation.contains("sweep"))

        // Should include Expense/Outflow forecast
        val forecastInsight = insights.find { it.category == InsightCategory.EXPENSE_FORECAST }
        assertNotNull(forecastInsight)

        // Should include Debt watchdog
        val debtInsight = insights.find { it.category == InsightCategory.DEBT_RISK }
        assertNotNull(debtInsight)
        assertTrue(debtInsight!!.summary.contains("Apex Supplier"))
    }
}
