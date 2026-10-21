package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_accounts")
data class MoneyAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val institution: String,
    val category: String, // AccountCategory name
    val balance: Double,
    val currency: String = "USD",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

fun MoneyAccountEntity.toDomain(): MoneyAccount {
    val cat = try {
        AccountCategory.valueOf(category)
    } catch (e: Exception) {
        AccountCategory.BANK
    }
    return MoneyAccount(
        id = id,
        name = name,
        institution = institution,
        category = cat,
        balance = balance,
        currency = currency,
        notes = notes,
        updatedAt = updatedAt
    )
}

fun MoneyAccount.toEntity(): MoneyAccountEntity {
    return MoneyAccountEntity(
        id = id,
        name = name,
        institution = institution,
        category = category.name,
        balance = balance,
        currency = currency,
        notes = notes,
        updatedAt = updatedAt
    )
}

@Entity(tableName = "cash_flow_entries")
data class CashFlowEntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val title: String,
    val amount: Double,
    val type: String, // CashFlowType name
    val category: String,
    val isBusiness: Boolean,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

fun CashFlowEntryEntity.toDomain(): CashFlowEntry {
    val flowType = try {
        CashFlowType.valueOf(type)
    } catch (e: Exception) {
        CashFlowType.EXPENSE
    }
    return CashFlowEntry(
        id = id,
        date = date,
        title = title,
        amount = amount,
        type = flowType,
        category = category,
        isBusiness = isBusiness,
        notes = notes,
        timestamp = timestamp
    )
}

fun CashFlowEntry.toEntity(): CashFlowEntryEntity {
    return CashFlowEntryEntity(
        id = id,
        date = date,
        title = title,
        amount = amount,
        type = type.name,
        category = category,
        isBusiness = isBusiness,
        notes = notes,
        timestamp = timestamp
    )
}

@Entity(tableName = "investment_assets")
data class InvestmentAssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // InvestmentAssetCategory name
    val currentValue: Double,
    val investedAmount: Double,
    val growthPercentage: Double,
    val monthlyRentalIncome: Double = 0.0,
    val rentStatus: String = "NONE", // RentCollectionStatus name
    val notes: String = ""
)

fun InvestmentAssetEntity.toDomain(): InvestmentAsset {
    val cat = try {
        InvestmentAssetCategory.valueOf(category)
    } catch (e: Exception) {
        InvestmentAssetCategory.STOCKS_FUNDS
    }
    val status = try {
        RentCollectionStatus.valueOf(rentStatus)
    } catch (e: Exception) {
        RentCollectionStatus.NONE
    }
    return InvestmentAsset(
        id = id,
        name = name,
        category = cat,
        currentValue = currentValue,
        investedAmount = investedAmount,
        growthPercentage = growthPercentage,
        monthlyRentalIncome = monthlyRentalIncome,
        rentStatus = status,
        notes = notes
    )
}

fun InvestmentAsset.toEntity(): InvestmentAssetEntity {
    return InvestmentAssetEntity(
        id = id,
        name = name,
        category = category.name,
        currentValue = currentValue,
        investedAmount = investedAmount,
        growthPercentage = growthPercentage,
        monthlyRentalIncome = monthlyRentalIncome,
        rentStatus = rentStatus.name,
        notes = notes
    )
}

@Entity(tableName = "savings_vaults")
data class SavingsVaultEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // SavingsVaultCategory name
    val currentAmount: Double,
    val targetAmount: Double,
    val monthlyTargetContribution: Double,
    val targetDate: String,
    val milestoneNotes: String = "",
    val accentColorHex: Long = 0xFF2E7D32
)

fun SavingsVaultEntity.toDomain(): SavingsVault {
    val cat = try {
        SavingsVaultCategory.valueOf(category)
    } catch (e: Exception) {
        SavingsVaultCategory.CUSTOM
    }
    return SavingsVault(
        id = id,
        name = name,
        category = cat,
        currentAmount = currentAmount,
        targetAmount = targetAmount,
        monthlyTargetContribution = monthlyTargetContribution,
        targetDate = targetDate,
        milestoneNotes = milestoneNotes,
        accentColorHex = accentColorHex
    )
}

fun SavingsVault.toEntity(): SavingsVaultEntity {
    return SavingsVaultEntity(
        id = id,
        name = name,
        category = category.name,
        currentAmount = currentAmount,
        targetAmount = targetAmount,
        monthlyTargetContribution = monthlyTargetContribution,
        targetDate = targetDate,
        milestoneNotes = milestoneNotes,
        accentColorHex = accentColorHex
    )
}

@Entity(tableName = "debt_credit_records")
data class DebtCreditRecordEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactPhone: String = "",
    val type: String, // DebtCreditType name
    val originalAmount: Double,
    val remainingAmount: Double,
    val dueDate: String,
    val interestOrTerms: String,
    val hasReminderAlarm: Boolean = true,
    val status: String = "ACTIVE", // DebtCreditStatus name
    val calendarSynced: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

fun DebtCreditRecordEntity.toDomain(): DebtCreditRecord {
    val debtType = try {
        DebtCreditType.valueOf(type)
    } catch (e: Exception) {
        DebtCreditType.LENT_OUT
    }
    val debtStatus = try {
        DebtCreditStatus.valueOf(status)
    } catch (e: Exception) {
        DebtCreditStatus.ACTIVE
    }
    return DebtCreditRecord(
        id = id,
        contactName = contactName,
        contactPhone = contactPhone,
        type = debtType,
        originalAmount = originalAmount,
        remainingAmount = remainingAmount,
        dueDate = dueDate,
        interestOrTerms = interestOrTerms,
        hasReminderAlarm = hasReminderAlarm,
        status = debtStatus,
        calendarSynced = calendarSynced,
        notes = notes,
        createdAt = createdAt
    )
}

fun DebtCreditRecord.toEntity(): DebtCreditRecordEntity {
    return DebtCreditRecordEntity(
        id = id,
        contactName = contactName,
        contactPhone = contactPhone,
        type = type.name,
        originalAmount = originalAmount,
        remainingAmount = remainingAmount,
        dueDate = dueDate,
        interestOrTerms = interestOrTerms,
        hasReminderAlarm = hasReminderAlarm,
        status = status.name,
        calendarSynced = calendarSynced,
        notes = notes,
        createdAt = createdAt
    )
}
