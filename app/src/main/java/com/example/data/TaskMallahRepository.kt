package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class TaskMallahRepository(private val db: AppDatabase) {
    private val dao = db.taskMallahDao()

    // Active User State Cached in Memory for convenience
    var currentUser: UserEntity? = null
        private set

    fun setCurrentUserDirect(user: UserEntity?) {
        currentUser = user
    }

    // Initialize Database with Seed Data
    suspend fun initializeDatabaseIfNeeded() {
        val allTasks = dao.getAllTasksFlow().firstOrNull()
        if (allTasks.isNullOrEmpty()) {
            val initialTasks = SeedData.getInitialTasks()
            initialTasks.forEach { dao.insertTask(it) }

            // Insert Super Admin if not exists
            val superAdmin = UserEntity(
                id = "admin_raju",
                name = "Raju Bhai (Super Admin)",
                email = "rajubhai3508194@gmail.com",
                phone = "03496677887",
                cnicHash = "ADMIN_CNIC_HASH",
                role = "ADMIN",
                walletBalancePkr = 100000.0,
                referralCode = "ADMINX",
                kycStatus = "APPROVED",
                accountLevel = "Platinum"
            )
            dao.insertUser(superAdmin)

            // Seed a few mock earners and advertisers for live feel
            val mockEarner = UserEntity(
                id = "earner_demo",
                name = "Muhammad Ali",
                email = "ali@gmail.com",
                phone = "03001234567",
                cnicHash = "CNIC_HASH_ALI",
                role = "EARNER",
                walletBalancePkr = 450.0,
                totalEarnedPkr = 1250.0,
                totalWithdrawnPkr = 800.0,
                referralCode = "ALI123",
                kycStatus = "APPROVED",
                accountLevel = "Silver"
            )
            dao.insertUser(mockEarner)

            val mockAdvertiser = UserEntity(
                id = "adv_demo",
                name = "Pak Tech Solutions",
                email = "adv@gmail.com",
                phone = "03112223344",
                cnicHash = "CNIC_HASH_ADV",
                role = "ADVERTISER",
                walletBalancePkr = 15000.0,
                referralCode = "PAKADV",
                kycStatus = "APPROVED",
                accountLevel = "Gold"
            )
            dao.insertUser(mockAdvertiser)

            // Seed mock transactions
            dao.insertTransaction(TransactionEntity(
                id = "tx_init_1",
                userId = "earner_demo",
                type = "TASK_REWARD",
                source = "YouTube View",
                amountPkr = 3.50,
                balanceAfterPkr = 450.0,
                referenceId = "task_a1",
                description = "Earned from YouTube Video Watch"
            ))
            dao.insertTransaction(TransactionEntity(
                id = "tx_init_2",
                userId = "earner_demo",
                type = "WITHDRAWAL",
                source = "EasyPaisa",
                amountPkr = -800.0,
                balanceAfterPkr = 446.50,
                referenceId = "EP_982312",
                description = "Withdrawal to EasyPaisa Account"
            ))

            // Seed mock campaigns for advertisers
            dao.insertDepositRequest(DepositRequestEntity(
                id = "dep_mock_1",
                advertiserId = "adv_demo",
                amountPkr = 15000.0,
                paymentMethod = "EasyPaisa",
                proofImagePath = "dummy_proof_path",
                status = "APPROVED",
                processedAt = System.currentTimeMillis()
            ))
        }
    }

    // Authentication & Registration Flow
    suspend fun signup(
        name: String,
        email: String,
        phone: String,
        passwordPlain: String, // Simulating security hashes
        cnic: String,
        referralCode: String?
    ): Result<UserEntity> {
        // Validate inputs
        if (name.length < 3 || !name.all { it.isLetter() || it == ' ' }) {
            return Result.failure(Exception("Naam kam az kam 3 huroof par mushtamil hona chahiye aur sirf alphabets hon."))
        }
        if (!email.contains("@") || !email.contains(".")) {
            return Result.failure(Exception("Bara-e-meherbani sahi Email darj karein."))
        }
        if (phone.length != 11 || !phone.startsWith("03")) {
            return Result.failure(Exception("Sahi Pakistani Mobile Number darj karein (e.g. 03XXXXXXXXX)."))
        }
        if (cnic.length != 13 || !cnic.all { it.isDigit() }) {
            return Result.failure(Exception("CNIC number bilkul 13 huroof (no dashes) ka hona chahiye."))
        }

        // Deduplication Check: CNIC Hash
        val cnicHash = cnic.hashCode().toString() // Simulated sha-256 for local SQLite
        val allUsersFlow = dao.getAllUsersFlow().firstOrNull() ?: emptyList()
        if (allUsersFlow.any { it.cnicHash == cnicHash }) {
            return Result.failure(Exception("Yeh CNIC pehle se register hai. Ek CNIC par ek hi account ban sakta hai."))
        }
        if (allUsersFlow.any { it.email.lowercase() == email.lowercase() }) {
            return Result.failure(Exception("Yeh Email pehle se register hai."))
        }
        if (allUsersFlow.any { it.phone == phone }) {
            return Result.failure(Exception("Yeh Mobile Number pehle se register hai."))
        }

        // Handle referral
        var referredByUserId: String? = null
        if (!referralCode.isNullOrBlank()) {
            val referrer = dao.getUserByReferralCode(referralCode.trim().uppercase())
            if (referrer != null) {
                referredByUserId = referrer.id
            } else {
                return Result.failure(Exception("Ghalat referral code."))
            }
        }

        // Set Role based on super admin credentials
        val isSuperAdmin = email.lowercase() == "rajubhai3508194@gmail.com" ||
                phone == "03496677887" ||
                phone == "03000856623"

        val role = if (isSuperAdmin) "ADMIN" else "EARNER" // Default is EARNER. User can choose Advertiser inside dashboard if they want.

        val newId = UUID.randomUUID().toString()
        val generatedReferral = name.take(3).uppercase() + (100..999).random().toString()

        val newUser = UserEntity(
            id = newId,
            name = name,
            email = email,
            phone = phone,
            cnicHash = cnicHash,
            role = role,
            walletBalancePkr = if (isSuperAdmin) 500000.0 else 0.0,
            referralCode = generatedReferral,
            referredBy = referredByUserId,
            kycStatus = if (isSuperAdmin) "APPROVED" else "NOT_SUBMITTED"
        )

        dao.insertUser(newUser)
        currentUser = newUser
        return Result.success(newUser)
    }

    suspend fun login(identifier: String): Result<UserEntity> {
        // Find user by email or phone
        val user = dao.getUserByEmailOrPhone(identifier.trim())
            ?: dao.getUserByReferralCode(identifier.trim().uppercase())

        return if (user != null) {
            if (user.isBanned) {
                Result.failure(Exception("Aap ka account ban hai. Wajah: ${user.banReason}"))
            } else {
                currentUser = user
                Result.success(user)
            }
        } else {
            Result.failure(Exception("Account nahi mila. Bara-e-meherbani sahi details darj karein."))
        }
    }

    fun logout() {
        currentUser = null
    }

    suspend fun switchRole(newRole: String) {
        val user = currentUser ?: return
        val updatedUser = user.copy(role = newRole)
        dao.updateUser(updatedUser)
        currentUser = updatedUser
    }

    // User Profile & KYC Upload
    suspend fun submitKyc(cnicNo: String, frontPath: String, backPath: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        if (cnicNo.length != 13 || !cnicNo.all { it.isDigit() }) {
            return Result.failure(Exception("CNIC number bilkul 13 huroof ka hona chahiye."))
        }

        val kyc = KycEntity(
            userId = user.id,
            cnicNumber = cnicNo,
            cnicFrontPath = frontPath,
            cnicBackPath = backPath,
            status = "PENDING"
        )
        dao.insertKyc(kyc)

        val updatedUser = user.copy(kycStatus = "PENDING")
        dao.updateUser(updatedUser)
        currentUser = updatedUser

        return Result.success(Unit)
    }

    // Task Browser Flows
    fun getTasksFlow(): Flow<List<TaskEntity>> = dao.getAllTasksFlow()

    suspend fun getTaskById(taskId: String): TaskEntity? = dao.getTaskById(taskId)

    fun getCompletionsForEarnerFlow(earnerId: String): Flow<List<CompletionEntity>> =
        dao.getCompletionsForEarner(earnerId)

    suspend fun getCompletionForTask(taskId: String): CompletionEntity? {
        val user = currentUser ?: return null
        return dao.getCompletionForEarnerAndTask(user.id, taskId)
    }

    // Submission Flow
    suspend fun submitTaskProof(taskId: String, screenshotPath: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val task = dao.getTaskById(taskId) ?: return Result.failure(Exception("Task nahi mila."))

        if (user.id == task.advertiserId) {
            return Result.failure(Exception("Aap apna banaya hua campaign mukammal nahi kar sakte."))
        }

        // Check duplicate
        val existing = dao.getCompletionForEarnerAndTask(user.id, taskId)
        if (existing != null) {
            return Result.failure(Exception("Aap yeh task pehle hi submit kar chuke hain."))
        }

        val completion = CompletionEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            earnerId = user.id,
            screenshotPath = screenshotPath,
            payoutAmountPkr = task.userPayoutPkr
        )
        dao.insertCompletion(completion)

        return Result.success(Unit)
    }

    // AdMob Earnings Flow (Server-Verified simulation with limits)
    suspend fun watchAd(adType: String): Result<Double> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val adViewsId = "${user.id}_$today"

        var adRecord = dao.getAdViewsById(adViewsId)
        if (adRecord == null) {
            adRecord = AdViewsEntity(id = adViewsId, userId = user.id, dateString = today)
        }

        val now = System.currentTimeMillis()
        val cooldownMillis = 3 * 60 * 1000 // 3 minutes cooldown

        val maxRewarded = when (user.accountLevel) {
            "Platinum" -> 35
            "Gold" -> 25
            else -> 20
        }
        val maxInterstitial = when (user.accountLevel) {
            "Platinum" -> 25
            "Gold" -> 20
            else -> 15
        }

        val rewardAmount: Double
        if (adType == "REWARDED") {
            if (adRecord.rewardedCount >= maxRewarded) {
                return Result.failure(Exception("Aap ki aaj ki Rewarded video dekhne ki limit mukammal ho chuki hai ($maxRewarded/day)."))
            }
            if (now - adRecord.lastRewardedAt < cooldownMillis) {
                val remainingSeconds = (cooldownMillis - (now - adRecord.lastRewardedAt)) / 1000
                return Result.failure(Exception("Bara-e-meherbani $remainingSeconds seconds sabr karein (3 minutes cooldown)."))
            }
            adRecord.rewardedCount += 1
            adRecord.lastRewardedAt = now
            rewardAmount = 0.35 // 0.35 PKR reward
        } else {
            if (adRecord.interstitialCount >= maxInterstitial) {
                return Result.failure(Exception("Aap ki aaj ki Interstitial ad dekhne ki limit mukammal ho chuki hai ($maxInterstitial/day)."))
            }
            if (now - adRecord.lastInterstitialAt < cooldownMillis) {
                val remainingSeconds = (cooldownMillis - (now - adRecord.lastInterstitialAt)) / 1000
                return Result.failure(Exception("Bara-e-meherbani $remainingSeconds seconds sabr karein (3 minutes cooldown)."))
            }
            adRecord.interstitialCount += 1
            adRecord.lastInterstitialAt = now
            rewardAmount = 0.21 // 0.21 PKR reward
        }

        // Credit Wallet
        val newBalance = user.walletBalancePkr + rewardAmount
        val newTotalEarned = user.totalEarnedPkr + rewardAmount
        val updatedUser = user.copy(
            walletBalancePkr = newBalance,
            totalEarnedPkr = newTotalEarned,
            accountLevel = calculateAccountLevel(newTotalEarned)
        )

        dao.insertAdViews(adRecord)
        dao.updateUser(updatedUser)
        currentUser = updatedUser

        // Record Transaction
        dao.insertTransaction(TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "TASK_REWARD",
            source = "AdMob $adType",
            amountPkr = rewardAmount,
            balanceAfterPkr = newBalance,
            referenceId = "ad_view_${System.currentTimeMillis()}",
            description = "Earning from AdMob $adType Ad"
        ))

        return Result.success(rewardAmount)
    }

    private fun calculateAccountLevel(totalEarned: Double): String {
        return when {
            totalEarned >= 10000.0 -> "Platinum"
            totalEarned >= 2000.0 -> "Gold"
            totalEarned >= 500.0 -> "Silver"
            else -> "Bronze"
        }
    }

    // Wallet & Withdrawals
    fun getTransactionsFlow(userId: String): Flow<List<TransactionEntity>> =
        dao.getTransactionsForUser(userId)

    fun getWithdrawalRequestsFlow(userId: String): Flow<List<WithdrawalRequestEntity>> =
        dao.getWithdrawalRequestsForUser(userId)

    suspend fun requestWithdrawal(
        amount: Double,
        payoutMethod: String,
        accountTitle: String,
        accountNo: String
    ): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))

        if (user.kycStatus != "APPROVED") {
            return Result.failure(Exception("Pesa nikalne ke liye KYC approved hona laazmi hai. Profile screen par CNIC upload karein."))
        }
        if (amount < 200.0) {
            return Result.failure(Exception("Kam az kam withdrawal amount 200 PKR hai."))
        }
        if (user.walletBalancePkr < amount) {
            return Result.failure(Exception("Aap ke wallet mein kaafi balance nahi hai."))
        }

        // Create withdrawal request
        val request = WithdrawalRequestEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            amountPkr = amount,
            payoutMethod = payoutMethod,
            accountTitle = accountTitle,
            accountNoOrIban = accountNo
        )
        dao.insertWithdrawalRequest(request)

        // Deduct wallet balance immediately (Pending status)
        val newBalance = user.walletBalancePkr - amount
        val updatedUser = user.copy(walletBalancePkr = newBalance)
        dao.updateUser(updatedUser)
        currentUser = updatedUser

        // Record transaction
        dao.insertTransaction(TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "WITHDRAWAL",
            source = payoutMethod,
            amountPkr = -amount,
            balanceAfterPkr = newBalance,
            referenceId = request.id,
            description = "Withdrawal request submitted (${request.status})",
            status = "PENDING"
        ))

        return Result.success(Unit)
    }

    // Advertiser Flows
    fun getDepositRequestsFlow(advertiserId: String): Flow<List<DepositRequestEntity>> =
        dao.getDepositRequestsForAdvertiser(advertiserId)

    suspend fun submitDepositRequest(amount: Double, method: String, proofPath: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        if (amount < 500.0) {
            return Result.failure(Exception("Kam az kam deposit 500 PKR hai."))
        }

        val request = DepositRequestEntity(
            id = UUID.randomUUID().toString(),
            advertiserId = user.id,
            amountPkr = amount,
            paymentMethod = method,
            proofImagePath = proofPath
        )
        dao.insertDepositRequest(request)

        return Result.success(Unit)
    }

    suspend fun createCampaign(
        platform: String,
        taskType: String,
        campaignName: String,
        targetUrl: String,
        instructions: String,
        slots: Int,
        pricePerSlot: Double
    ): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val totalCost = slots * pricePerSlot

        if (user.walletBalancePkr < totalCost) {
            return Result.failure(Exception("Aap ka balance kam hai. Bara-e-meherbani pehle PKR deposit karein (Total needed: $totalCost PKR)."))
        }

        if (slots < 10) {
            return Result.failure(Exception("Kam az kam 10 slots hona laazmi hain."))
        }

        // Admin fee is 30% of total transaction. Earner share is 70%.
        val earnerPayout = pricePerSlot * 0.70
        val adminMargin = pricePerSlot * 0.30

        val campaignId = "camp_" + UUID.randomUUID().toString().take(8)
        val newTask = TaskEntity(
            id = campaignId,
            advertiserId = user.id,
            campaignName = campaignName,
            platform = platform,
            category = "Advertiser Campaigns",
            taskType = taskType,
            targetUrl = targetUrl,
            instructions = instructions,
            advPricePkr = pricePerSlot,
            userPayoutPkr = earnerPayout,
            adminMarginPkr = adminMargin,
            totalSlots = slots
        )

        // Deduct from advertiser balance atomically
        val newBalance = user.walletBalancePkr - totalCost
        val updatedUser = user.copy(walletBalancePkr = newBalance)

        dao.insertTask(newTask)
        dao.updateUser(updatedUser)
        currentUser = updatedUser

        // Add transaction log
        dao.insertTransaction(TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "DEPOSIT",
            source = "Campaign Creation",
            amountPkr = -totalCost,
            balanceAfterPkr = newBalance,
            referenceId = campaignId,
            description = "Created campaign '$campaignName' ($slots slots)"
        ))

        return Result.success(Unit)
    }

    fun getAdvertiserCampaignsFlow(): Flow<List<TaskEntity>> {
        val user = currentUser ?: throw Exception("Not logged in")
        return dao.getTasksByAdvertiser(user.id)
    }

    // SUPER ADMIN POWERS (Only unlocked by the 3 Super Admin identifiers)
    fun getAllPendingCompletionsFlow(): Flow<List<CompletionEntity>> = dao.getAllPendingCompletionsFlow()
    fun getAllPendingKycFlow(): Flow<List<KycEntity>> = dao.getAllPendingKycFlow()
    fun getAllPendingDepositsFlow(): Flow<List<DepositRequestEntity>> = dao.getAllPendingDepositsFlow()
    fun getAllPendingWithdrawalsFlow(): Flow<List<WithdrawalRequestEntity>> = dao.getAllPendingWithdrawalsFlow()
    fun getAllUsersFlow(): Flow<List<UserEntity>> = dao.getAllUsersFlow()
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = dao.getAllTransactionsFlow()
    fun getAllCompletionsFlow(): Flow<List<CompletionEntity>> = dao.getAllCompletionsFlow()

    suspend fun reviewTaskCompletion(completionId: String, approve: Boolean, rejectionReason: String? = null): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN") return Result.failure(Exception("Permission denied."))

        val completion = dao.getCompletionById(completionId) ?: return Result.failure(Exception("Completion not found."))
        if (completion.status != "PENDING") return Result.failure(Exception("Already reviewed."))

        val task = dao.getTaskById(completion.taskId) ?: return Result.failure(Exception("Task not found."))
        val earner = dao.getUserById(completion.earnerId) ?: return Result.failure(Exception("Earner not found."))

        if (approve) {
            // Approve: Reward earner, update task slot, handle referrals
            completion.status = "APPROVED"
            completion.reviewedBy = admin.name
            completion.reviewedAt = System.currentTimeMillis()

            val newEarnerBalance = earner.walletBalancePkr + completion.payoutAmountPkr
            val newEarnerTotalEarned = earner.totalEarnedPkr + completion.payoutAmountPkr
            val updatedEarner = earner.copy(
                walletBalancePkr = newEarnerBalance,
                totalEarnedPkr = newEarnerTotalEarned,
                accountLevel = calculateAccountLevel(newEarnerTotalEarned)
            )

            // Update Task Slots
            task.slotsFilled += 1
            if (task.slotsFilled >= task.totalSlots) {
                task.status = "COMPLETED"
            }

            dao.updateCompletion(completion)
            dao.updateUser(updatedEarner)
            dao.updateTask(task)

            // Record transaction for earner
            dao.insertTransaction(TransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = earner.id,
                type = "TASK_REWARD",
                source = task.platform,
                amountPkr = completion.payoutAmountPkr,
                balanceAfterPkr = newEarnerBalance,
                referenceId = completion.id,
                description = "Task Approved: ${task.campaignName}"
            ))

            // Handle Referral: Paid from admin's 30% only (Referral level 1: 7%, level 2: 3% of admin margin)
            val adminMargin = task.adminMarginPkr
            if (earner.referredBy != null) {
                val lvl1Referrer = dao.getUserById(earner.referredBy)
                if (lvl1Referrer != null) {
                    val lvl1Reward = adminMargin * 0.07
                    val updatedLvl1 = lvl1Referrer.copy(
                        walletBalancePkr = lvl1Referrer.walletBalancePkr + lvl1Reward,
                        totalReferralPkr = lvl1Referrer.totalReferralPkr + lvl1Reward
                    )
                    dao.updateUser(updatedLvl1)
                    dao.insertTransaction(TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = lvl1Referrer.id,
                        type = "REFERRAL",
                        source = "L1: ${earner.name}",
                        amountPkr = lvl1Reward,
                        balanceAfterPkr = updatedLvl1.walletBalancePkr,
                        referenceId = completion.id,
                        description = "Referral Commission Lvl 1 from ${earner.name}"
                    ))

                    // Indirect Referrer Level 2
                    if (lvl1Referrer.referredBy != null) {
                        val lvl2Referrer = dao.getUserById(lvl1Referrer.referredBy)
                        if (lvl2Referrer != null) {
                            val lvl2Reward = adminMargin * 0.03
                            val updatedLvl2 = lvl2Referrer.copy(
                                walletBalancePkr = lvl2Referrer.walletBalancePkr + lvl2Reward,
                                totalReferralPkr = lvl2Referrer.totalReferralPkr + lvl2Reward
                            )
                            dao.updateUser(updatedLvl2)
                            dao.insertTransaction(TransactionEntity(
                                id = UUID.randomUUID().toString(),
                                userId = lvl2Referrer.id,
                                type = "REFERRAL",
                                source = "L2: ${earner.name}",
                                amountPkr = lvl2Reward,
                                balanceAfterPkr = updatedLvl2.walletBalancePkr,
                                referenceId = completion.id,
                                description = "Referral Commission Lvl 2 from ${earner.name}"
                            ))
                        }
                    }
                }
            }
        } else {
            // Reject: Just mark status
            completion.status = "REJECTED"
            completion.rejectionReason = rejectionReason ?: "Screenshot unclear or requirements not met."
            completion.reviewedBy = admin.name
            completion.reviewedAt = System.currentTimeMillis()

            dao.updateCompletion(completion)
        }

        return Result.success(Unit)
    }

    suspend fun reviewKyc(userId: String, approve: Boolean, reason: String? = null): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN") return Result.failure(Exception("Permission denied."))

        val kyc = dao.getKycForUser(userId) ?: return Result.failure(Exception("KYC submission not found."))
        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found."))

        if (approve) {
            kyc.status = "APPROVED"
            user.kycStatus = "APPROVED"
        } else {
            kyc.status = "REJECTED"
            kyc.rejectionReason = reason ?: "CNIC pictures are blurry or mismatch."
            user.kycStatus = "REJECTED"
        }

        kyc.reviewedAt = System.currentTimeMillis()
        kyc.reviewedBy = admin.name

        dao.updateKyc(kyc)
        dao.updateUser(user)

        return Result.success(Unit)
    }

    suspend fun processDeposit(requestId: String, approve: Boolean, reason: String? = null): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN") return Result.failure(Exception("Permission denied."))

        val deposit = dao.getDepositById(requestId) ?: return Result.failure(Exception("Deposit request not found."))
        if (deposit.status != "PENDING") return Result.failure(Exception("Deposit already processed."))

        val advertiser = dao.getUserById(deposit.advertiserId) ?: return Result.failure(Exception("Advertiser not found."))

        if (approve) {
            deposit.status = "APPROVED"
            deposit.processedAt = System.currentTimeMillis()

            val newBalance = advertiser.walletBalancePkr + deposit.amountPkr
            val updatedAdvertiser = advertiser.copy(walletBalancePkr = newBalance)

            dao.updateDepositRequest(deposit)
            dao.updateUser(updatedAdvertiser)

            // Record transaction
            dao.insertTransaction(TransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = advertiser.id,
                type = "DEPOSIT",
                source = deposit.paymentMethod,
                amountPkr = deposit.amountPkr,
                balanceAfterPkr = newBalance,
                referenceId = deposit.id,
                description = "EasyPaisa/Bank Deposit Approved by Admin"
            ))
        } else {
            deposit.status = "REJECTED"
            deposit.rejectionReason = reason ?: "Proof screenshot does not match bank logs."
            deposit.processedAt = System.currentTimeMillis()

            dao.updateDepositRequest(deposit)
        }

        return Result.success(Unit)
    }

    suspend fun processWithdrawal(requestId: String, approve: Boolean, reason: String? = null): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN") return Result.failure(Exception("Permission denied."))

        val withdrawal = dao.getWithdrawalById(requestId) ?: return Result.failure(Exception("Withdrawal request not found."))
        if (withdrawal.status != "PENDING") return Result.failure(Exception("Withdrawal already processed."))

        val user = dao.getUserById(withdrawal.userId) ?: return Result.failure(Exception("User not found."))

        if (approve) {
            withdrawal.status = "COMPLETED"
            withdrawal.processedAt = System.currentTimeMillis()

            val newTotalWithdrawn = user.totalWithdrawnPkr + withdrawal.amountPkr
            val updatedUser = user.copy(totalWithdrawnPkr = newTotalWithdrawn)

            dao.updateWithdrawalRequest(withdrawal)
            dao.updateUser(updatedUser)

            // Find corresponding pending transaction and mark completed
            val txns = dao.getTransactionsForUser(user.id).firstOrNull() ?: emptyList()
            val tx = txns.find { it.referenceId == withdrawal.id }
            if (tx != null) {
                // Remove pending log or change text to completed
                val updatedTx = tx.copy(description = "Withdrawal to ${withdrawal.payoutMethod} completed successfully", status = "COMPLETED")
                dao.insertTransaction(updatedTx)
            }
        } else {
            withdrawal.status = "REJECTED"
            withdrawal.rejectionReason = reason ?: "Invalid payment details / rejected by bank."
            withdrawal.processedAt = System.currentTimeMillis()

            // Refund user balance!
            val newBalance = user.walletBalancePkr + withdrawal.amountPkr
            val updatedUser = user.copy(walletBalancePkr = newBalance)

            dao.updateWithdrawalRequest(withdrawal)
            dao.updateUser(updatedUser)

            // Add transaction log for refund
            dao.insertTransaction(TransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                type = "ADMIN_ADJUSTMENT",
                source = "Withdrawal Refund",
                amountPkr = withdrawal.amountPkr,
                balanceAfterPkr = newBalance,
                referenceId = withdrawal.id,
                description = "Withdrawal Rejected: ${withdrawal.rejectionReason}. Refunded ${withdrawal.amountPkr} PKR."
            ))
        }

        return Result.success(Unit)
    }

    suspend fun adjustUserWallet(userId: String, amount: Double, reason: String): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN") return Result.failure(Exception("Permission denied."))

        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found."))
        val newBalance = user.walletBalancePkr + amount
        val updatedUser = user.copy(walletBalancePkr = newBalance)

        dao.updateUser(updatedUser)

        dao.insertTransaction(TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "ADMIN_ADJUSTMENT",
            source = "Admin Manual",
            amountPkr = amount,
            balanceAfterPkr = newBalance,
            referenceId = "admin_adj_${System.currentTimeMillis()}",
            description = "Admin Manual adjustment: $reason (by ${admin.name})"
        ))

        return Result.success(Unit)
    }

    suspend fun banUser(userId: String, ban: Boolean, reason: String? = null): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN") return Result.failure(Exception("Permission denied."))

        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found."))
        val updatedUser = user.copy(isBanned = ban, banReason = if (ban) reason else null)

        dao.updateUser(updatedUser)
        return Result.success(Unit)
    }
}
