package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgendaTask
import com.example.data.model.AiFinancialInsight
import com.example.data.model.CashFlowEntry
import com.example.data.model.DailyPrayerRecord
import com.example.data.model.DebtCreditRecord
import com.example.data.model.DiaryEntry
import com.example.data.model.FinancialSummaryMetrics
import com.example.data.model.InvestmentAsset
import com.example.data.model.MoneyAccount
import com.example.data.model.MorningPlan
import com.example.data.model.RentCollectionStatus
import com.example.data.model.SavingsVault
import com.example.data.repository.LifestyleRepository
import com.example.data.repository.MoneyRepository
import com.example.data.service.LivePrayerTimesResult
import com.example.data.service.LocationPrayerService
import com.example.util.DiaryPdfExporter
import com.example.util.PdfExportOptions
import com.example.util.PdfExportResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class LifestyleTab(val label: String, val iconLabel: String) {
    DUAL("View", "⚡"),
    MORNING("Routine", "🕌"),
    AGENDA("24 Hours", "📅"),
    DIARY("Diary", "📝"),
    MONEY("Money", "₹")
}

data class DiaryFilterCriteria(
    val query: String,
    val dateFilter: String?,
    val mood: String?,
    val scopeAll: Boolean
)

class LifestyleViewModel(private val repository: LifestyleRepository) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)

    // Current selected date
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Current active navigation tab
    private val _currentTab = MutableStateFlow(LifestyleTab.DUAL)
    val currentTab: StateFlow<LifestyleTab> = _currentTab.asStateFlow()

    // Scroll target within the Routine tab: "countdown", "zikr", "hydration", "namaz", or null
    private val _routineSectionTarget = MutableStateFlow<String?>(null)
    val routineSectionTarget: StateFlow<String?> = _routineSectionTarget.asStateFlow()

    fun navigateToRoutineSection(section: String) {
        _routineSectionTarget.value = section
        _currentTab.value = LifestyleTab.MORNING
    }

    fun clearRoutineSectionTarget() {
        _routineSectionTarget.value = null
    }

    // Category filter for Agenda
    private val _categoryFilter = MutableStateFlow("All")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    // Diary search, date filter, mood filter, and scope
    private val _diarySearchQuery = MutableStateFlow("")
    val diarySearchQuery: StateFlow<String> = _diarySearchQuery.asStateFlow()

    private val _diaryDateFilter = MutableStateFlow<String?>(null)
    val diaryDateFilter: StateFlow<String?> = _diaryDateFilter.asStateFlow()

    private val _diaryMoodFilter = MutableStateFlow<String?>(null)
    val diaryMoodFilter: StateFlow<String?> = _diaryMoodFilter.asStateFlow()

    private val _diarySearchScopeAll = MutableStateFlow(true)
    val diarySearchScopeAll: StateFlow<Boolean> = _diarySearchScopeAll.asStateFlow()

    // PDF Export State
    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _lastExportResult = MutableStateFlow<PdfExportResult?>(null)
    val lastExportResult: StateFlow<PdfExportResult?> = _lastExportResult.asStateFlow()

    // Dashboard card order for View tab (Countdown, Zikr, Hydration, Namaz Status)
    private val defaultCardOrder = listOf("countdown", "zikr", "hydration", "namaz")
    private val _dashboardCardOrder = MutableStateFlow<List<String>>(defaultCardOrder)
    val dashboardCardOrder: StateFlow<List<String>> = _dashboardCardOrder.asStateFlow()

    fun moveDashboardCardUp(cardId: String) {
        val current = _dashboardCardOrder.value.toMutableList()
        val index = current.indexOf(cardId)
        if (index > 0) {
            val item = current.removeAt(index)
            current.add(index - 1, item)
            _dashboardCardOrder.value = current
        }
    }

    fun moveDashboardCardDown(cardId: String) {
        val current = _dashboardCardOrder.value.toMutableList()
        val index = current.indexOf(cardId)
        if (index >= 0 && index < current.size - 1) {
            val item = current.removeAt(index)
            current.add(index + 1, item)
            _dashboardCardOrder.value = current
        }
    }

    fun resetDashboardCardOrder() {
        _dashboardCardOrder.value = defaultCardOrder
    }

    private val _exportErrorMessage = MutableStateFlow<String?>(null)
    val exportErrorMessage: StateFlow<String?> = _exportErrorMessage.asStateFlow()

    // Live GPS & Assabile.com Prayer Timings
    private val _livePrayerTimesResult = MutableStateFlow<LivePrayerTimesResult?>(null)
    val livePrayerTimesResult: StateFlow<LivePrayerTimesResult?> = _livePrayerTimesResult.asStateFlow()

    private val _isFetchingLivePrayerTimes = MutableStateFlow(false)
    val isFetchingLivePrayerTimes: StateFlow<Boolean> = _isFetchingLivePrayerTimes.asStateFlow()

    private val _livePrayerTimesError = MutableStateFlow<String?>(null)
    val livePrayerTimesError: StateFlow<String?> = _livePrayerTimesError.asStateFlow()

    // 15-Minute Morning Timer state
    private val _timerSeconds = MutableStateFlow(15 * 60) // 900 seconds
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    // Reactive streams from Room
    val tasks: StateFlow<List<AgendaTask>> = _selectedDate.flatMapLatest { date ->
        repository.getTasksForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaryEntries: StateFlow<List<DiaryEntry>> = _selectedDate.flatMapLatest { date ->
        repository.getDiaryEntriesForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiaryEntries: StateFlow<List<DiaryEntry>> = repository.getAllDiaryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val morningPlan: StateFlow<MorningPlan?> = _selectedDate.flatMapLatest { date ->
        repository.getMorningPlanForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val prayerRecord: StateFlow<DailyPrayerRecord?> = _selectedDate.flatMapLatest { date ->
        repository.getPrayerRecordForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Daily Habit Tracker state: completed habit IDs per date
    // Stored in a reactive map keyed by date string
    // Filtered agenda tasks
    val filteredTasks: StateFlow<List<AgendaTask>> = combine(tasks, _categoryFilter) { taskList, cat ->
        if (cat == "All") taskList else taskList.filter { it.category.equals(cat, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered diary entries (searchable by keyword, content, tags, gratitude, mood, or date)
    private val _diaryFilterCriteria = combine(
        _diarySearchQuery,
        _diaryDateFilter,
        _diaryMoodFilter,
        _diarySearchScopeAll
    ) { query, dateFilter, mood, scopeAll ->
        DiaryFilterCriteria(query, dateFilter, mood, scopeAll)
    }

    val filteredDiaryEntries: StateFlow<List<DiaryEntry>> = combine(
        allDiaryEntries,
        diaryEntries,
        _diaryFilterCriteria
    ) { allEntries, currentDayEntries, criteria ->
        val baseList = if (criteria.scopeAll || criteria.query.isNotBlank() || criteria.dateFilter != null) {
            allEntries
        } else {
            currentDayEntries
        }
        baseList.filter { entry ->
            val matchesDate = if (criteria.dateFilter != null) {
                entry.date == criteria.dateFilter
            } else {
                true
            }

            val matchesQuery = if (criteria.query.isBlank()) {
                true
            } else {
                val q = criteria.query.trim()
                entry.title.contains(q, ignoreCase = true) ||
                        entry.content.contains(q, ignoreCase = true) ||
                        entry.tags.contains(q, ignoreCase = true) ||
                        entry.gratitudeNotes.contains(q, ignoreCase = true) ||
                        entry.mood.contains(q, ignoreCase = true) ||
                        entry.date.contains(q, ignoreCase = true) ||
                        formatDateForSearch(entry.date).contains(q, ignoreCase = true)
            }

            val matchesMood = criteria.mood == null || entry.mood.equals(criteria.mood, ignoreCase = true)

            matchesDate && matchesQuery && matchesMood
        }.sortedWith(compareByDescending<DiaryEntry> { it.date }.thenByDescending { it.time })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed progress: completed vs total tasks
    val dailyProgress: StateFlow<Pair<Int, Int>> = tasks.flatMapLatest { list ->
        val total = list.size
        val completed = list.count { it.isCompleted }
        MutableStateFlow(Pair(completed, total))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))

    // Dates that have tasks recorded (past, present, or future)
    val datesWithTasks: StateFlow<Set<String>> = repository.getDatesWithTasks().flatMapLatest { list ->
        MutableStateFlow(list.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Dates that have diary reflections recorded
    val datesWithDiaryEntries: StateFlow<Set<String>> = repository.getDatesWithDiaryEntries().flatMapLatest { list ->
        MutableStateFlow(list.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allTasks: StateFlow<List<AgendaTask>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Money & Wealth Ecosystem State ---
    val moneyAccounts: StateFlow<List<MoneyAccount>> = repository.moneyRepository?.accounts
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val cashFlowEntries: StateFlow<List<CashFlowEntry>> = repository.moneyRepository?.cashFlowEntries
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val investmentAssets: StateFlow<List<InvestmentAsset>> = repository.moneyRepository?.investments
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val savingsVaults: StateFlow<List<SavingsVault>> = repository.moneyRepository?.vaults
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val debtCreditRecords: StateFlow<List<DebtCreditRecord>> = repository.moneyRepository?.debts
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ?: MutableStateFlow(emptyList())

    val aiFinancialInsights: StateFlow<List<AiFinancialInsight>> = repository.moneyRepository?.aiInsights
        ?: MutableStateFlow(emptyList())

    val isGeneratingAiFinancials: StateFlow<Boolean> = repository.moneyRepository?.isGeneratingAi
        ?: MutableStateFlow(false)

    val financialMetrics: StateFlow<FinancialSummaryMetrics> = repository.moneyRepository?.financialMetrics
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummaryMetrics())
        ?: MutableStateFlow(FinancialSummaryMetrics())

    // Selected filter for Cash Flow ledger (ALL, BUSINESS, PERSONAL)
    private val _cashFlowFilter = MutableStateFlow("ALL")
    val cashFlowFilter: StateFlow<String> = _cashFlowFilter.asStateFlow()

    fun setCashFlowFilter(filter: String) {
        _cashFlowFilter.value = filter
    }

    // Money Actions
    fun addCashFlowEntry(entry: CashFlowEntry) {
        viewModelScope.launch {
            repository.moneyRepository?.addCashFlowEntry(entry)
        }
    }

    fun deleteCashFlowEntry(id: String) {
        viewModelScope.launch {
            repository.moneyRepository?.deleteCashFlowEntry(id)
        }
    }

    fun updateAccountBalance(accountId: String, newBalance: Double) {
        viewModelScope.launch {
            repository.moneyRepository?.updateAccountBalance(accountId, newBalance)
        }
    }

    fun addOrUpdateAccount(account: MoneyAccount) {
        viewModelScope.launch {
            repository.moneyRepository?.addOrUpdateAccount(account)
        }
    }

    fun addOrUpdateInvestment(asset: InvestmentAsset) {
        viewModelScope.launch {
            repository.moneyRepository?.addOrUpdateInvestment(asset)
        }
    }

    fun deleteInvestment(assetId: String) {
        viewModelScope.launch {
            repository.moneyRepository?.deleteInvestment(assetId)
        }
    }

    fun updateRentStatus(assetId: String, status: RentCollectionStatus) {
        viewModelScope.launch {
            repository.moneyRepository?.updateRentStatus(assetId, status)
        }
    }

    fun depositToVault(vaultId: String, amount: Double) {
        viewModelScope.launch {
            repository.moneyRepository?.depositToVault(vaultId, amount)
        }
    }

    fun withdrawFromVault(vaultId: String, amount: Double) {
        viewModelScope.launch {
            repository.moneyRepository?.withdrawFromVault(vaultId, amount)
        }
    }

    fun addOrUpdateVault(vault: SavingsVault) {
        viewModelScope.launch {
            repository.moneyRepository?.addOrUpdateVault(vault)
        }
    }

    fun deleteVault(vaultId: String) {
        viewModelScope.launch {
            repository.moneyRepository?.deleteVault(vaultId)
        }
    }

    fun addDebtCreditRecord(record: DebtCreditRecord) {
        viewModelScope.launch {
            repository.moneyRepository?.addDebtCreditRecord(record)
        }
    }

    fun recordDebtRepayment(recordId: String, amount: Double) {
        viewModelScope.launch {
            repository.moneyRepository?.recordDebtRepayment(recordId, amount)
        }
    }

    fun toggleDebtAlarm(recordId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.moneyRepository?.toggleDebtAlarm(recordId, enabled)
        }
    }

    fun markCalendarSynced(recordId: String) {
        viewModelScope.launch {
            repository.moneyRepository?.markCalendarSynced(recordId)
        }
    }

    fun deleteDebtCreditRecord(recordId: String) {
        viewModelScope.launch {
            repository.moneyRepository?.deleteDebtCreditRecord(recordId)
        }
    }

    fun refreshAiInsights(customQuestion: String? = null) {
        viewModelScope.launch {
            repository.moneyRepository?.refreshAiInsights(customQuestion)
        }
    }

    init {
        viewModelScope.launch {
            // Keep Diary tab clean and empty: clear any template diary entries
            repository.clearTemplateDiaryEntries()

            val todayStr = dateFormat.format(Date())
            val existing = repository.getMorningPlanSync(todayStr)
            if (existing == null) {
                repository.seedTemplateDay(todayStr)
            }
        }
    }

    fun setTab(tab: LifestyleTab) {
        _currentTab.value = tab
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun setDiarySearchQuery(query: String) {
        _diarySearchQuery.value = query
    }

    fun setDiaryDateFilter(date: String?) {
        _diaryDateFilter.value = date
    }

    fun setDiaryMoodFilter(mood: String?) {
        _diaryMoodFilter.value = mood
    }

    fun setDiarySearchScopeAll(all: Boolean) {
        _diarySearchScopeAll.value = all
    }

    fun clearDiaryFilters() {
        _diarySearchQuery.value = ""
        _diaryDateFilter.value = null
        _diaryMoodFilter.value = null
    }

    fun formatDateForSearch(dateStr: String): String {
        return try {
            val parsed = dateFormat.parse(dateStr)
            if (parsed != null) {
                SimpleDateFormat("MMMM d yyyy MMM d EEEE", Locale.US).format(parsed)
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    // --- PDF Export Feature ---
    fun exportDiaryToPdf(
        context: Context,
        entries: List<DiaryEntry>,
        options: PdfExportOptions = PdfExportOptions(),
        onComplete: ((Result<PdfExportResult>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isExportingPdf.value = true
            _exportErrorMessage.value = null
            try {
                val exporter = DiaryPdfExporter(context)
                val result = exporter.exportToPdf(entries, options)
                if (result.isSuccess) {
                    _lastExportResult.value = result.getOrNull()
                } else {
                    _exportErrorMessage.value = result.exceptionOrNull()?.message ?: "Export failed"
                }
                _isExportingPdf.value = false
                onComplete?.invoke(result)
            } catch (e: Exception) {
                _isExportingPdf.value = false
                _exportErrorMessage.value = e.message ?: "Export failed"
                onComplete?.invoke(Result.failure(e))
            }
        }
    }

    fun clearLastExportResult() {
        _lastExportResult.value = null
        _exportErrorMessage.value = null
    }

    // --- Daily Prayers, Zikr & Tilawat Tracker ---
    fun savePrayerRecord(
        record: DailyPrayerRecord,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val updated = record.copy(updatedAt = timestamp)
                repository.savePrayerRecord(updated)
                onComplete?.invoke(true, "Daily prayer & spiritual record saved!")
            } catch (e: Exception) {
                onComplete?.invoke(false, "Failed to save record: ${e.message}")
            }
        }
    }

    /**
     * Fetches live, dynamic prayer timings from assabile.com using user GPS coordinates
     * or specified latitude/longitude, and synchronizes the daily prayer record.
     */
    fun syncPrayerTimesFromAssabile(
        context: Context,
        latitude: Double? = null,
        longitude: Double? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isFetchingLivePrayerTimes.value = true
            _livePrayerTimesError.value = null
            try {
                val service = LocationPrayerService(context.applicationContext)
                val result = service.fetchLivePrayerTimes(latitude, longitude)
                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    _livePrayerTimesResult.value = data

                    // Dynamically update the current date's prayer record without fixed values
                    val currentDate = _selectedDate.value
                    val currentRecord = prayerRecord.value ?: DailyPrayerRecord(date = currentDate)
                    val updated = currentRecord.copy(
                        fajrTime = data.fajr,
                        dhuhrTime = data.dhuhr,
                        asrTime = data.asr,
                        maghribTime = data.maghrib,
                        ishaTime = data.isha,
                        updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                    )
                    repository.savePrayerRecord(updated)
                    val msg = "✓ Live timings for ${data.cityName} (${data.countryName}) updated from assabile.com!"
                    onComplete?.invoke(true, msg)
                } else {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to connect to assabile.com"
                    _livePrayerTimesError.value = err
                    onComplete?.invoke(false, err)
                }
            } catch (e: Exception) {
                val err = "Error fetching from assabile.com: ${e.localizedMessage}"
                _livePrayerTimesError.value = err
                onComplete?.invoke(false, err)
            } finally {
                _isFetchingLivePrayerTimes.value = false
            }
        }
    }

    fun changeDate(offsetDays: Int) {
        try {
            val cal = Calendar.getInstance()
            val parsed = dateFormat.parse(_selectedDate.value) ?: Date()
            cal.time = parsed
            cal.add(Calendar.DAY_OF_YEAR, offsetDays)
            val newDateStr = dateFormat.format(cal.time)
            _selectedDate.value = newDateStr

            // Ensure template exists if brand new day
            viewModelScope.launch {
                val existing = repository.getMorningPlanSync(newDateStr)
                if (existing == null && offsetDays == 0) {
                    repository.seedTemplateDay(newDateStr)
                }
            }
        } catch (e: Exception) {
            // keep current date
        }
    }

    fun selectToday() {
        _selectedDate.value = dateFormat.format(Date())
    }

    fun setDate(newDate: String) {
        _selectedDate.value = newDate
        viewModelScope.launch {
            val existing = repository.getMorningPlanSync(newDate)
            if (existing == null && newDate == dateFormat.format(Date())) {
                repository.seedTemplateDay(newDate)
            }
        }
    }

    fun getMonthYearString(): String {
        return try {
            val parsed = dateFormat.parse(_selectedDate.value) ?: Date()
            SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
        } catch (e: Exception) {
            ""
        }
    }

    fun isSelectedDateToday(): Boolean {
        return _selectedDate.value == dateFormat.format(Date())
    }

    fun getFormattedSelectedDate(): String {
        return try {
            val parsed = dateFormat.parse(_selectedDate.value) ?: Date()
            val todayStr = dateFormat.format(Date())
            if (_selectedDate.value == todayStr) {
                "Today, ${SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed)}"
            } else {
                displayFormat.format(parsed)
            }
        } catch (e: Exception) {
            _selectedDate.value
        }
    }

    // --- Task Actions ---
    fun toggleTask(task: AgendaTask) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    fun saveTask(task: AgendaTask) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun deleteTask(task: AgendaTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // --- Diary Save Status & Feedback ---
    private val _isSavingDiary = MutableStateFlow(false)
    val isSavingDiary: StateFlow<Boolean> = _isSavingDiary.asStateFlow()

    private val _lastDiarySaveStatus = MutableStateFlow<String?>(null)
    val lastDiarySaveStatus: StateFlow<String?> = _lastDiarySaveStatus.asStateFlow()

    fun clearLastDiarySaveStatus() {
        _lastDiarySaveStatus.value = null
    }

    // --- Diary Actions ---
    fun saveDiaryEntry(entry: DiaryEntry, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isSavingDiary.value = true
            try {
                val cloudSaved = repository.insertDiaryEntry(entry)
                val statusText = if (cloudSaved) {
                    "✓ Reflection saved and synced to Cloud Firestore!"
                } else {
                    "✓ Reflection saved locally. (Sign in with Google to backup to Cloud Firestore)"
                }
                _lastDiarySaveStatus.value = statusText
                onComplete?.invoke(cloudSaved, statusText)
            } catch (e: Exception) {
                val err = "Error saving reflection: ${e.localizedMessage ?: "Unknown error"}"
                _lastDiarySaveStatus.value = err
                onComplete?.invoke(false, err)
            } finally {
                _isSavingDiary.value = false
            }
        }
    }

    fun syncDiaryFromCloud(date: String = _selectedDate.value, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val count = repository.syncRemoteDiaryEntriesForDate(date)
                onComplete?.invoke(count)
            } catch (e: Exception) {
                onComplete?.invoke(0)
            }
        }
    }

    fun deleteDiaryEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            repository.deleteDiaryEntry(entry)
        }
    }

    // --- Morning Routine Actions ---
    fun updateWaterGlasses(count: Int) {
        viewModelScope.launch {
            repository.updateWaterGlasses(_selectedDate.value, count.coerceIn(0, 8))
        }
    }

    fun saveMorningDraft(plan: MorningPlan) {
        viewModelScope.launch {
            repository.saveMorningPlan(plan.copy(date = _selectedDate.value))
        }
    }

    fun completeMorningRoutine(plan: MorningPlan) {
        viewModelScope.launch {
            repository.completeMorningRoutineAndPush(plan.copy(date = _selectedDate.value))
            _currentTab.value = LifestyleTab.DUAL
        }
    }

    // --- Reset Template ---
    fun resetToSampleTemplate() {
        viewModelScope.launch {
            repository.seedTemplateDay(_selectedDate.value)
        }
    }

    // --- Cloud Sync & Cross-Device State ---
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _lastCloudSyncStatus = MutableStateFlow<String?>(null)
    val lastCloudSyncStatus: StateFlow<String?> = _lastCloudSyncStatus.asStateFlow()

    fun onUserAuthenticated(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            repository.startRealtimeCloudSync(viewModelScope)
            syncBidirectional()
        } else {
            repository.stopRealtimeCloudSync()
        }
    }

    fun startRealtimeSync() {
        repository.startRealtimeCloudSync(viewModelScope)
    }

    fun stopRealtimeSync() {
        repository.stopRealtimeCloudSync()
    }

    fun syncBidirectional(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                // 1. Download latest from Cloud Firestore (tasks, diary reflections, morning plans)
                val summary = repository.syncAllFromCloud()

                // 2. Upload any local items to Cloud Firestore
                tasks.value.forEach { repository.insertTask(it) }
                diaryEntries.value.forEach { repository.insertDiaryEntry(it) }
                morningPlan.value?.let { repository.saveMorningPlan(it) }
                repository.syncDailyProgressToCloud(_selectedDate.value)

                val msg = if (summary.totalSynced > 0) {
                    "✓ Synced ${summary.tasksSynced} tasks & ${summary.diaryEntriesSynced} reflections across devices!"
                } else {
                    "✓ Multi-device sync complete. Diary & schedule are securely synced."
                }
                _lastCloudSyncStatus.value = msg
                onComplete?.invoke(true, msg)
            } catch (e: Exception) {
                val err = "Sync error: ${e.localizedMessage ?: "Network or permission error"}"
                _lastCloudSyncStatus.value = err
                onComplete?.invoke(false, err)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun syncAllToCloud(onComplete: (Boolean) -> Unit = {}) {
        syncBidirectional { success, _ ->
            onComplete(success)
        }
    }

    // --- Evening Review ---
    fun completeEveningReview(
        wins: String,
        lessons: String,
        rating: Int,
        carryOverPending: Boolean
    ) {
        viewModelScope.launch {
            val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val reviewContent = buildString {
                append("🌙 Daily Evening Retrospective (Rating: $rating/5 ★)\n\n")
                if (wins.isNotBlank()) {
                    append("Key Wins & Victories:\n$wins\n\n")
                }
                if (lessons.isNotBlank()) {
                    append("Lessons Learned & Observations:\n$lessons\n\n")
                }
                append("Evening review finalized. Grateful for progress made today.")
            }
            val entry = DiaryEntry(
                id = "diary_evening_${_selectedDate.value}_${System.currentTimeMillis()}",
                date = _selectedDate.value,
                time = timeStr,
                title = "🌙 Evening Retrospective & Wins",
                content = reviewContent,
                mood = if (rating >= 4) "Grateful" else "Reflective",
                energy = rating.coerceIn(1, 5),
                tags = "#evening, #wins, #reflection, #review",
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            repository.insertDiaryEntry(entry)

            if (carryOverPending) {
                // Roll pending tasks into tomorrow
                val cal = Calendar.getInstance()
                val parsed = dateFormat.parse(_selectedDate.value) ?: Date()
                cal.time = parsed
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowStr = dateFormat.format(cal.time)

                tasks.value.filter { !it.isCompleted }.forEach { pendingTask ->
                    val rolled = pendingTask.copy(
                        id = "task_rolled_${tomorrowStr}_${System.currentTimeMillis() % 10000}",
                        date = tomorrowStr,
                        orderIndex = pendingTask.orderIndex + 10,
                        description = "${pendingTask.description} (Rolled from ${_selectedDate.value})".trim()
                    )
                    repository.insertTask(rolled)
                }
            }
        }
    }

    // --- 15-Minute Countdown Timer ---
    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _timerSeconds.value = _timerSeconds.value - 1
            }
            _isTimerRunning.value = false
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerSeconds.value = 15 * 60
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class LifestyleViewModelFactory(private val repository: LifestyleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifestyleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifestyleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
