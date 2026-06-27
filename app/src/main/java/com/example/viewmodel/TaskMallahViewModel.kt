package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TaskMallahViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TaskMallahRepository(application, db)
    private val dao = db.taskMallahDao()

    // ==========================================
    // AUTH STATE
    // ==========================================
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _activeRole = MutableStateFlow("EARNER")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess: StateFlow<String?> = _authSuccess.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // ==========================================
    // ADMIN PROFIT POOL
    // ==========================================
    val adminProfitPool: StateFlow<Double> = repository.adminProfitPool

    // ==========================================
    // EARNER DATA
    // ==========================================
    private val _filteredTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val filteredTasks: StateFlow<List<TaskEntity>> = _filteredTasks.asStateFlow()

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _selectedPlatformFilter = MutableStateFlow<String?>(null)
    val selectedPlatformFilter: StateFlow<String?> = _selectedPlatformFilter.asStateFlow()

    private val _selectedTask = MutableStateFlow<TaskEntity?>(null)
    val selectedTask: StateFlow<TaskEntity?> = _selectedTask.asStateFlow()

    private val _selectedTaskCompletion = MutableStateFlow<CompletionEntity?>(null)
    val selectedTaskCompletion: StateFlow<CompletionEntity?> = _selectedTaskCompletion.asStateFlow()

    private val _userTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val userTransactions: StateFlow<List<TransactionEntity>> = _userTransactions.asStateFlow()

    // ==========================================
    // ADVERTISER DATA
    // ==========================================
    private val _advertiserCampaigns = MutableStateFlow<List<TaskEntity>>(emptyList())
    val advertiserCampaigns: StateFlow<List<TaskEntity>> = _advertiserCampaigns.asStateFlow()

    // ==========================================
    // ADMIN DATA - ALL
    // ==========================================
    private val _adminPendingCompletions = MutableStateFlow<List<CompletionEntity>>(emptyList())
    val adminPendingCompletions: StateFlow<List<CompletionEntity>> = _adminPendingCompletions.asStateFlow()

    private val _adminPendingKyc = MutableStateFlow<List<KycEntity>>(emptyList())
    val adminPendingKyc: StateFlow<List<KycEntity>> = _adminPendingKyc.asStateFlow()

    private val _adminPendingDeposits = MutableStateFlow<List<DepositRequestEntity>>(emptyList())
    val adminPendingDeposits: StateFlow<List<DepositRequestEntity>> = _adminPendingDeposits.asStateFlow()

    private val _adminPendingWithdrawals = MutableStateFlow<List<WithdrawalRequestEntity>>(emptyList())
    val adminPendingWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = _adminPendingWithdrawals.asStateFlow()

    private val _adminAllUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val adminAllUsers: StateFlow<List<UserEntity>> = _adminAllUsers.asStateFlow()

    private val _adminAllTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val adminAllTransactions: StateFlow<List<TransactionEntity>> = _adminAllTransactions.asStateFlow()

    // ALL Withdrawals (not just pending)
    private val _adminAllWithdrawals = MutableStateFlow<List<WithdrawalRequestEntity>>(emptyList())
    val adminAllWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = _adminAllWithdrawals.asStateFlow()

    // ALL Deposits (not just pending)
    private val _adminAllDeposits = MutableStateFlow<List<DepositRequestEntity>>(emptyList())
    val adminAllDeposits: StateFlow<List<DepositRequestEntity>> = _adminAllDeposits.asStateFlow()

    // ALL Tasks (for admin task management)
    private val _allTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val allTasks: StateFlow<List<TaskEntity>> = _allTasks.asStateFlow()

    // ==========================================
    // INIT
    // ==========================================
    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
            val user = repository.currentUser
            if (user != null) {
                _currentUser.value = user
                _activeRole.value = user.role
                startObservingData()
            }
        }
    }

    private fun startObservingData() {
        val user = _currentUser.value ?: return

        // Observe transactions for current user
        viewModelScope.launch {
            repository.getTransactionsFlow(user.id).collect {
                _userTransactions.value = it
            }
        }

        // Observe tasks
        viewModelScope.launch {
            repository.getTasksFlow().collect { tasks ->
                _allTasks.value = tasks
                applyTaskFilters(tasks)
            }
        }

        // Admin-only observations
        viewModelScope.launch {
            repository.getAllPendingCompletionsFlow().collect { _adminPendingCompletions.value = it }
        }
        viewModelScope.launch {
            repository.getAllPendingKycFlow().collect { _adminPendingKyc.value = it }
        }
        viewModelScope.launch {
            repository.getAllPendingDepositsFlow().collect { _adminPendingDeposits.value = it }
        }
        viewModelScope.launch {
            repository.getAllPendingWithdrawalsFlow().collect { _adminPendingWithdrawals.value = it }
        }
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { _adminAllUsers.value = it }
        }
        viewModelScope.launch {
            repository.getAllTransactionsFlow().collect { _adminAllTransactions.value = it }
        }

        // All withdrawals and deposits for Finance screen
        viewModelScope.launch {
            dao.getAllWithdrawalRequestsFlow().collect { _adminAllWithdrawals.value = it }
        }
        viewModelScope.launch {
            dao.getAllDepositRequestsFlow().collect { _adminAllDeposits.value = it }
        }
    }

    private fun applyTaskFilters(tasks: List<TaskEntity>) {
        val query = _taskSearchQuery.value
        val filter = _selectedPlatformFilter.value
        _filteredTasks.value = tasks.filter { task ->
            val matchesQuery = query.isEmpty() ||
                    task.campaignName.contains(query, true) ||
                    task.platform.contains(query, true) ||
                    task.taskType.contains(query, true)
            val matchesPlatform = filter == null || task.platform == filter
            matchesQuery && matchesPlatform && task.status == "ACTIVE"
        }
    }

    // ==========================================
    // AUTH FUNCTIONS
    // ==========================================
    fun registerUser(name: String, email: String, phone: String, password: String, cnic: String, referralCode: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _authError.value = null
            val result = repository.signup(name, email, phone, password, cnic, referralCode.ifBlank { null })
            _isProcessing.value = false
            result.onFailure { _authError.value = it.message }
        }
    }

    fun verifyOtpAndRegister(code: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _authError.value = null
            val result = repository.verifyOtpAndCompleteSignup(code)
            _isProcessing.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _activeRole.value = user.role
                startObservingData()
                _authSuccess.value = "Welcome, ${user.name}! Account ban gaya."
            }.onFailure { _authError.value = it.message }
        }
    }

    fun loginUser(identifier: String, password: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _authError.value = null
            val result = repository.login(identifier, password)
            _isProcessing.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _activeRole.value = user.role
                startObservingData()
                _authSuccess.value = "Login kamyab! Welcome ${user.name}"
            }.onFailure { _authError.value = it.message }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.loginWithGoogleToken(idToken)
            _isProcessing.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _activeRole.value = user.role
                startObservingData()
                _authSuccess.value = "Google login kamyab! Welcome ${user.name}"
            }.onFailure { _authError.value = it.message }
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _activeRole.value = "EARNER"
        _userTransactions.value = emptyList()
        _advertiserCampaigns.value = emptyList()
    }

    fun switchActiveRole(role: String) {
        viewModelScope.launch {
            repository.switchRole(role)
            _activeRole.value = role
        }
    }

    fun clearAuthMessages() {
        _authError.value = null
        _authSuccess.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // ==========================================
    // TASK SEARCH & FILTER
    // ==========================================
    fun updateSearchQuery(query: String) {
        _taskSearchQuery.value = query
        applyTaskFilters(_allTasks.value)
    }

    fun selectPlatformFilter(platform: String?) {
        _selectedPlatformFilter.value = platform
        applyTaskFilters(_allTasks.value)
    }

    fun selectTask(task: TaskEntity) {
        _selectedTask.value = task
        viewModelScope.launch {
            _selectedTaskCompletion.value = repository.getCompletionForTask(task.id)
        }
    }

    // ==========================================
    // EARNER FUNCTIONS
    // ==========================================
    fun submitTaskCompletionProof(taskId: String, screenshotPath: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.submitTaskProof(taskId, screenshotPath)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "Proof submit ho gaya! Admin review karega."
                _selectedTaskCompletion.value = repository.getCompletionForTask(taskId)
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun watchAdMobAd(adType: String) {
        viewModelScope.launch {
            val result = repository.watchAd(adType)
            result.onSuccess { amount ->
                val updatedUser = repository.currentUser
                _currentUser.value = updatedUser
                _toastMessage.value = "PKR ${"%.2f".format(amount)} milgaye! Ad se earning."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun submitWithdrawal(amount: Double, method: String, accountTitle: String, accountNo: String) {
        viewModelScope.launch {
            val result = repository.requestWithdrawal(amount, method, accountTitle, accountNo)
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Withdrawal request submit ho gayi. Admin process karega."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun submitKycDetails(cnicNo: String, frontPath: String, backPath: String) {
        viewModelScope.launch {
            val result = repository.submitKyc(cnicNo, frontPath, backPath)
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "KYC submit ho gayi. Admin verify karega."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    // ==========================================
    // ADVERTISER FUNCTIONS
    // ==========================================
    fun submitDepositPayment(amount: Double, method: String, proofPath: String) {
        viewModelScope.launch {
            val result = repository.submitDepositRequest(amount, method, proofPath)
            result.onSuccess {
                _toastMessage.value = "Deposit proof submit ho gaya. Admin verify karega."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun submitCampaign(
        platform: String,
        taskType: String,
        name: String,
        url: String,
        instructions: String,
        slots: Int,
        pricePerSlot: Double
    ) {
        viewModelScope.launch {
            val result = repository.createCampaign(platform, taskType, name, url, instructions, slots, pricePerSlot)
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Campaign create ho gaya! Earners ab dekhenge."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    // ==========================================
    // ADMIN FUNCTIONS — TASK REVIEWS
    // ==========================================
    fun approveOrRejectTaskCompletion(completionId: String, approve: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val result = repository.reviewTaskCompletion(completionId, approve, reason)
            result.onSuccess {
                _toastMessage.value = if (approve) "Task approved! Earner ko payment mil gayi." else "Task reject kar diya gaya."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun approveOrRejectKyc(userId: String, approve: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val result = repository.reviewKyc(userId, approve, reason)
            result.onSuccess {
                _toastMessage.value = if (approve) "KYC approved!" else "KYC rejected."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    // ==========================================
    // ADMIN FUNCTIONS — FINANCE
    // ==========================================
    fun processDepositRequest(requestId: String, approve: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val result = repository.processDeposit(requestId, approve, reason)
            result.onSuccess {
                _toastMessage.value = if (approve) "Deposit approve ho gaya! Balance add kar diya." else "Deposit reject kar diya."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun processWithdrawalRequest(requestId: String, approve: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val result = repository.processWithdrawal(requestId, approve, reason)
            result.onSuccess {
                _toastMessage.value = if (approve) "Withdrawal complete! User ko pesa bhej diya." else "Withdrawal reject. Balance wapas kar diya."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    // ==========================================
    // ADMIN FUNCTIONS — USER MANAGEMENT
    // ==========================================
    fun manualWalletAdjust(userId: String, amount: Double, reason: String) {
        viewModelScope.launch {
            val result = repository.adjustUserWallet(userId, amount, reason)
            result.onSuccess {
                _toastMessage.value = "Balance adjust ho gaya: PKR ${if (amount > 0) "+$amount" else "$amount"}"
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun moderateUserBan(userId: String, ban: Boolean, reason: String? = null) {
        viewModelScope.launch {
            val result = repository.banUser(userId, ban, reason)
            result.onSuccess {
                _toastMessage.value = if (ban) "User ban ho gaya." else "User ka ban hata diya gaya."
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    // DELETE USER — Remove completely from DB and Firebase
    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.deleteUserCompletely(userId)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "User delete ho gaya."
            }.onFailure { _toastMessage.value = "Delete mein masla: ${it.message}" }
        }
    }

    // ==========================================
    // ADMIN FUNCTIONS — TASK MANAGEMENT
    // ==========================================
    fun adminCreateTask(
        platform: String,
        taskType: String,
        name: String,
        url: String,
        instructions: String,
        slots: Int,
        userPayout: Double
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                id = "admin_task_" + UUID.randomUUID().toString().take(8),
                advertiserId = _currentUser.value?.id ?: "admin",
                campaignName = name,
                platform = platform,
                category = "Admin Created",
                taskType = taskType,
                targetUrl = url,
                instructions = instructions,
                advPricePkr = userPayout / 0.70,
                userPayoutPkr = userPayout,
                adminMarginPkr = (userPayout / 0.70) * 0.30,
                totalSlots = slots,
                status = "ACTIVE"
            )
            dao.insertTask(task)
            _toastMessage.value = "Task '$name' create ho gaya!"
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            dao.deleteTaskById(taskId)
            _toastMessage.value = "Task delete ho gaya."
        }
    }

    fun updateTaskStatus(taskId: String, status: String) {
        viewModelScope.launch {
            val task = dao.getTaskById(taskId) ?: return@launch
            task.status = status
            dao.updateTask(task)
            _toastMessage.value = "Task status update: $status"
        }
    }

    // ==========================================
    // SUBSCRIPTION
    // ==========================================
    fun buySubscription(tier: String) {
        viewModelScope.launch {
            val result = repository.buySubscription(tier)
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "$tier package activate ho gaya!"
            }.onFailure { _toastMessage.value = it.message }
        }
    }
}
