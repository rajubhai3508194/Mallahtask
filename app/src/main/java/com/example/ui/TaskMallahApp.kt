package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.IslamicGreen
import com.example.ui.theme.HalalGold
import com.example.ui.theme.HalalGoldDark
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.TaskMallahViewModel

sealed class AppScreen {
    object Splash : AppScreen()
    object Onboarding : AppScreen()
    object AuthSelection : AppScreen()
    object Register : AppScreen()
    object Login : AppScreen()
    object OTPVerify : AppScreen()
    object Dashboard : AppScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskMallahApp(viewModel: TaskMallahViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }

    val currentUser by viewModel.currentUser.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()

    // Handle Auth success navigation
    val authSuccess by viewModel.authSuccess.collectAsState()
    LaunchedEffect(authSuccess) {
        if (authSuccess != null) {
            Toast.makeText(context, authSuccess, Toast.LENGTH_LONG).show()
            currentScreen = AppScreen.Dashboard
            viewModel.clearAuthMessages()
        }
    }

    // Handle Generic Toasts
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Scaffold for the entire app navigation
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.Splash -> SplashScreen {
                    currentScreen = AppScreen.Onboarding
                }
                AppScreen.Onboarding -> OnboardingScreen {
                    currentScreen = AppScreen.AuthSelection
                }
                AppScreen.AuthSelection -> AuthSelectionScreen(
                    onLoginClick = { currentScreen = AppScreen.Login },
                    onRegisterClick = { currentScreen = AppScreen.Register }
                )
                AppScreen.Register -> RegisterScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.AuthSelection },
                    onSuccess = { currentScreen = AppScreen.OTPVerify }
                )
                AppScreen.Login -> LoginScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreen.AuthSelection }
                )
                AppScreen.OTPVerify -> OTPVerifyScreen {
                    currentScreen = AppScreen.Dashboard
                }
                AppScreen.Dashboard -> DashboardPortal(
                    viewModel = viewModel,
                    onLogout = {
                        viewModel.logout()
                        currentScreen = AppScreen.AuthSelection
                    }
                )
            }

            if (isProcessing) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = IslamicGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Bara-e-Meherbani Sabr Karein...", color = IslamicGreen, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ----------------- SCREEN: SPLASH -----------------
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F3E22), Color(0xFF1B8C4E), Color(0xFF121814))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Elegant Vector Icon Frame
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, HalalGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "Wallet",
                    tint = HalalGold,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TaskMallah",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "RozRoz Kamao",
                color = HalalGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "\"Har Kaam Ka Inaam — Sachcha, Saaf, Saath\"",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = HalalGold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}

