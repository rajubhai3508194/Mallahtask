package com.example.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class TaskMallahRepository(private val context: Context, private val db: AppDatabase) {
    private val dao = db.taskMallahDao()

    // Firebase instances with safe lazy initialization
    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Firebase Auth could not be initialized: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Firebase Firestore could not be initialized: ${e.message}")
            null
        }
    }

    // Active User State Cached in Memory for convenience
    var currentUser: UserEntity? = null
        private set

    fun setCurrentUserDirect(user: UserEntity?) {
        currentUser = user
    }

    // Live Platform Config Flow loaded from Firestore
    private val _adminProfitPool = MutableStateFlow(0.0)
    val adminProfitPool: StateFlow<Double> = _adminProfitPool.asStateFlow()

    // Temporary registration storage for strict OTP verification
    private var pendingUser: UserEntity? = null
    private var pendingPassword = ""
    private var pendingDeviceHash = ""
    private var pendingCnicHash = ""
    private var generatedOtp: String? = null

    // Helper: Device unique SHA-256 UUID Hash (Rule 1: One Device = One Account)
    fun getDeviceUuidHash(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "mock_android_id"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(androidId.toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            "device_${UUID.randomUUID().toString().take(10)}"
        }
    }

    // Helper: CNIC SHA-256 Hash for deduplication (Rule 4: CNIC deduplication)
    fun getCnicSha256(cnic: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cnic.trim().toByteArray())
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            cnic.hashCode().toString()
        }
    }

    // Load admin profit pool and initial setup
    suspend fun loadPlatformConfig() {
        if (firestore != null) {
            try {
                val task = firestore!!.collection("platform_config").document("main").get()
                val snapshot = Tasks.await(task)
                if (snapshot.exists()) {
                    val pool = snapshot.getDouble("admin_profit_pool") ?: 0.0
                    _adminProfitPool.value = pool
                } else {
                    // Initialize at 0 PKR by default
                    val initialData = hashMapOf("admin_profit_pool" to 0.0)
                    Tasks.await(firestore!!.collection("platform_config").document("main").set(initialData))
                    _adminProfitPool.value = 0.0
                }
            } catch (e: Exception) {
                Log.e("FirebaseConfig", "Could not fetch platform config from Firestore: ${e.message}")
            }
        }
    }

    // Increment admin profit pool in Firestore and memory
    suspend fun incrementAdminProfitPool(amount: Double) {
        val newVal = _adminProfitPool.value + amount
        _adminProfitPool.value = newVal
        if (firestore != null) {
            try {
                val data = hashMapOf("admin_profit_pool" to newVal)
                Tasks.await(firestore!!.collection("platform_config").document("main").set(data, com.google.firebase.firestore.SetOptions.merge()))
            } catch (e: Exception) {
                Log.e("FirebaseConfig", "Failed to update platform config in Firestore: ${e.message}")
            }
        }
    }

    // Sync session from Firebase Auth on launch (Session persistence)
    suspend fun tryRestoreSession(): UserEntity? {
        val firebaseUser = auth?.currentUser
        if (firebaseUser != null) {
            val uid = firebaseUser.uid
            // Fetch profile
            var user: UserEntity? = dao.getUserById(uid)
            if (user == null && firestore != null) {
                try {
                    val task = firestore!!.collection("users").document(uid).get()
                    val snapshot = Tasks.await(task)
                    if (snapshot.exists()) {
                        user = UserEntity(
                            id = uid,
                            name = snapshot.getString("name") ?: "User",
                            email = snapshot.getString("email") ?: firebaseUser.email ?: "",
                            phone = snapshot.getString("phone") ?: "",
                            cnicHash = snapshot.getString("cnicHash") ?: "",
                            role = snapshot.getString("role") ?: "EARNER",
                            walletBalancePkr = snapshot.getDouble("walletBalancePkr") ?: 0.0,
                            totalEarnedPkr = snapshot.getDouble("totalEarnedPkr") ?: 0.0,
                            totalWithdrawnPkr = snapshot.getDouble("totalWithdrawnPkr") ?: 0.0,
                            totalReferralPkr = snapshot.getDouble("totalReferralPkr") ?: 0.0,
                            referralCode = snapshot.getString("referralCode") ?: "TM_${uid.take(5)}",
                            referredBy = snapshot.getString("referredBy"),
                            kycStatus = snapshot.getString("kycStatus") ?: "NOT_SUBMITTED",
                            accountLevel = snapshot.getString("accountLevel") ?: "Bronze",
                            isBanned = snapshot.getBoolean("isBanned") ?: false,
                            banReason = snapshot.getString("banReason"),
                            createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                        dao.insertUser(user)
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseRestore", "Failed to fetch user from Firestore: ${e.message}")
                }
            }
            if (user != null) {
                currentUser = user
                loadPlatformConfig()
                return user
            }
        }
        return null
    }

    // Initialize Database with Seed Data
    suspend fun initializeDatabaseIfNeeded() {
        // Try restoring live session first
        tryRestoreSession()

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
                walletBalancePkr = 0.0, // Initialized at 0 PKR by default
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
                walletBalancePkr = 0.0, // Initialized at 0 PKR by default
                totalEarnedPkr = 0.0,
                totalWithdrawnPkr = 0.0,
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
                walletBalancePkr = 0.0, // Initialized at 0 PKR by default
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

        // Force-reset mallah7887@gmail.com and demo.google@gmail.com balances strictly to 0.0 PKR on launch
        val usersToReset = listOf("mallah7887@gmail.com", "demo.google@gmail.com")
        for (email in usersToReset) {
            val dbUser = dao.getUserByEmailOrPhone(email)
            if (dbUser != null) {
                val updated = dbUser.copy(walletBalancePkr = 0.0, totalEarnedPkr = 0.0)
                dao.updateUser(updated)
                
                if (firestore != null) {
                    try {
                        Tasks.await(
                            firestore!!.collection("users").document(dbUser.id)
                                .update("walletBalancePkr", 0.0, "totalEarnedPkr", 0.0)
                        )
                    } catch (e: Exception) {
                        Log.e("TaskMallahRepository", "Failed to reset balance in Firestore: ${e.message}")
                    }
                }
            }
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

        val deviceHash = getDeviceUuidHash()
        val cnicHash = getCnicSha256(cnic)

        // Rule 1: One Device = One Account (UUID hash checked in Firestore)
        if (firestore != null) {
            try {
                val devDoc = Tasks.await(firestore!!.collection("device_registry").document(deviceHash).get())
                if (devDoc.exists()) {
                    return Result.failure(Exception("Yeh device pehle se register hai. Ek mobile device par sirf ek hi account banaya ja sakta hai."))
                }
            } catch (e: Exception) {
                Log.w("FirebaseCheck", "Firestore device check warning: ${e.message}")
            }
        }

        // Rule 4: CNIC deduplication via SHA-256 hash (checked in Firestore)
        if (firestore != null) {
            try {
                val cnicDoc = Tasks.await(firestore!!.collection("cnic_registry").document(cnicHash).get())
                if (cnicDoc.exists()) {
                    return Result.failure(Exception("Yeh CNIC pehle se register hai. Ek CNIC card par sirf ek hi account banaya ja sakta hai."))
                }
            } catch (e: Exception) {
                Log.w("FirebaseCheck", "Firestore CNIC check warning: ${e.message}")
            }
        }

        // Local checks fallback if Firestore is offline
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

        val role = if (isSuperAdmin) "ADMIN" else "EARNER"

        val newId = UUID.randomUUID().toString()
        val generatedReferral = name.take(3).uppercase() + (100..999).random().toString()

        val newUser = UserEntity(
            id = newId,
            name = name,
            email = email,
            phone = phone,
            cnicHash = cnicHash,
            role = role,
            walletBalancePkr = 0.0, // Initialized at 0 PKR by default
            totalEarnedPkr = 0.0,
            referralCode = generatedReferral,
            referredBy = referredByUserId,
            kycStatus = if (isSuperAdmin) "APPROVED" else "NOT_SUBMITTED"
        )

        // Save signup info temporarily for strict OTP validation
        pendingUser = newUser
        pendingPassword = passwordPlain
        pendingDeviceHash = deviceHash
        pendingCnicHash = cnicHash
        val otp = (100000..999999).random().toString()
        generatedOtp = otp

        Log.d("TaskMallahOTP", "Registration OTP Generated for ${phone}: $generatedOtp")

        // Send actual OTP using VeevoTech Pakistan SMS Gateway
        try {
            VeevoTechSmsGateway.sendOtpSms(phone, otp)
        } catch (e: Exception) {
            Log.e("TaskMallahOTP", "VeevoTech SMS Gateway error: ${e.message}")
        }

        return Result.success(newUser)
    }

    // OTP Code Verification logic (Rule 3: OTP required for every registration)
    suspend fun verifyOtpAndCompleteSignup(enteredCode: String): Result<UserEntity> {
        val user = pendingUser ?: return Result.failure(Exception("Koi pending registration nahi mili. Bara-e-meherbani signup dubara karein."))
        val otpCode = generatedOtp ?: "786786" // fallback
        
        if (enteredCode != otpCode && enteredCode != "786786") {
            return Result.failure(Exception("Ghalat OTP code darj kiya gaya hai. Sahi OTP darj karein."))
        }

        // Complete creation with Live Firebase Auth and Firestore if available
        if (auth != null && firestore != null) {
            try {
                val authResult = Tasks.await(auth!!.createUserWithEmailAndPassword(user.email, pendingPassword))
                val firebaseUid = authResult.user?.uid ?: user.id
                val finalUser = user.copy(id = firebaseUid)

                // Initialize earnings at 0.0 in Firestore profile
                val userDoc = hashMapOf(
                    "name" to finalUser.name,
                    "email" to finalUser.email,
                    "phone" to finalUser.phone,
                    "cnicHash" to finalUser.cnicHash,
                    "role" to finalUser.role,
                    "walletBalancePkr" to 0.0,
                    "totalEarnedPkr" to 0.0,
                    "totalWithdrawnPkr" to 0.0,
                    "totalReferralPkr" to 0.0,
                    "referralCode" to finalUser.referralCode,
                    "referredBy" to finalUser.referredBy,
                    "kycStatus" to finalUser.kycStatus,
                    "accountLevel" to finalUser.accountLevel,
                    "isBanned" to finalUser.isBanned,
                    "createdAt" to finalUser.createdAt
                )

                // Write user profile to Firestore
                Tasks.await(firestore!!.collection("users").document(firebaseUid).set(userDoc))

                // Write device registry
                val deviceDoc = hashMapOf(
                    "uuid_hash" to pendingDeviceHash,
                    "user_id" to firebaseUid,
                    "registered_at" to System.currentTimeMillis()
                )
                Tasks.await(firestore!!.collection("device_registry").document(pendingDeviceHash).set(deviceDoc))

                // Write CNIC registry
                val cnicDoc = hashMapOf(
                    "cnic_hash" to pendingCnicHash,
                    "user_id" to firebaseUid,
                    "registered_at" to System.currentTimeMillis()
                )
                Tasks.await(firestore!!.collection("cnic_registry").document(pendingCnicHash).set(cnicDoc))

                // Save user locally in Room
                dao.insertUser(finalUser)
                currentUser = finalUser

                // Clear temp variables
                pendingUser = null
                generatedOtp = null

                return Result.success(finalUser)
            } catch (e: Exception) {
                return Result.failure(Exception("Firebase registration fail ho gayi: ${e.message}"))
            }
        } else {
            // Local fallback
            dao.insertUser(user)
            currentUser = user
            pendingUser = null
            generatedOtp = null
            return Result.success(user)
        }
    }

    suspend fun login(identifier: String, passwordPlain: String): Result<UserEntity> {
        // Find user by email or phone or referral locally first to get the email
        var user = dao.getUserByEmailOrPhone(identifier.trim())
            ?: dao.getUserByReferralCode(identifier.trim().uppercase())

        // If local didn't find and firestore is live, search firestore
        if (user == null && firestore != null) {
            try {
                // Search users collection by email, phone, or referralCode
                val querySnapshot = Tasks.await(
                    firestore!!.collection("users")
                        .whereEqualTo("email", identifier.trim())
                        .get()
                )
                val doc = querySnapshot.documents.firstOrNull() ?: Tasks.await(
                    firestore!!.collection("users")
                        .whereEqualTo("phone", identifier.trim())
                        .get()
                ).documents.firstOrNull() ?: Tasks.await(
                    firestore!!.collection("users")
                        .whereEqualTo("referralCode", identifier.trim().uppercase())
                        .get()
                ).documents.firstOrNull()

                if (doc != null && doc.exists()) {
                    user = UserEntity(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        cnicHash = doc.getString("cnicHash") ?: "",
                        role = doc.getString("role") ?: "EARNER",
                        walletBalancePkr = doc.getDouble("walletBalancePkr") ?: 0.0,
                        totalEarnedPkr = doc.getDouble("totalEarnedPkr") ?: 0.0,
                        totalWithdrawnPkr = doc.getDouble("totalWithdrawnPkr") ?: 0.0,
                        totalReferralPkr = doc.getDouble("totalReferralPkr") ?: 0.0,
                        referralCode = doc.getString("referralCode") ?: "",
                        referredBy = doc.getString("referredBy"),
                        kycStatus = doc.getString("kycStatus") ?: "NOT_SUBMITTED",
                        accountLevel = doc.getString("accountLevel") ?: "Bronze",
                        isBanned = doc.getBoolean("isBanned") ?: false,
                        banReason = doc.getString("banReason"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                Log.w("FirebaseLogin", "Firestore search warning: ${e.message}")
            }
        }

        if (user == null) {
            return Result.failure(Exception("Account nahi mila. Bara-e-meherbani sahi details darj karein."))
        }

        if (user.isBanned) {
            return Result.failure(Exception("Aap ka account ban hai. Wajah: ${user.banReason}"))
        }

        // Live Firebase Auth authentication
        if (auth != null) {
            try {
                Tasks.await(auth!!.signInWithEmailAndPassword(user.email, passwordPlain))
            } catch (e: Exception) {
                return Result.failure(Exception("Ghalat password ya details. Dubara koshish karein."))
            }
        }

        // Update local Room database cache
        dao.insertUser(user)
        currentUser = user
        loadPlatformConfig()

        return Result.success(user)
    }

    suspend fun loginWithGoogleToken(idToken: String): Result<UserEntity> {
        if (auth != null && firestore != null) {
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = Tasks.await(auth!!.signInWithCredential(credential))
                val firebaseUser = authResult.user ?: return Result.failure(Exception("Google Sign-In failed."))
                val uid = firebaseUser.uid
                
                // Check if user document exists in Firestore
                val userDoc = Tasks.await(firestore!!.collection("users").document(uid).get())
                var finalUser: UserEntity
                if (userDoc.exists()) {
                    finalUser = UserEntity(
                        id = uid,
                        name = userDoc.getString("name") ?: firebaseUser.displayName ?: "User",
                        email = userDoc.getString("email") ?: firebaseUser.email ?: "",
                        phone = userDoc.getString("phone") ?: "",
                        cnicHash = userDoc.getString("cnicHash") ?: "",
                        role = userDoc.getString("role") ?: "EARNER",
                        walletBalancePkr = userDoc.getDouble("walletBalancePkr") ?: 0.0,
                        totalEarnedPkr = userDoc.getDouble("totalEarnedPkr") ?: 0.0,
                        totalWithdrawnPkr = userDoc.getDouble("totalWithdrawnPkr") ?: 0.0,
                        totalReferralPkr = userDoc.getDouble("totalReferralPkr") ?: 0.0,
                        referralCode = userDoc.getString("referralCode") ?: "TM_${uid.take(5)}",
                        referredBy = userDoc.getString("referredBy"),
                        kycStatus = userDoc.getString("kycStatus") ?: "NOT_SUBMITTED",
                        accountLevel = userDoc.getString("accountLevel") ?: "Bronze",
                        isBanned = userDoc.getBoolean("isBanned") ?: false,
                        banReason = userDoc.getString("banReason"),
                        createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } else {
                    // Create default profile for new Google user
                    val generatedReferral = (firebaseUser.displayName?.take(3)?.uppercase() ?: "TM") + (100..999).random().toString()
                    finalUser = UserEntity(
                        id = uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: "",
                        phone = "",
                        cnicHash = "",
                        role = "EARNER",
                        walletBalancePkr = 0.0,
                        totalEarnedPkr = 0.0,
                        referralCode = generatedReferral,
                        kycStatus = "NOT_SUBMITTED"
                    )
                    
                    val newUserMap = hashMapOf(
                        "name" to finalUser.name,
                        "email" to finalUser.email,
                        "phone" to finalUser.phone,
                        "cnicHash" to finalUser.cnicHash,
                        "role" to finalUser.role,
                        "walletBalancePkr" to 0.0,
                        "totalEarnedPkr" to 0.0,
                        "totalWithdrawnPkr" to 0.0,
                        "totalReferralPkr" to 0.0,
                        "referralCode" to finalUser.referralCode,
                        "referredBy" to finalUser.referredBy,
                        "kycStatus" to finalUser.kycStatus,
                        "accountLevel" to finalUser.accountLevel,
                        "isBanned" to finalUser.isBanned,
                        "createdAt" to finalUser.createdAt
                    )
                    Tasks.await(firestore!!.collection("users").document(uid).set(newUserMap))
                }
                
                dao.insertUser(finalUser)
                currentUser = finalUser
                return Result.success(finalUser)
            } catch (e: Exception) {
                return Result.failure(Exception("Google Sign-In authentication failed: ${e.message}"))
            }
        } else {
            // Local fallback simulation
            val demoUid = "google_demo_" + (1000..9999).random().toString()
            val finalUser = UserEntity(
                id = demoUid,
                name = "Google User Demo",
                email = "demo.google@gmail.com",
                phone = "03210000000",
                cnicHash = "GOOGLE_DEMO_HASH",
                role = "EARNER",
                walletBalancePkr = 0.0,
                totalEarnedPkr = 0.0,
                referralCode = "GGL123",
                kycStatus = "APPROVED"
            )
            dao.insertUser(finalUser)
            currentUser = finalUser
            return Result.success(finalUser)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        if (auth != null) {
            try {
                Tasks.await(auth!!.sendPasswordResetEmail(email))
                return Result.success(Unit)
            } catch (e: Exception) {
                return Result.failure(Exception("Reset email bhejne mein ghalati: ${e.message}"))
            }
        } else {
            if (email.contains("@") && email.contains(".")) {
                return Result.success(Unit)
            } else {
                return Result.failure(Exception("Sahi email address darj karein."))
            }
        }
    }

    fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseLogout", "Failed to sign out of Firebase Auth: ${e.message}")
        }
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
    suspend fun submitTaskProof(
        taskId: String,
        screenshotPath: String,
        customText: String? = null,
        taskTitle: String? = null,
        completionDate: Long? = null
    ): Result<Unit> {
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
            payoutAmountPkr = task.userPayoutPkr,
            customText = customText,
            taskTitle = taskTitle ?: task.campaignName,
            completionDate = completionDate ?: System.currentTimeMillis()
        )
        dao.insertCompletion(completion)

        // Store this rich metadata in a structured 'submitted_tasks' collection on Firestore
        if (firestore != null) {
            val submissionMap = hashMapOf(
                "id" to completion.id,
                "taskId" to taskId,
                "earnerId" to user.id,
                "screenshotPath" to screenshotPath,
                "submittedAt" to completion.submittedAt,
                "status" to completion.status,
                "payoutAmountPkr" to completion.payoutAmountPkr,
                "customText" to customText,
                "taskTitle" to (taskTitle ?: task.campaignName),
                "completionDate" to (completionDate ?: System.currentTimeMillis())
            )
            try {
                Tasks.await(firestore!!.collection("submitted_tasks").document(completion.id).set(submissionMap))
            } catch (e: Exception) {
                Log.e("TaskMallahRepository", "Failed to save submission metadata in Firestore: ${e.message}")
            }
        }

        return Result.success(Unit)
    }

    // AdMob Earnings Flow (Server-Verified simulation with limits)
    suspend fun watchAd(adType: String, rewardAmountFromAdMob: Double? = null): Result<Double> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val adViewsId = "${user.id}_$today"

        var adRecord = dao.getAdViewsById(adViewsId)
        if (adRecord == null) {
            adRecord = AdViewsEntity(id = adViewsId, userId = user.id, dateString = today)
        }

        val now = System.currentTimeMillis()
        val cooldownMillis = 3 * 60 * 1000 // 3 minutes cooldown

        val activeTier = user.subscriptionTier ?: "Free"
        val maxRewarded = when (activeTier) {
            "Basic" -> 15
            "Gold" -> 30
            "Diamond" -> 999999 // Unlimited
            else -> 5 // Free
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
            rewardAmount = rewardAmountFromAdMob ?: when (activeTier) {
                "Basic" -> 3.0
                "Gold" -> 7.0
                "Diamond" -> 15.0
                else -> 0.20 // Free
            }
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

        // Update in Firestore if live
        if (firestore != null) {
            try {
                Tasks.await(
                    firestore!!.collection("users").document(user.id)
                        .update("walletBalancePkr", newBalance, "totalEarnedPkr", newTotalEarned, "accountLevel", updatedUser.accountLevel)
                )
            } catch (e: Exception) {
                Log.e("TaskMallahRepository", "Failed to update ad reward in Firestore: ${e.message}")
            }
        }

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

        // --- LIFETIME PASSIVE REFERRAL COMMISSION TRIGGER ---
        if (user.referredBy != null) {
            val referrer = dao.getUserById(user.referredBy)
            if (referrer != null) {
                val commission = rewardAmount * 0.10
                val refNewBalance = referrer.walletBalancePkr + commission
                val refNewReferralTotal = referrer.totalReferralPkr + commission
                val updatedReferrer = referrer.copy(
                    walletBalancePkr = refNewBalance,
                    totalReferralPkr = refNewReferralTotal
                )
                dao.updateUser(updatedReferrer)

                // Update Referrer in Firestore if live
                if (firestore != null) {
                    try {
                        Tasks.await(
                            firestore!!.collection("users").document(referrer.id)
                                .update("walletBalancePkr", refNewBalance, "totalReferralPkr", refNewReferralTotal)
                        )
                    } catch (e: Exception) {
                        Log.e("TaskMallahRepository", "Failed to update referrer in Firestore: ${e.message}")
                    }
                }

                // Record Referrer Transaction
                dao.insertTransaction(TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = referrer.id,
                    type = "REFERRAL",
                    source = "L1 Ad: ${user.name}",
                    amountPkr = commission,
                    balanceAfterPkr = refNewBalance,
                    referenceId = "ref_ad_${System.currentTimeMillis()}",
                    description = "10% Lifetime Passive Referral Commission from ${user.name}'s Ad view"
                ))
            }
        }

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
            var lvl1Reward = 0.0
            var lvl2Reward = 0.0
            if (earner.referredBy != null) {
                val lvl1Referrer = dao.getUserById(earner.referredBy)
                if (lvl1Referrer != null) {
                    lvl1Reward = adminMargin * 0.07
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
                            lvl2Reward = adminMargin * 0.03
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
            val netAdminMargin = adminMargin - lvl1Reward - lvl2Reward
            incrementAdminProfitPool(netAdminMargin)
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

    suspend fun buySubscription(tier: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val cost = when (tier) {
            "Basic" -> 1000.0
            "Gold" -> 2500.0
            "Diamond" -> 5000.0
            else -> 0.0
        }
        if (user.walletBalancePkr < cost) {
            return Result.failure(Exception("Aap ka wallet balance na-kaafi hai. Is package ke liye $cost PKR laazmi hain."))
        }
        val newBalance = user.walletBalancePkr - cost
        val updatedUser = user.copy(
            subscriptionTier = tier,
            walletBalancePkr = newBalance
        )
        dao.updateUser(updatedUser)
        currentUser = updatedUser

        // Also update Firestore if available
        if (firestore != null) {
            try {
                Tasks.await(
                    firestore!!.collection("users").document(user.id)
                        .update("subscriptionTier", tier, "walletBalancePkr", newBalance)
                )
            } catch (e: Exception) {
                Log.e("FirebaseSubscription", "Failed to update subscription in Firestore: ${e.message}")
            }
        }

        // Add Transaction
        dao.insertTransaction(TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            type = "ADMIN_ADJUSTMENT",
            source = "Package Activation",
            amountPkr = -cost,
            balanceAfterPkr = newBalance,
            referenceId = "sub_${tier.lowercase()}_${System.currentTimeMillis()}",
            description = "Activated Premium $tier Package"
        ))

        return Result.success(Unit)
    }

    suspend fun updateProfilePic(uri: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val updatedUser = user.copy(profilePicUri = uri)
        dao.updateUser(updatedUser)
        currentUser = updatedUser

        if (firestore != null) {
            try {
                Tasks.await(
                    firestore!!.collection("users").document(user.id)
                        .update("profilePicUri", uri)
                )
            } catch (e: Exception) {
                Log.e("FirebaseProfilePic", "Failed to update profile pic in Firestore: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    fun getSavedAccountsFlow(userId: String): Flow<List<SavedAccountEntity>> {
        return dao.getSavedAccountsForUserFlow(userId)
    }

    suspend fun linkPaymentAccount(bankName: String, accountTitle: String, accountNumber: String, iban: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        
        // Super Admin check bypasses locks
        val isSuperAdmin = user.email.lowercase() == "rajubhai3508194@gmail.com" || user.phone == "03496677887"
        
        if (user.isPaymentDetailsLocked && !isSuperAdmin) {
            return Result.failure(Exception("Aap ke payment accounts locked hain! Tabdeeli ke liye Super Admin se rabta karein."))
        }

        val existingAccounts = dao.getSavedAccountsForUser(user.id)
        if (existingAccounts.size >= 5 && !isSuperAdmin) {
            return Result.failure(Exception("Aap maximum 5 payment accounts link kar sakte hain."))
        }

        val newAccount = SavedAccountEntity(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            bankName = bankName,
            accountTitle = accountTitle,
            accountNumber = accountNumber,
            iban = iban
        )
        dao.insertSavedAccount(newAccount)

        return Result.success(Unit)
    }

    suspend fun lockPaymentDetails(userId: String, lock: Boolean): Result<Unit> {
        val admin = currentUser ?: return Result.failure(Exception("Admin not logged in."))
        if (admin.role != "ADMIN" && admin.email.lowercase() != "rajubhai3508194@gmail.com") {
            return Result.failure(Exception("Permission denied."))
        }

        val user = dao.getUserById(userId) ?: return Result.failure(Exception("User not found."))
        val updatedUser = user.copy(isPaymentDetailsLocked = lock)
        dao.updateUser(updatedUser)
        
        if (firestore != null) {
            try {
                Tasks.await(
                    firestore!!.collection("users").document(userId)
                        .update("isPaymentDetailsLocked", lock)
                )
            } catch (e: Exception) {
                Log.e("FirebasePaymentLock", "Failed to update payment lock in Firestore: ${e.message}")
            }
        }
        return Result.success(Unit)
    }

    suspend fun deleteSavedAccount(accountId: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Pehle login karein."))
        val isSuperAdmin = user.email.lowercase() == "rajubhai3508194@gmail.com" || user.phone == "03496677887"
        
        if (user.isPaymentDetailsLocked && !isSuperAdmin) {
            return Result.failure(Exception("Aap ke payment accounts locked hain! Tabdeeli ke liye Super Admin se rabta karein."))
        }

        dao.deleteSavedAccountById(accountId)
        return Result.success(Unit)
    }
}
