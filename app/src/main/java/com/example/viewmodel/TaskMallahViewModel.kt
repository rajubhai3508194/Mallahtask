package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskMallahViewModel(private val repository: TaskMallahRepository) : ViewModel() {

    // --- Authentication States ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess: StateFlow<String?> = _authSuccess.asStateFlow()

    private val _activeRole = MutableStateFlow("EARNER")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    // Admin Profit Pool StateFlow
    val adminProfitPool: StateFlow<Double> = repository.adminProfitPool

    // OTP Navigation StateFlows
    private val _isOtpRequired = MutableStateFlow(false)
    val isOtpRequired: StateFlow<Boolean> = _isOtpRequired.asStateFlow()

    fun clearOtpRequired() {
        _isOtpRequired.value = false
    }

    // --- Earner Flows ---
    val allTasks: StateFlow<List<TaskEntity>> = repository.getTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _selectedPlatformFilter = MutableStateFlow<String?>(null)
    val selectedPlatformFilter: StateFlow<String?> = _selectedPlatformFilter.asStateFlow()

    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _taskSearchQuery,
        _selectedPlatformFilter
    ) { tasks, query, platform ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() || task.campaignName.contains(query, ignoreCase = true) || task.platform.contains(query, ignoreCase = true)
            val matchesPlatform = platform == null || task.platform.lowercase() == platform.lowercase()
            val isActive = task.status == "ACTIVE"
            matchesQuery && matchesPlatform && isActive
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTask = MutableStateFlow<TaskEntity?>(null)
    val selectedTask: StateFlow<TaskEntity?> = _selectedTask.asStateFlow()

    private val _selectedTaskCompletion = MutableStateFlow<CompletionEntity?>(null)
    val selectedTaskCompletion: StateFlow<CompletionEntity?> = _selectedTaskCompletion.asStateFlow()

    // Realtime transactions for logged in user
    val userTransactions: StateFlow<List<TransactionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getTransactionsFlow(user.id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime completions for logged in earner
    val earnerCompletions: StateFlow<List<CompletionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getCompletionsForEarnerFlow(user.id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime withdrawal requests for logged in user
    val userWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getWithdrawalRequestsFlow(user.id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime deposit requests for advertiser
    val advertiserDeposits: StateFlow<List<DepositRequestEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "ADVERTISER") repository.getDepositRequestsFlow(user.id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime advertiser campaigns created
    val advertiserCampaigns: StateFlow<List<TaskEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null && user.role == "ADVERTISER") repository.getAdvertiserCampaignsFlow() else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Realtime saved payment accounts
    val savedAccounts: StateFlow<List<SavedAccountEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getSavedAccountsFlow(user.id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Super Admin Realtime Flows ---
    val adminPendingCompletions: StateFlow<List<CompletionEntity>> = repository.getAllPendingCompletionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminPendingKyc: StateFlow<List<KycEntity>> = repository.getAllPendingKycFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminPendingDeposits: StateFlow<List<DepositRequestEntity>> = repository.getAllPendingDepositsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminPendingWithdrawals: StateFlow<List<WithdrawalRequestEntity>> = repository.getAllPendingWithdrawalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAllUsers: StateFlow<List<UserEntity>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAllTransactions: StateFlow<List<TransactionEntity>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAllCompletions: StateFlow<List<CompletionEntity>> = repository.getAllCompletionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Operations UI States ---
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfNeeded()
            _currentUser.value = repository.currentUser
            _activeRole.value = repository.currentUser?.role ?: "EARNER"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearAuthMessages() {
        _authError.value = null
        _authSuccess.value = null
    }

    fun updateSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun selectPlatformFilter(platform: String?) {
        _selectedPlatformFilter.value = platform
    }

    fun selectTask(task: TaskEntity) {
        _selectedTask.value = task
        viewModelScope.launch {
            _selectedTaskCompletion.value = repository.getCompletionForTask(task.id)
        }
    }

    // --- Authentication Actions ---
    fun registerUser(
        name: String,
        email: String,
        phone: String,
        passwordPlain: String,
        cnic: String,
        referralCode: String?
    ) {
        _isProcessing.value = true
        clearAuthMessages()
        viewModelScope.launch {
            val result = repository.signup(name, email, phone, passwordPlain, cnic, referralCode)
            _isProcessing.value = false
            result.onSuccess { user ->
                _isOtpRequired.value = true
                _toastMessage.value = "Tasdeeq ke liye OTP Code bhej diya gaya hai."
            }.onFailure { exception ->
                _authError.value = exception.message ?: "Registration nakam ho gayi."
            }
        }
    }

    fun verifyOtpAndRegister(enteredCode: String) {
        _isProcessing.value = true
        clearAuthMessages()
        viewModelScope.launch {
            val result = repository.verifyOtpAndCompleteSignup(enteredCode)
            _isProcessing.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _activeRole.value = user.role
                _isOtpRequired.value = false
                _authSuccess.value = "Khush Amdeed! Aap ka TaskMallah account kamyabi se ban gaya hai."
            }.onFailure { exception ->
                _authError.value = exception.message ?: "OTP tasdeeq nakam ho gayi."
            }
        }
    }

    fun loginUser(identifier: String, passwordPlain: String) {
        _isProcessing.value = true
        clearAuthMessages()
        viewModelScope.launch {
            val result = repository.login(identifier, passwordPlain)
            _isProcessing.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _activeRole.value = user.role
                _authSuccess.value = "Khush Amdeed dobara, ${user.name}!"
            }.onFailure { exception ->
                _authError.value = exception.message ?: "Login nakam ho gayi."
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _isProcessing.value = true
        clearAuthMessages()
        viewModelScope.launch {
            val result = repository.loginWithGoogleToken(idToken)
            _isProcessing.value = false
            result.onSuccess { user ->
                _currentUser.value = user
                _activeRole.value = user.role
                _authSuccess.value = "Google ke zariye khush amdeed, ${user.name}!"
            }.onFailure { exception ->
                _authError.value = exception.message ?: "Google Sign-In nakam ho gaya."
            }
        }
    }

    fun forgotPassword(email: String) {
        _isProcessing.value = true
        clearAuthMessages()
        viewModelScope.launch {
            val result = repository.sendPasswordReset(email)
            _isProcessing.value = false
            result.onSuccess {
                _authSuccess.value = "Password reset email bhej di gayi hai. Bara-e-meherbani apni inbox check karein."
            }.onFailure { exception ->
                _authError.value = exception.message ?: "Reset email bhejne mein ghalati hui."
            }
        }
    }

    fun switchActiveRole(role: String) {
        viewModelScope.launch {
            repository.switchRole(role)
            _currentUser.value = repository.currentUser
            _activeRole.value = role
            _toastMessage.value = "Role switched to $role"
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        clearAuthMessages()
        _toastMessage.value = "Account logged out successfully."
    }

    // --- KYC Submit Action ---
    fun submitKycDetails(cnicNo: String, frontImg: String, backImg: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.submitKyc(cnicNo, frontImg, backImg)
            _isProcessing.value = false
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "KYC tasdeeq ke liye bhej di gayi hai. 24 hours mein response milega."
            }.onFailure {
                _toastMessage.value = it.message ?: "KYC submit karne mein masla aya."
            }
        }
    }

    // --- Task Submissions Action ---
    fun submitTaskCompletionProof(
        taskId: String,
        screenshotPath: String,
        customText: String? = null,
        taskTitle: String? = null,
        completionDate: Long? = null
    ) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.submitTaskProof(taskId, screenshotPath, customText, taskTitle, completionDate)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "Task verification ke liye jama ho gaya hai! Shukriya."
                // Refresh completion status
                val task = _selectedTask.value
                if (task != null) {
                    _selectedTaskCompletion.value = repository.getCompletionForTask(task.id)
                }
            }.onFailure {
                _toastMessage.value = it.message ?: "Task submit karne mein masla aya."
            }
        }
    }

    // --- AdMob Earning Actions ---
    fun watchAdMobAd(adType: String, rewardAmount: Double? = null) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.watchAd(adType, rewardAmount)
            _isProcessing.value = false
            result.onSuccess { reward ->
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Mubarak! Aap ko ad dekhne par ${"%.2f".format(reward)} PKR mil gaye hain."
            }.onFailure {
                _toastMessage.value = it.message ?: "Ad view reward register nahi ho saka."
            }
        }
    }

    // --- Withdrawal Action ---
    fun submitWithdrawal(amount: Double, payoutMethod: String, accountTitle: String, accountNo: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.requestWithdrawal(amount, payoutMethod, accountTitle, accountNo)
            _isProcessing.value = false
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Withdrawal request kamyabi se jama ho chuki hai."
            }.onFailure {
                _toastMessage.value = it.message ?: "Withdrawal process nahi ho sakhi."
            }
        }
    }

    // --- Advertiser Actions ---
    fun submitDepositPayment(amount: Double, method: String, proofPath: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.submitDepositRequest(amount, method, proofPath)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "Deposit proof review ke liye bhej diya gaya hai."
            }.onFailure {
                _toastMessage.value = it.message ?: "Deposit submit karne mein masla aya."
            }
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
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.createCampaign(platform, taskType, name, url, instructions, slots, pricePerSlot)
            _isProcessing.value = false
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Campaign kamyabi se active ho gaya hai!"
            }.onFailure {
                _toastMessage.value = it.message ?: "Campaign create nahi ho saka."
            }
        }
    }


    // --- Super Admin Moderation Actions ---
    fun approveOrRejectTaskCompletion(completionId: String, approve: Boolean, reason: String? = null) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.reviewTaskCompletion(completionId, approve, reason)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = if (approve) "Task kamyabi se APPROVE kar diya gaya." else "Task REJECT kar diya gaya."
            }.onFailure {
                _toastMessage.value = it.message ?: "Review process karne mein masla aya."
            }
        }
    }

    fun approveOrRejectKyc(userId: String, approve: Boolean, reason: String? = null) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.reviewKyc(userId, approve, reason)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = if (approve) "KYC tasdeeq kamyab!" else "KYC REJECT kar di gayi."
            }.onFailure {
                _toastMessage.value = it.message ?: "Review process karne mein masla aya."
            }
        }
    }

    fun approveOrRejectDeposit(requestId: String, approve: Boolean, reason: String? = null) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.processDeposit(requestId, approve, reason)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = if (approve) "Deposit APPROVE ho gaya! Advertiser balance barh gaya." else "Deposit REJECT kar diya gaya."
            }.onFailure {
                _toastMessage.value = it.message ?: "Review process karne mein masla aya."
            }
        }
    }

    fun approveOrRejectWithdrawal(requestId: String, approve: Boolean, reason: String? = null) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.processWithdrawal(requestId, approve, reason)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = if (approve) "Withdrawal mukammal!" else "Withdrawal REJECT kar di gayi. Balance wapis ho gaya."
            }.onFailure {
                _toastMessage.value = it.message ?: "Review process karne mein masla aya."
            }
        }
    }

    fun manualWalletAdjust(userId: String, amount: Double, reason: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.adjustUserWallet(userId, amount, reason)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "Wallet adjustment complete: $amount PKR"
            }.onFailure {
                _toastMessage.value = it.message ?: "Adjustment process nahi ho saki."
            }
        }
    }

    fun moderateUserBan(userId: String, ban: Boolean, reason: String?) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.banUser(userId, ban, reason)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = if (ban) "User ko BAN kar diya gaya hai." else "User ko UNBAN kar diya gaya."
            }.onFailure {
                _toastMessage.value = it.message ?: "Action complete nahi ho saki."
            }
        }
    }

    fun buySubscriptionTier(tier: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.buySubscription(tier)
            _isProcessing.value = false
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Mubarak! Premium $tier Package kamyabi se active ho chuka hai."
            }.onFailure {
                _toastMessage.value = it.message ?: "Package purchase nahi ho saka."
            }
        }
    }

    fun updateProfilePicture(uri: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.updateProfilePic(uri)
            _isProcessing.value = false
            result.onSuccess {
                _currentUser.value = repository.currentUser
                _toastMessage.value = "Profile picture kamyabi se tabdeel ho chuki hai!"
            }.onFailure {
                _toastMessage.value = it.message ?: "Profile picture update nahi ho saki."
            }
        }
    }

    fun linkNewPaymentAccount(bankName: String, accountTitle: String, accountNumber: String, iban: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.linkPaymentAccount(bankName, accountTitle, accountNumber, iban)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "Naya Payment Account kamyabi se link ho gaya!"
            }.onFailure {
                _toastMessage.value = it.message ?: "Account link karne mein masla aya."
            }
        }
    }

    fun changePaymentAccountLock(userId: String, lock: Boolean) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.lockPaymentDetails(userId, lock)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = if (lock) "Payment accounts locked!" else "Payment accounts unlocked!"
            }.onFailure {
                _toastMessage.value = it.message ?: "Lock change nahi ho saka."
            }
        }
    }

    fun removeSavedPaymentAccount(accountId: String) {
        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.deleteSavedAccount(accountId)
            _isProcessing.value = false
            result.onSuccess {
                _toastMessage.value = "Account kamyabi se delete ho gaya."
            }.onFailure {
                _toastMessage.value = it.message ?: "Account delete nahi ho saka."
            }
        }
    }
}

class ViewModelFactory(private val repository: TaskMallahRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskMallahViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskMallahViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
