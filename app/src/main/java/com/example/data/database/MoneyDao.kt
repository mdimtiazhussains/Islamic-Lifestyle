package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CashFlowEntryEntity
import com.example.data.model.DebtCreditRecordEntity
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.MoneyAccountEntity
import com.example.data.model.SavingsVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDao {

    // --- Accounts ---
    @Query("SELECT * FROM money_accounts ORDER BY category ASC, balance DESC")
    fun getAllAccounts(): Flow<List<MoneyAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: MoneyAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<MoneyAccountEntity>)

    @Update
    suspend fun updateAccount(account: MoneyAccountEntity)

    @Query("UPDATE money_accounts SET balance = :newBalance, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateAccountBalance(id: String, newBalance: Double, timestamp: Long)

    @Query("DELETE FROM money_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    // --- Cash Flow & Business Ledger ---
    @Query("SELECT * FROM cash_flow_entries ORDER BY date DESC, timestamp DESC")
    fun getAllCashFlowEntries(): Flow<List<CashFlowEntryEntity>>

    @Query("SELECT * FROM cash_flow_entries WHERE isBusiness = 1 ORDER BY date DESC, timestamp DESC")
    fun getBusinessCashFlowEntries(): Flow<List<CashFlowEntryEntity>>

    @Query("SELECT * FROM cash_flow_entries WHERE date = :date ORDER BY timestamp DESC")
    fun getCashFlowEntriesForDate(date: String): Flow<List<CashFlowEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashFlowEntry(entry: CashFlowEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashFlowEntries(entries: List<CashFlowEntryEntity>)

    @Query("DELETE FROM cash_flow_entries WHERE id = :id")
    suspend fun deleteCashFlowEntryById(id: String)

    // --- Investment Portfolio & Rentals ---
    @Query("SELECT * FROM investment_assets ORDER BY currentValue DESC")
    fun getAllInvestments(): Flow<List<InvestmentAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(asset: InvestmentAssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestments(assets: List<InvestmentAssetEntity>)

    @Query("UPDATE investment_assets SET rentStatus = :status WHERE id = :id")
    suspend fun updateRentStatus(id: String, status: String)

    @Query("DELETE FROM investment_assets WHERE id = :id")
    suspend fun deleteInvestmentById(id: String)

    // --- Savings Vaults ---
    @Query("SELECT * FROM savings_vaults ORDER BY targetAmount DESC")
    fun getAllVaults(): Flow<List<SavingsVaultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: SavingsVaultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaults(vaults: List<SavingsVaultEntity>)

    @Query("UPDATE savings_vaults SET currentAmount = :newAmount WHERE id = :id")
    suspend fun updateVaultAmount(id: String, newAmount: Double)

    @Query("DELETE FROM savings_vaults WHERE id = :id")
    suspend fun deleteVaultById(id: String)

    // --- Granular Debt & Credit Tracker ---
    @Query("SELECT * FROM debt_credit_records ORDER BY dueDate ASC")
    fun getAllDebtCreditRecords(): Flow<List<DebtCreditRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtCreditRecord(record: DebtCreditRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtCreditRecords(records: List<DebtCreditRecordEntity>)

    @Query("UPDATE debt_credit_records SET remainingAmount = :newRemaining, status = :newStatus WHERE id = :id")
    suspend fun updateDebtRepayment(id: String, newRemaining: Double, newStatus: String)

    @Query("UPDATE debt_credit_records SET hasReminderAlarm = :enabled WHERE id = :id")
    suspend fun toggleDebtAlarm(id: String, enabled: Boolean)

    @Query("UPDATE debt_credit_records SET calendarSynced = :synced WHERE id = :id")
    suspend fun updateCalendarSynced(id: String, synced: Boolean)

    @Query("DELETE FROM debt_credit_records WHERE id = :id")
    suspend fun deleteDebtCreditRecordById(id: String)
}
