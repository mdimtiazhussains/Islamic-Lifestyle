package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AgendaTask
import com.example.data.model.CashFlowEntryEntity
import com.example.data.model.DailyPrayerRecord
import com.example.data.model.DebtCreditRecordEntity
import com.example.data.model.DiaryEntry
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.MoneyAccountEntity
import com.example.data.model.MorningPlan
import com.example.data.model.SavingsVaultEntity
import com.example.data.model.BayanClipEntity

@Database(
    entities = [
        AgendaTask::class,
        DiaryEntry::class,
        MorningPlan::class,
        DailyPrayerRecord::class,
        MoneyAccountEntity::class,
        CashFlowEntryEntity::class,
        InvestmentAssetEntity::class,
        SavingsVaultEntity::class,
        DebtCreditRecordEntity::class,
        BayanClipEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class LifestyleDatabase : RoomDatabase() {
    abstract fun lifestyleDao(): LifestyleDao
    abstract fun moneyDao(): MoneyDao
    abstract fun bayanDao(): BayanDao

    companion object {
        @Volatile
        private var INSTANCE: LifestyleDatabase? = null

        fun getDatabase(context: Context): LifestyleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifestyleDatabase::class.java,
                    "imtiaz_lifestyle.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
