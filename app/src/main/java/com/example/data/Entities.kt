package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val cnicHash: String,
    val role: String, // EARNER, ADVERTISER, ADMIN
    var walletBalancePkr: Double = 0.0,
    var totalEarnedPkr: Double = 0.0,
    var totalWithdrawnPkr: Double = 0.0,
    var totalReferralPkr: Double = 0.0,
    val referralCode: String,
    val referredBy: String? = null,
    var kycStatus: String = "NOT_SUBMITTED", // NOT_SUBMITTED, PENDING, APPROVED, REJECTED
    var accountLevel: String = "Bronze", // Bronze, Silver, Gold, Platinum
    var isBanned: Boolean = false,
    var banReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var preferredPlatforms: String = "YouTube,TikTok,Instagram,Google Play" // Comma-separated list
) : Serializable

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val advertiserId: String,
    val campaignName: String,
    val platform: String,
    val category: String,
    val taskType: String,
    val targetUrl: String,
    val instructions: String,
    val advPricePkr: Double,
    val userPayoutPkr: Double,
    val adminMarginPkr: Double,
    val totalSlots: Int,
    var slotsFilled: Int = 0,
    var status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED, REJECTED
    val createdAt: Long = System.currentTimeMillis(),
    val isFeatured: Boolean = false
) : Serializable

@Entity(tableName = "task_completions")
data class CompletionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val earnerId: String,
    val screenshotPath: String, // Path to local storage mock screenshot
    val submittedAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val payoutAmountPkr: Double,
    var reviewedBy: String? = null,
    var reviewedAt: Long? = null,
    var rejectionReason: String? = null,
    var isDuplicateFlagged: Boolean = false
) : Serializable

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // DEPOSIT, WITHDRAWAL, TASK_REWARD, REFERRAL, ADMIN_ADJUSTMENT
    val source: String, // Wallet transaction source
    val amountPkr: Double,
    val balanceAfterPkr: Double,
    val referenceId: String, // Bank / Transaction ID or Task ID
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    var status: String = "COMPLETED" // PENDING, COMPLETED, REJECTED
) : Serializable

@Entity(tableName = "kyc_submissions")
data class KycEntity(
    @PrimaryKey val userId: String,
    val cnicNumber: String,
    val cnicFrontPath: String,
    val cnicBackPath: String,
    val submittedAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING", // PENDING, APPROVED, REJECTED
    var reviewedAt: Long? = null,
    var reviewedBy: String? = null,
    var rejectionReason: String? = null
) : Serializable

@Entity(tableName = "deposit_requests")
data class DepositRequestEntity(
    @PrimaryKey val id: String,
    val advertiserId: String,
    val amountPkr: Double,
    val paymentMethod: String, // Bank Transfer, EasyPaisa, JazzCash
    val proofImagePath: String,
    val submittedAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING", // PENDING, APPROVED, REJECTED
    var processedAt: Long? = null,
    var rejectionReason: String? = null
) : Serializable

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amountPkr: Double,
    val payoutMethod: String, // EasyPaisa, JazzCash, Sadapay, Bank Transfer
    val accountTitle: String,
    val accountNoOrIban: String,
    val requestedAt: Long = System.currentTimeMillis(),
    var status: String = "PENDING", // PENDING, APPROVED, REJECTED
    var processedAt: Long? = null,
    var rejectionReason: String? = null
) : Serializable

@Entity(tableName = "ad_views_limit")
data class AdViewsEntity(
    @PrimaryKey val id: String, // userId_YYYY-MM-DD
    val userId: String,
    val dateString: String, // YYYY-MM-DD
    var rewardedCount: Int = 0,
    var interstitialCount: Int = 0,
    var lastRewardedAt: Long = 0L,
    var lastInterstitialAt: Long = 0L
) : Serializable