// ----------------- SCREEN: ONBOARDING -----------------
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val slides = listOf(
        OnboardingSlide(
            title = "Aasan Halal Earning",
            description = "YouTube watch, TikTok follow, Daraz reviews aur Google Maps reviews jaise aasan kaam mukammal karein aur real PKR kamaein.",
            icon = Icons.Default.VolunteerActivism
        ),
        OnboardingSlide(
            title = "Apna Business Promote Karein",
            description = "Advertiser ban kar apni social media presence barhain, followers aur reviews hasil karein bilkul saste rates par.",
            icon = Icons.Default.Campaign
        ),
        OnboardingSlide(
            title = "100% Mehfooz aur Transparent",
            description = "Har task verify hota hai, aur withdraw EasyPaisa, JazzCash ya bank transfer ke zariye seedha aap ke account mein ata hai.",
            icon = Icons.Default.Security
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(IslamicGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slides[currentPage].icon,
                    contentDescription = null,
                    tint = IslamicGreen,
                    modifier = Modifier.size(80.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = slides[currentPage].title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = slides[currentPage].description,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slides.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentPage) IslamicGreen else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = {
                    if (currentPage < slides.lastIndex) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (currentPage == slides.lastIndex) "Shuru Karein" else "Agla Page",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

data class OnboardingSlide(val title: String, val description: String, val icon: ImageVector)

// ----------------- SCREEN: AUTH SELECTION -----------------
@Composable
fun AuthSelectionScreen(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CurrencyLira,
                    contentDescription = null,
                    tint = IslamicGreen,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("TaskMallah PK", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                Text("Pakistan Ka Sab Se Bara Task Marketplace", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_login_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Login Karein", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_register_btn"),
                    border = BorderStroke(2.dp, IslamicGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Naya Account Banayein", color = IslamicGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Text(
                "By signing up, you agree to our Terms of Service & Privacy Policy.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ----------------- SCREEN: REGISTER -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: TaskMallahViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }

    // Form inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }

    val authError by viewModel.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (step > 1) step-- else onBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = IslamicGreen)
            }
            Text("Naya Account (Step $step/2)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (authError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = authError!!,
                    color = ErrorRed,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (step == 1) {
            Text("Zati Maalomat (Personal Details)", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Mukammal Naam (Alphabets Only)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = IslamicGreen) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IslamicGreen) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Mobile Number (03XXXXXXXXX)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = IslamicGreen) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.length < 3) {
                        viewModel.loginUser("") // Trigger dummy failure check or validation
                        Toast.makeText(context, "Sahi naam darj karein.", Toast.LENGTH_SHORT).show()
                    } else {
                        step = 2
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
            ) {
                Text("Agla Step", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Security & CNIC Info", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cnic,
                onValueChange = { if (it.length <= 13) cnic = it },
                label = { Text("CNIC Number (13 Digits, No dashes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = IslamicGreen) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (Min 8 chars)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IslamicGreen) }
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = referralCode,
                onValueChange = { referralCode = it },
                label = { Text("Referral Code (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = IslamicGreen) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.registerUser(name, email, phone, password, cnic, referralCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
            ) {
                Text("Register Karein", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- SCREEN: LOGIN -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: TaskMallahViewModel, onBack: () -> Unit) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authError by viewModel.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = IslamicGreen)
            }
            Text("Login Account", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (authError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = authError!!,
                    color = ErrorRed,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it },
            label = { Text("Email, Mobile ya Referral Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = IslamicGreen) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = IslamicGreen) }
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.loginUser(identifier)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_login_button"),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Super Admin Accounts Se Login Kar Sakte Hain:\nEmail: rajubhai3508194@gmail.com\nPhone: 03496677887",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ----------------- SCREEN: OTP VERIFY -----------------
@Composable
fun OTPVerifyScreen(onVerify: () -> Unit) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Sms, contentDescription = null, tint = IslamicGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("OTP Phone Verification", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Hum ne aap ke mobile number par 6-digits ka OTP SMS bheja hai.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(0.7f))
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Verification Code") },
            modifier = Modifier.width(200.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
        ) {
            Text("Tasdeeq Karein (Verify)", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- COMPONENT: DASHBOARD PORTAL -----------------
@Composable
fun DashboardPortal(viewModel: TaskMallahViewModel, onLogout: () -> Unit) {
    val activeRole by viewModel.activeRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedBottomTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // Main dynamic navigation items based on active role
                NavigationBarItem(
                    selected = selectedBottomTab == 0,
                    onClick = { selectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                if (activeRole == "EARNER") {
                    NavigationBarItem(
                        selected = selectedBottomTab == 1,
                        onClick = { selectedBottomTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Browse Tasks") },
                        label = { Text("Tasks") }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 2,
                        onClick = { selectedBottomTab = 2 },
                        icon = { Icon(Icons.Default.MonetizationOn, contentDescription = "AdMob Earn") },
                        label = { Text("Ads Earn") }
                    )
                } else if (activeRole == "ADVERTISER") {
                    NavigationBarItem(
                        selected = selectedBottomTab == 1,
                        onClick = { selectedBottomTab = 1 },
                        icon = { Icon(Icons.Default.AddBox, contentDescription = "Create Camp") },
                        label = { Text("Create") }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 2,
                        onClick = { selectedBottomTab = 2 },
                        icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Campaigns") },
                        label = { Text("Campaigns") }
                    )
                } else if (activeRole == "ADMIN") {
                    NavigationBarItem(
                        selected = selectedBottomTab == 1,
                        onClick = { selectedBottomTab = 1 },
                        icon = { Icon(Icons.Default.FactCheck, contentDescription = "Approvals") },
                        label = { Text("Reviews") }
                    )
                    NavigationBarItem(
                        selected = selectedBottomTab == 2,
                        onClick = { selectedBottomTab = 2 },
                        icon = { Icon(Icons.Default.Group, contentDescription = "Users") },
                        label = { Text("Users") }
                    )
                }
                NavigationBarItem(
                    selected = selectedBottomTab == 3,
                    onClick = { selectedBottomTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (activeRole) {
                "EARNER" -> {
                    when (selectedBottomTab) {
                        0 -> EarnerHomeScreen(viewModel)
                        1 -> EarnerTaskBrowserScreen(viewModel)
                        2 -> AdMobEarningScreen(viewModel)
                        3 -> ProfileScreen(viewModel, onLogout)
                    }
                }
                "ADVERTISER" -> {
                    when (selectedBottomTab) {
                        0 -> AdvertiserHomeScreen(viewModel)
                        1 -> CreateCampaignScreen(viewModel)
                        2 -> CampaignListScreen(viewModel)
                        3 -> ProfileScreen(viewModel, onLogout)
                    }
                }
                "ADMIN" -> {
                    when (selectedBottomTab) {
                        0 -> AdminHomeScreen(viewModel)
                        1 -> AdminReviewsScreen(viewModel)
                        2 -> AdminUsersScreen(viewModel)
                        3 -> ProfileScreen(viewModel, onLogout)
                    }
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: EARNER HOME -----------------
@Composable
fun EarnerHomeScreen(viewModel: TaskMallahViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.userTransactions.collectAsState()
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Elegant Greeting & Brand Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Assalam-o-Alaikum,", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text(currentUser?.name ?: "TaskMallah Partner", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                }
                Box(
                    modifier = Modifier
                        .background(IslamicGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Lvl: ${currentUser?.accountLevel ?: "Bronze"}",
                        color = IslamicGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            // HALAL PKR WALLET DISPLAY
            Card(
                colors = CardDefaults.cardColors(containerColor = IslamicGreen),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Kul Earning (Halal Wallet Balance)", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${"%.2f".format(currentUser?.walletBalancePkr ?: 0.0)} PKR",
                        color = HalalGold,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Earned", color = Color.White.copy(0.7f), fontSize = 11.sp)
                            Text("${"%.2f".format(currentUser?.totalEarnedPkr ?: 0.0)} PKR", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Withdrawn", color = Color.White.copy(0.7f), fontSize = 11.sp)
                            Text("${"%.2f".format(currentUser?.totalWithdrawnPkr ?: 0.0)} PKR", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showWithdrawalDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HalalGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("earner_withdrawal_button")
                    ) {
                        Text("Pesa Nikalein (Withdraw)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // DAILY PROGRESS CHART / STATS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Earning Weekly Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IslamicGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    // Drawing a beautiful weekly bar chart
                    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val earnings = listOf(50f, 120f, 240f, 80f, 150f, 320f, 110f)
                        val maxEarning = 400f
                        val barWidth = 35.dp.toPx()
                        val spacing = (size.width - (barWidth * days.size)) / (days.size + 1)

                        days.forEachIndexed { idx, day ->
                            val left = spacing + idx * (barWidth + spacing)
                            val barHeight = (earnings[idx] / maxEarning) * size.height
                            val top = size.height - barHeight

                            // Draw Bar
                            drawRect(
                                color = IslamicGreen.copy(alpha = if (idx == 5) 1.0f else 0.4f),
                                topLeft = Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                        }
                    }
                }
            }
        }

        item {
            // TRANSACTIONS HISTORY HEADER
            Text("Earning History (Transactions)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IslamicGreen)
        }

        if (transactions.isEmpty()) {
            item {
                Text(
                    "Abhi tak koi transaction nahi hui. Tasks complete karein aur earning shuru karein!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        } else {
            items(transactions) { txn ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.onBackground.copy(0.05f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(if (txn.amountPkr > 0) SuccessGreen.copy(0.1f) else ErrorRed.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (txn.amountPkr > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (txn.amountPkr > 0) SuccessGreen else ErrorRed
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(txn.source, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(txn.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                        }
                        Text(
                            text = "${if (txn.amountPkr > 0) "+" else ""}${txn.amountPkr} PKR",
                            color = if (txn.amountPkr > 0) SuccessGreen else ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // WITHDRAWAL REQUEST SCREEN SHEET
    if (showWithdrawalDialog) {
        var withdrawAmount by remember { mutableStateOf("") }
        var selectedMethod by remember { mutableStateOf("EasyPaisa") }
        var accountTitle by remember { mutableStateOf("") }
        var accountNo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showWithdrawalDialog = false },
            title = { Text("Pesa Nikalyein (Withdraw)", color = IslamicGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Minimum Withdrawal amount 200 PKR hai. KYC approved hona laazmi hai.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it },
                        label = { Text("Raqam (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Method:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EasyPaisa", "JazzCash", "SadaPay", "Bank").forEach { method ->
                            Box(
                                modifier = Modifier
                                    .border(2.dp, if (selectedMethod == method) IslamicGreen else Color.Gray.copy(0.3f), RoundedCornerShape(8.dp))
                                    .background(if (selectedMethod == method) IslamicGreen.copy(0.1f) else Color.Transparent)
                                    .clickable { selectedMethod = method }
                                    .padding(8.dp)
                            ) {
                                Text(method, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = accountTitle,
                        onValueChange = { accountTitle = it },
                        label = { Text("Account Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accountNo,
                        onValueChange = { accountNo = it },
                        label = { Text("Mobile Number / IBAN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = withdrawAmount.toDoubleOrNull()
                        if (amt != null) {
                            viewModel.submitWithdrawal(amt, selectedMethod, accountTitle, accountNo)
                            showWithdrawalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                ) {
                    Text("Request Withdraw")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------- SUB-SCREEN: EARNER TASK BROWSER -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnerTaskBrowserScreen(viewModel: TaskMallahViewModel) {
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.taskSearchQuery.collectAsState()
    val selectedFilter by viewModel.selectedPlatformFilter.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()

    var showTaskDetail by remember { mutableStateOf(false) }

    val platforms = listOf("YouTube", "TikTok", "Instagram", "Google Play Store", "Google Maps", "Spotify")

    if (showTaskDetail && selectedTask != null) {
        TaskDetailSheet(viewModel = viewModel) {
            showTaskDetail = false
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Kaam Talash Karein", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Keywords se search karein...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Platform Filter Row
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Filter By Platform:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (selectedFilter == null) IslamicGreen else Color.Gray.copy(0.3f), RoundedCornerShape(16.dp))
                                .background(if (selectedFilter == null) IslamicGreen.copy(0.1f) else Color.Transparent)
                                .clickable { viewModel.selectPlatformFilter(null) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        platforms.take(3).forEach { p ->
                            Box(
                                modifier = Modifier
                                    .border(1.dp, if (selectedFilter == p) IslamicGreen else Color.Gray.copy(0.3f), RoundedCornerShape(16.dp))
                                    .background(if (selectedFilter == p) IslamicGreen.copy(0.1f) else Color.Transparent)
                                    .clickable { viewModel.selectPlatformFilter(p) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(p, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Available Campaigns:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IslamicGreen)
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Koi naya campaign nahi mila.", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredTasks) { task ->
                        Card(
                            onClick = {
                                viewModel.selectTask(task)
                                showTaskDetail = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(0.08f), RoundedCornerShape(16.dp))
                                .testTag("task_item_${task.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(IslamicGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(task.platform, color = IslamicGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Text(
                                        text = "${task.userPayoutPkr} PKR",
                                        color = HalalGoldDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(task.campaignName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(task.taskType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- DETAIL SHEET: TASK DETAILS & SUBMISSION -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(viewModel: TaskMallahViewModel, onBack: () -> Unit) {
    val task by viewModel.selectedTask.collectAsState()
    val completion by viewModel.selectedTaskCompletion.collectAsState()

    var screenshotMockPath by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = IslamicGreen)
            }
            Text("Campaign Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        }
        Spacer(modifier = Modifier.height(16.dp))

        task?.let { t ->
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IslamicGreen.copy(0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(t.campaignName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = IslamicGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Platform: ${t.platform}", fontWeight = FontWeight.Medium)
                                Text("Payout: ${t.userPayoutPkr} PKR", color = HalalGoldDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Hidayat (Instructions):", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IslamicGreen)
                    Text(t.instructions, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, modifier = Modifier.padding(top = 4.dp))
                }

                item {
                    Button(
                        onClick = {
                            // In real app, open target URL in external browser. We trigger simulated browser open
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Target Platform Link")
                    }
                }

                item {
                    Divider()
                }

                item {
                    Text("Apna Saboot Upload Karein (Upload Proof Screenshot):", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IslamicGreen)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (completion != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (completion!!.status) {
                                    "APPROVED" -> SuccessGreen.copy(0.1f)
                                    "REJECTED" -> ErrorRed.copy(0.1f)
                                    else -> Color.Gray.copy(0.1f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Status: ${completion!!.status}", fontWeight = FontWeight.Bold, color = if (completion!!.status == "APPROVED") SuccessGreen else if (completion!!.status == "REJECTED") ErrorRed else Color.Gray)
                                if (completion!!.status == "REJECTED") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Rejection Reason: ${completion!!.rejectionReason}", color = ErrorRed)
                                }
                            }
                        }
                    } else {
                        // Simulated upload
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { screenshotMockPath = "proof_${System.currentTimeMillis()}.png" },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Photo")
                            }
                            if (screenshotMockPath.isNotEmpty()) {
                                Text("Selected: $screenshotMockPath", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (completion == null && screenshotMockPath.isNotEmpty()) {
                    item {
                        Button(
                            onClick = {
                                viewModel.submitTaskCompletionProof(t.id, screenshotMockPath)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HalalGold),
                            modifier = Modifier.fillMaxWidth().testTag("submit_proof_button")
                        ) {
                            Text("Submit Task Proof", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: ADMOB ADS EARNING -----------------
@Composable
fun AdMobEarningScreen(viewModel: TaskMallahViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LiveTv, contentDescription = null, tint = IslamicGreen, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("AdMob Ad Watching Earning", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Video ads dekh kar extra PKR kamaein. Server-side automatic authentication system complete hai.", textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.watchAdMobAd("REWARDED") },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("watch_rewarded_ad"),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayCircleOutline, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Watch Rewarded Ad (0.35 PKR)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.watchAdMobAd("INTERSTITIAL") },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("watch_interstitial_ad"),
            border = BorderStroke(2.dp, IslamicGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Tv, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Show Interstitial Ad (0.21 PKR)", color = IslamicGreen, fontWeight = FontWeight.Bold)
        }
    }
}

// ----------------- SUB-SCREEN: ADVERTISER HOME -----------------
@Composable
fun AdvertiserHomeScreen(viewModel: TaskMallahViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val campaigns by viewModel.advertiserCampaigns.collectAsState()

    var showDepositDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Advertiser Portal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Campaign Funds (Balance)", fontSize = 13.sp)
                    Text("${"%.2f".format(currentUser?.walletBalancePkr ?: 0.0)} PKR", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDepositDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PKR Deposit Karein", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Campaigns", fontSize = 12.sp)
                        Text("${campaigns.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Engagements Received", fontSize = 12.sp)
                        val totalSlots = campaigns.sumOf { it.slotsFilled }
                        Text("$totalSlots", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HalalGoldDark)
                    }
                }
            }
        }

        item {
            Text("Campaign Performance Analytics", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IslamicGreen)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Overall Campaigns Success Rate", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = 0.85f,
                        color = IslamicGreen,
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("85% tasks completed and approved correctly.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }

    if (showDepositDialog) {
        var depAmount by remember { mutableStateOf("") }
        var depMethod by remember { mutableStateOf("EasyPaisa") }

        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = { Text("Deposit Payment Proof", color = IslamicGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Hamare EasyPaisa number (03000856623) par deposit bhej kar proof submit karein.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = depAmount,
                        onValueChange = { depAmount = it },
                        label = { Text("Amount (Min 500)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = depMethod,
                        onValueChange = { depMethod = it },
                        label = { Text("Method (e.g., EasyPaisa, JazzCash, HBL)") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depAmount.toDoubleOrNull()
                        if (amt != null) {
                            viewModel.submitDepositPayment(amt, depMethod, "proof_screenshot.png")
                            showDepositDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                ) {
                    Text("Submit Proof")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ----------------- SUB-SCREEN: ADVERTISER CREATE CAMPAIGN -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignScreen(viewModel: TaskMallahViewModel) {
    var platform by remember { mutableStateOf("YouTube") }
    var taskType by remember { mutableStateOf("Watch + Like + Sub") }
    var name by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var slots by remember { mutableStateOf("") }
    var pricePerSlot by remember { mutableStateOf("5.0") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Naya Campaign Banayein", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Campaign Naam (e.g. YouTube Subscribe)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("Target Platform (e.g. TikTok, YouTube, maps)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = taskType,
                onValueChange = { taskType = it },
                label = { Text("Task Type (e.g. Follow, 5-Star Review)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = targetUrl,
                onValueChange = { targetUrl = it },
                label = { Text("Target Link (URL)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Instructions / Hidayat (Earner kya kare?)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = slots,
                    onValueChange = { slots = it },
                    label = { Text("Slots (Min 10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pricePerSlot,
                    onValueChange = { pricePerSlot = it },
                    label = { Text("Price Per Task") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val totalCost = (slots.toIntOrNull() ?: 0) * (pricePerSlot.toDoubleOrNull() ?: 0.0)
            Card(colors = CardDefaults.cardColors(containerColor = IslamicGreen.copy(0.05f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kul Kharch (Total Cost): $totalCost PKR", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IslamicGreen)
                    Text("Note: 30% Admin Margin shaamil hai, 70% earner ko milega.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        item {
            Button(
                onClick = {
                    val s = slots.toIntOrNull()
                    val p = pricePerSlot.toDoubleOrNull()
                    if (s != null && p != null && name.isNotEmpty()) {
                        viewModel.submitCampaign(platform, taskType, name, targetUrl, instructions, s, p)
                        name = ""
                        targetUrl = ""
                        instructions = ""
                        slots = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_campaign_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
            ) {
                Text("Publish Campaign", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------- SUB-SCREEN: ADVERTISER CAMPAIGN LIST -----------------
@Composable
fun CampaignListScreen(viewModel: TaskMallahViewModel) {
    val campaigns by viewModel.advertiserCampaigns.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Aap ke Campaigns", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        Spacer(modifier = Modifier.height(12.dp))

        if (campaigns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Koi campaign nahi banaya gaya abhi tak.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(campaigns) { camp ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(camp.campaignName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Slots: ${camp.slotsFilled}/${camp.totalSlots}", fontSize = 13.sp)
                                Text("Budget: ${camp.totalSlots * camp.advPricePkr} PKR", color = IslamicGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Status: ${camp.status}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (camp.status == "ACTIVE") SuccessGreen else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: SUPER ADMIN OVERVIEW -----------------
@Composable
fun AdminHomeScreen(viewModel: TaskMallahViewModel) {
    val pendingCompletions by viewModel.adminPendingCompletions.collectAsState()
    val pendingKyc by viewModel.adminPendingKyc.collectAsState()
    val pendingDeposits by viewModel.adminPendingDeposits.collectAsState()
    val pendingWithdrawals by viewModel.adminPendingWithdrawals.collectAsState()
    val allUsers by viewModel.adminAllUsers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Super Admin Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Admin Profit Pool", fontSize = 12.sp)
                    Text("34,250.00 PKR", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                    Text("Overall float in system: 1,450,000 PKR", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        item {
            Text("System Action Pending Queue", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IslamicGreen)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Task Verification", fontSize = 11.sp)
                        Text("${pendingCompletions.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("KYC Verifications", fontSize = 11.sp)
                        Text("${pendingKyc.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Deposits Queue", fontSize = 11.sp)
                        Text("${pendingDeposits.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HalalGoldDark)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Withdrawals Queue", fontSize = 11.sp)
                        Text("${pendingWithdrawals.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HalalGoldDark)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Platform Users", fontWeight = FontWeight.SemiBold)
                    Text("${allUsers.size}", fontWeight = FontWeight.Bold, color = IslamicGreen)
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: SUPER ADMIN REVIEWS QUEUE -----------------
@Composable
fun AdminReviewsScreen(viewModel: TaskMallahViewModel) {
    val pendingCompletions by viewModel.adminPendingCompletions.collectAsState()
    val pendingKyc by viewModel.adminPendingKyc.collectAsState()

    var activeTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pending Approvals Queue", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        Spacer(modifier = Modifier.height(12.dp))

        ScrollableTabRow(selectedTabIndex = activeTab) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Task Screenshots (${pendingCompletions.size})", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("KYC Submissions (${pendingKyc.size})", modifier = Modifier.padding(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == 0) {
            if (pendingCompletions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Koi task screenshot review queue mein nahi hai.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pendingCompletions) { comp ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Completion ID: ${comp.id}", fontWeight = FontWeight.Bold)
                                Text("Proof Path: ${comp.screenshotPath}", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.approveOrRejectTaskCompletion(comp.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Text("Approve")
                                    }
                                    Button(
                                        onClick = { viewModel.approveOrRejectTaskCompletion(comp.id, false, "Proof does not match criteria.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (pendingKyc.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Koi KYC submission review queue mein nahi hai.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pendingKyc) { kyc ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("User ID: ${kyc.userId}", fontWeight = FontWeight.Bold)
                                Text("CNIC Number: ${kyc.cnicNumber}", fontWeight = FontWeight.SemiBold, color = IslamicGreen)
                                Text("Front Path: ${kyc.cnicFrontPath}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.approveOrRejectKyc(kyc.userId, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Text("Approve")
                                    }
                                    Button(
                                        onClick = { viewModel.approveOrRejectKyc(kyc.userId, false, "CNIC details blurry.") },
                                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: SUPER ADMIN USERS LIST -----------------
@Composable
fun AdminUsersScreen(viewModel: TaskMallahViewModel) {
    val allUsers by viewModel.adminAllUsers.collectAsState()

    var showAdjustDialog by remember { mutableStateOf<UserEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("User Management (Audit)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(allUsers) { user ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(user.role, color = IslamicGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Email: ${user.email}", fontSize = 13.sp)
                        Text("Phone: ${user.phone}", fontSize = 13.sp)
                        Text("Balance: ${user.walletBalancePkr} PKR", fontWeight = FontWeight.Bold, color = HalalGoldDark)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAdjustDialog = user },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                            ) {
                                Text("Adjust Balance")
                            }
                            Button(
                                onClick = { viewModel.moderateUserBan(user.id, !user.isBanned, "Security audit ban.") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) SuccessGreen else ErrorRed)
                            ) {
                                Text(if (user.isBanned) "Unban" else "Ban User")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog != null) {
        var adjustAmt by remember { mutableStateOf("") }
        var adjReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdjustDialog = null },
            title = { Text("Adjust Wallet: ${showAdjustDialog!!.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = adjustAmt,
                        onValueChange = { adjustAmt = it },
                        label = { Text("Amount (+ or -)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = adjReason,
                        onValueChange = { adjReason = it },
                        label = { Text("Reason for audit trail") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = adjustAmt.toDoubleOrNull()
                        if (amt != null && adjReason.isNotEmpty()) {
                            viewModel.manualWalletAdjust(showAdjustDialog!!.id, amt, adjReason)
                            showAdjustDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                ) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = null }) { Text("Cancel") }
            }
        )
    }
}

// ----------------- SUB-SCREEN: PROFILE & SETTINGS -----------------
@Composable
fun ProfileScreen(viewModel: TaskMallahViewModel, onLogout: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()

    var cnicNo by remember { mutableStateOf("") }
    var mockFrontPath by remember { mutableStateOf("") }
    var mockBackPath by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(IslamicGreen.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccountBox, contentDescription = null, tint = IslamicGreen, modifier = Modifier.size(54.dp))
            }
        }

        item {
            Text(currentUser?.name ?: "User Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
            Text(currentUser?.email ?: "", fontSize = 13.sp, color = Color.Gray)
        }

        item {
            // KYC Verified Status Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (currentUser?.kycStatus) {
                            "APPROVED" -> SuccessGreen.copy(0.15f)
                            "PENDING" -> HalalGold.copy(0.15f)
                            else -> ErrorRed.copy(0.15f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "KYC Status: ${currentUser?.kycStatus ?: "NOT_SUBMITTED"}",
                    color = when (currentUser?.kycStatus) {
                        "APPROVED" -> SuccessGreen
                        "PENDING" -> HalalGoldDark
                        else -> ErrorRed
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Divider()
        }

        item {
            // Switch Portal Role Button
            Text("Switch Role Portal:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.switchActiveRole("EARNER") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (activeRole == "EARNER") IslamicGreen else Color.Gray)
                ) {
                    Text("Earner")
                }
                Button(
                    onClick = { viewModel.switchActiveRole("ADVERTISER") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (activeRole == "ADVERTISER") IslamicGreen else Color.Gray)
                ) {
                    Text("Advertiser")
                }
                // Raju Bhai / Admin Check
                if (currentUser?.email?.lowercase() == "rajubhai3508194@gmail.com" || currentUser?.phone == "03496677887") {
                    Button(
                        onClick = { viewModel.switchActiveRole("ADMIN") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (activeRole == "ADMIN") IslamicGreen else Color.Gray)
                    ) {
                        Text("Admin")
                    }
                }
            }
        }

        // KYC Form if not submitted
        if (currentUser?.kycStatus == "NOT_SUBMITTED" || currentUser?.kycStatus == "REJECTED") {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Submit KYC Verification (CNIC)", fontWeight = FontWeight.Bold, color = IslamicGreen)
                        OutlinedTextField(
                            value = cnicNo,
                            onValueChange = { if (it.length <= 13) cnicNo = it },
                            label = { Text("13 Digits CNIC Number") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { mockFrontPath = "cnic_front_${System.currentTimeMillis()}.jpg" },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Front CNIC")
                            }
                            Button(
                                onClick = { mockBackPath = "cnic_back_${System.currentTimeMillis()}.jpg" },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back CNIC")
                            }
                        }
                        if (mockFrontPath.isNotEmpty() && mockBackPath.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.submitKycDetails(cnicNo, mockFrontPath, mockBackPath) },
                                colors = ButtonDefaults.buttonColors(containerColor = HalalGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Submit KYC", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            // Logout
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}
