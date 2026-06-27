package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskMallahDao {

    // User Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :query OR phone = :query LIMIT 1")
    suspend fun getUserByEmailOrPhone(query: String): UserEntity?

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): UserEntity?

    @Query("SELECT * FROM users WHERE referredBy = :referrerId")
    fun getReferredUsers(referrerId: String): Flow<List<UserEntity>>


    // Task Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE advertiserId = :advertiserId ORDER BY createdAt DESC")
    fun getTasksByAdvertiser(advertiserId: String): Flow<List<TaskEntity>>


    // Task Completion Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: CompletionEntity)

    @Update
    suspend fun updateCompletion(completion: CompletionEntity)

    @Query("SELECT * FROM task_completions WHERE id = :id")
    suspend fun getCompletionById(id: String): CompletionEntity?

    @Query("SELECT * FROM task_completions WHERE earnerId = :earnerId ORDER BY submittedAt DESC")
    fun getCompletionsForEarner(earnerId: String): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM task_completions WHERE earnerId = :earnerId AND taskId = :taskId LIMIT 1")
    suspend fun getCompletionForEarnerAndTask(earnerId: String, taskId: String): CompletionEntity?

    @Query("SELECT * FROM task_completions WHERE status = 'PENDING' ORDER BY submittedAt ASC")
    fun getAllPendingCompletionsFlow(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM task_completions ORDER BY submittedAt DESC")
    fun getAllCompletionsFlow(): Flow<List<CompletionEntity>>


    // Transaction Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(txn: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>


    // KYC Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKyc(kyc: KycEntity)

    @Update
    suspend fun updateKyc(kyc: KycEntity)

    @Query("SELECT * FROM kyc_submissions WHERE userId = :userId")
    suspend fun getKycForUser(userId: String): KycEntity?

    @Query("SELECT * FROM kyc_submissions WHERE userId = :userId")
    fun getKycForUserFlow(userId: String): Flow<KycEntity?>

    @Query("SELECT * FROM kyc_submissions WHERE status = 'PENDING' ORDER BY submittedAt ASC")
    fun getAllPendingKycFlow(): Flow<List<KycEntity>>


    // Deposit Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDepositRequest(deposit: DepositRequestEntity)

    @Update
    suspend fun updateDepositRequest(deposit: DepositRequestEntity)

    @Query("SELECT * FROM deposit_requests WHERE id = :id")
    suspend fun getDepositById(id: String): DepositRequestEntity?

    @Query("SELECT * FROM deposit_requests WHERE advertiserId = :advertiserId ORDER BY submittedAt DESC")
    fun getDepositRequestsForAdvertiser(advertiserId: String): Flow<List<DepositRequestEntity>>

    @Query("SELECT * FROM deposit_requests WHERE status = 'PENDING' ORDER BY submittedAt ASC")
    fun getAllPendingDepositsFlow(): Flow<List<DepositRequestEntity>>


    // Withdrawal Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawalRequest(withdrawal: WithdrawalRequestEntity)

    @Update
    suspend fun updateWithdrawalRequest(withdrawal: WithdrawalRequestEntity)

    @Query("SELECT * FROM withdrawal_requests WHERE id = :id")
    suspend fun getWithdrawalById(id: String): WithdrawalRequestEntity?

    @Query("SELECT * FROM withdrawal_requests WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getWithdrawalRequestsForUser(userId: String): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests WHERE status = 'PENDING' ORDER BY requestedAt ASC")
    fun getAllPendingWithdrawalsFlow(): Flow<List<WithdrawalRequestEntity>>


    // Ad Views Tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdViews(adViews: AdViewsEntity)

    @Query("SELECT * FROM ad_views_limit WHERE id = :id LIMIT 1")
    suspend fun getAdViewsById(id: String): AdViewsEntity?

    // Saved Payment Accounts Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedAccount(account: SavedAccountEntity)

    @Update
    suspend fun updateSavedAccount(account: SavedAccountEntity)

    @Query("SELECT * FROM saved_accounts WHERE userId = :userId ORDER BY createdAt ASC")
    fun getSavedAccountsForUserFlow(userId: String): Flow<List<SavedAccountEntity>>

    @Query("SELECT * FROM saved_accounts WHERE userId = :userId")
    suspend fun getSavedAccountsForUser(userId: String): List<SavedAccountEntity>

    @Query("SELECT * FROM saved_accounts WHERE id = :id")
    suspend fun getSavedAccountById(id: String): SavedAccountEntity?

    @Query("DELETE FROM saved_accounts WHERE id = :id")
    suspend fun deleteSavedAccountById(id: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM withdrawal_requests ORDER BY requestedAt DESC")
    fun getAllWithdrawalRequestsFlow(): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM deposit_requests ORDER BY submittedAt DESC")
    fun getAllDepositRequestsFlow(): Flow<List<DepositRequestEntity>>
}
