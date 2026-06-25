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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.data.AdMobManager
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

@Composable
fun MuslimAvatarView(avatarName: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                when (avatarName) {
                    "Green Crescent" -> IslamicGreen
                    "Golden Dome" -> Color(0xFF1E1E24)
                    "Islamic Geometry" -> Color(0xFF008080)
                    "Kaaba Minimalist" -> Color(0xFF111111)
                    "Raju Bhai" -> Color(0xFF0D47A1)
                    else -> IslamicGreen.copy(0.1f)
                },
                CircleShape
            )
            .border(
                2.dp,
                when (avatarName) {
                    "Golden Dome" -> HalalGold
                    "Kaaba Minimalist" -> HalalGold
                    "Raju Bhai" -> HalalGold
                    else -> Color.Transparent
                },
                CircleShape
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (avatarName) {
            "Green Crescent" -> {
                Icon(Icons.Default.Brightness3, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            "Golden Dome" -> {
                Icon(Icons.Default.Castle, contentDescription = null, tint = HalalGold, modifier = Modifier.size(36.dp))
            }
            "Islamic Geometry" -> {
                Icon(Icons.Default.Filter8, contentDescription = null, tint = HalalGold, modifier = Modifier.size(36.dp))
            }
            "Kaaba Minimalist" -> {
                Icon(Icons.Default.CropSquare, contentDescription = null, tint = HalalGold, modifier = Modifier.size(36.dp))
            }
            "Raju Bhai" -> {
                Text("RB", color = HalalGold, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            else -> {
                Icon(Icons.Default.AccountBox, contentDescription = null, tint = IslamicGreen, modifier = Modifier.size(54.dp))
            }
        }
    }
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

    // --- ANTI-FRAUD SECURITY DETECTIONS ---
    var isEmulatorDetected by remember { mutableStateOf(SecurityEngine.isEmulator()) }
    var isRootDetected by remember { mutableStateOf(SecurityEngine.isRooted()) }
    var isVpnDetected by remember { mutableStateOf(SecurityEngine.isVpnActive(context)) }
    var isSandboxBypassActive by remember { mutableStateOf(true) } // Enabled by default to not brick the browser preview
    var showSecurityPanel by remember { mutableStateOf(false) }

    // Periodically update VPN status
    LaunchedEffect(Unit) {
        while (true) {
            isVpnDetected = SecurityEngine.isVpnActive(context)
            kotlinx.coroutines.delay(5000)
        }
    }

    // Handle Auth success navigation
    val authSuccess by viewModel.authSuccess.collectAsState()
    LaunchedEffect(authSuccess) {
        if (authSuccess != null) {
            Toast.makeText(context, authSuccess, Toast.LENGTH_LONG).show()
            currentScreen = AppScreen.Dashboard
            viewModel.clearAuthMessages()
        }
    }

    // Handle OTP pending navigation
    val isOtpRequired by viewModel.isOtpRequired.collectAsState()
    LaunchedEffect(isOtpRequired) {
        if (isOtpRequired) {
            currentScreen = AppScreen.OTPVerify
            viewModel.clearOtpRequired()
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
            val isBlocked = !isSandboxBypassActive && (isEmulatorDetected || isRootDetected)

            if (isBlocked) {
                // --- DESIGN: BEAUTIFUL ANTI-FRAUD PERMANENT BLOCK SCREEN ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D1117))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Lock Header Visual
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(ErrorRed.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Device Blocked",
                            tint = ErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Device Blocked / اکاؤنٹ بلاک",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "TaskMallah Anti-Cheat Engine",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HalalGold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Urdu:",
                                fontWeight = FontWeight.Bold,
                                color = HalalGold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Emulator ya Rooted mobile detect hua hai! TaskMallah ke safety rules ke tehat is device par app chalana sakht mamnoo hai. Baraye meherbani asan micro-tasks karne ke liye real physical Android mobile device istemal karein.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "English:",
                                fontWeight = FontWeight.Bold,
                                color = HalalGold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Emulator or Rooted device environment detected! To preserve the organic traffic quality for advertisers, TaskMallah prohibits completion of tasks on simulated, emulated, or root-compromised systems.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Simulated diagnostics info
                    Text(
                        text = "Device Hash: ${SecurityEngine.getDeviceUuid(context).take(20).uppercase()}...\n" +
                                "Emulator: $isEmulatorDetected | Rooted: $isRootDetected",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    // Developer Bypass Option so they are never stuck
                    OutlinedButton(
                        onClick = { isSandboxBypassActive = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HalalGold),
                        border = BorderStroke(1.dp, HalalGold)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bypass Shield (For Testing)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Normal App Screens
                Column(modifier = Modifier.fillMaxSize()) {
                    // Active VPN Warnings at the top
                    if (!isSandboxBypassActive && isVpnDetected) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorRed),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VPN is active! Please disable VPN to protect your account from permanent ban.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            AppScreen.Splash -> SplashScreen {
                                currentScreen = if (currentUser != null) AppScreen.Dashboard else AppScreen.Onboarding
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
                            AppScreen.OTPVerify -> OTPVerifyScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.Register }
                            )
                            AppScreen.Dashboard -> DashboardPortal(
                                viewModel = viewModel,
                                onLogout = {
                                    viewModel.logout()
                                    currentScreen = AppScreen.AuthSelection
                                }
                            )
                        }
                    }
                }
            }
            
            // --- SECURITY TESTING CONTROLLER PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = HalalGold),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.clickable { showSecurityPanel = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Security Panel", tint = IslamicGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🛡️ Security Shield",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            if (showSecurityPanel) {
                AlertDialog(
                    onDismissRequest = { showSecurityPanel = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = IslamicGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Anti-Fraud Control Hub", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Demonstrate TaskMallah's 7-Layer security checks on emulators and rooted environments:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            HorizontalDivider(color = Color.Gray.copy(0.2f))

                            // Current status indicators
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Emulator Status:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (isEmulatorDetected) "DETECTED (Simulated)" else "Not Detected",
                                        color = if (isEmulatorDetected) ErrorRed else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Root Environment:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (isRootDetected) "ROOTED" else "Clean / Secured",
                                        color = if (isRootDetected) ErrorRed else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Active VPN Tunnel:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        if (isVpnDetected) "ACTIVE" else "Inactive",
                                        color = if (isVpnDetected) ErrorRed else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.Gray.copy(0.2f))

                            // Simulation toggles
                            Text("SIMULATION MATRIX:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IslamicGreen)

                            // Sandbox toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Developer Sandbox Bypass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Allows preview to run on browser emulators", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isSandboxBypassActive,
                                    onCheckedChange = { isSandboxBypassActive = it }
                                )
                            }

                            // Simulate Root toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Simulate Root Device", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Triggers block screen if bypass is OFF", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isRootDetected,
                                    onCheckedChange = { isRootDetected = it }
                                )
                            }

                            // Simulate VPN toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Simulate Active VPN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Triggers warning banner if bypass is OFF", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = isVpnDetected,
                                    onCheckedChange = { isVpnDetected = it }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSecurityPanel = false },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                        ) {
                            Text("Done")
                        }
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
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        if (activity != null) {
            AdMobManager.showAppOpenAd(activity) {
                onTimeout()
            }
        } else {
            onTimeout()
        }
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
            // Elegant official App Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color(0xFF121212), CircleShape)
                    .border(3.dp, HalalGold, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_app_logo),
                    contentDescription = "TaskMallah Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                Text(" YA ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
            }

            OutlinedButton(
                onClick = {
                    viewModel.loginWithGoogle("MOCK_GOOGLE_ID_TOKEN_" + System.currentTimeMillis())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Google Icon",
                    tint = IslamicGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
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
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val authError by viewModel.authError.collectAsState()

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Password Recovery", color = IslamicGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Bara-e-meherbani apna registered email address darj karein taake aap ko recovery link bheja ja sake.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = IslamicGreen) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            viewModel.forgotPassword(resetEmail)
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                ) {
                    Text("Email Bhein", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

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
        
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = { showResetDialog = true }) {
                Text("Forgot Password?", color = IslamicGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.loginUser(identifier, password)
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
            Text(" YA ", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
        }

        OutlinedButton(
            onClick = {
                viewModel.loginWithGoogle("MOCK_GOOGLE_ID_TOKEN_" + System.currentTimeMillis())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Google Icon",
                tint = IslamicGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue with Google", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
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
fun OTPVerifyScreen(viewModel: TaskMallahViewModel, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()

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
        Text("Hum ne aap ke mobile number par 6-digits ka OTP SMS bheja hai. (Tasdeeq ke liye OTP Code enter karein, ya default code '786786' use karein)", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(0.7f))
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
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Verification Code") },
            modifier = Modifier.width(200.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.verifyOtpAndRegister(code)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
        ) {
            Text("Tasdeeq Karein (Verify)", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Wapis Jayein (Back)", color = IslamicGreen, fontWeight = FontWeight.Medium)
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
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showReferralDialog by remember { mutableStateOf(false) }
    var showStatementDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
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
                        text = "Lvl: ${currentUser?.accountLevel ?: "Bronze"} | ${currentUser?.subscriptionTier ?: "Free"}",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Subscription Upgrade Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSubscriptionDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = HalalGoldDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Upgrade", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = IslamicGreen)
                    }
                }

                // Referral History Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showReferralDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = IslamicGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Referral Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = IslamicGreen)
                    }
                }

                // Statement Utility Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStatementDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = IslamicGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = IslamicGreen)
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
        }
        AdMobBannerAd()
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
                            if (activity != null) {
                                AdMobManager.showInterstitial(activity) {
                                    viewModel.submitWithdrawal(amt, selectedMethod, accountTitle, accountNo)
                                }
                            } else {
                                viewModel.submitWithdrawal(amt, selectedMethod, accountTitle, accountNo)
                            }
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

    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            title = { Text("Premium Packages", color = IslamicGreen, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    val packages = listOf(
                        Triple("Free", "0.0 PKR", "Ad Reward: 0.20 PKR\nDaily Limit: 5 Ads"),
                        Triple("Basic", "1,000.0 PKR", "Ad Reward: 3.0 PKR\nDaily Limit: 15 Ads"),
                        Triple("Gold", "2,500.0 PKR", "Ad Reward: 7.0 PKR\nDaily Limit: 30 Ads"),
                        Triple("Diamond", "5,000.0 PKR", "Ad Reward: 15.0 PKR\nDaily Limit: Unlimited")
                    )
                    items(packages) { pkg ->
                        val isCurrent = currentUser?.subscriptionTier == pkg.first
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) IslamicGreen.copy(0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isCurrent) IslamicGreen else Color.Gray.copy(0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pkg.first + " Package", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = IslamicGreen)
                                    Text("Fee: ${pkg.second}", fontSize = 12.sp, color = HalalGoldDark, fontWeight = FontWeight.SemiBold)
                                    Text(pkg.third, fontSize = 11.sp, color = Color.Gray)
                                }
                                if (isCurrent) {
                                    Text("Active", color = IslamicGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.buySubscriptionTier(pkg.first)
                                            showSubscriptionDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Activate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubscriptionDialog = false }) { Text("Close") }
            }
        )
    }

    if (showReferralDialog) {
        val allUsers by viewModel.adminAllUsers.collectAsState()
        val referredUsers = remember(allUsers, currentUser) {
            allUsers.filter { it.referredBy == currentUser?.id }
        }

        AlertDialog(
            onDismissRequest = { showReferralDialog = false },
            title = { Text("Referral Portal & Status", color = IslamicGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IslamicGreen.copy(0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Apka Referral Code", fontSize = 12.sp, color = Color.Gray)
                            Text(currentUser?.referralCode ?: "N/A", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("10% Lifetime automated passive commission on all ad earnings of your referrals!", fontSize = 11.sp, textAlign = TextAlign.Center, color = HalalGoldDark)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Commission:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${"%.2f".format(currentUser?.totalReferralPkr ?: 0.0)} PKR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }

                    Text("Apki Referral Network (${referredUsers.size}):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IslamicGreen)

                    if (referredUsers.isEmpty()) {
                        Text("Abhi tak aap ka koi referral nahi hai. Apna code share karein!", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                        ) {
                            items(referredUsers) { ref ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, Color.Gray.copy(0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(ref.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Tier: ${ref.subscriptionTier} | Level: ${ref.accountLevel}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text(
                                            text = if (ref.isBanned) "Banned" else "Active",
                                            color = if (ref.isBanned) ErrorRed else SuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReferralDialog = false }) { Text("Close") }
            }
        )
    }

    if (showStatementDialog) {
        val allTxns by viewModel.userTransactions.collectAsState()
        val oneYearAgo = System.currentTimeMillis() - (365L * 24L * 60L * 60L * 1000L)
        val filteredTxns = remember(allTxns) {
            allTxns.filter { it.createdAt >= oneYearAgo }
        }

        val totalCashouts = filteredTxns.filter { it.type == "WITHDRAWAL" && it.status == "COMPLETED" }.sumOf { kotlin.math.abs(it.amountPkr) }
        val totalAdRewards = filteredTxns.filter { it.type == "TASK_REWARD" }.sumOf { it.amountPkr }
        val totalCommissionEarned = filteredTxns.filter { it.type == "REFERRAL" }.sumOf { it.amountPkr }

        AlertDialog(
            onDismissRequest = { showStatementDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = IslamicGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("1-Year Account Statement", color = IslamicGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
                ) {
                    Text("Mallah Creative - TaskMallah Official", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Divider(color = Color.Gray.copy(0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Package:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text(currentUser?.subscriptionTier ?: "Free", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account Level:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text(currentUser?.accountLevel ?: "Bronze", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HalalGoldDark)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Ad Rewards (1Y):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text("${"%.2f".format(totalAdRewards)} PKR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Commission (1Y):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text("${"%.2f".format(totalCommissionEarned)} PKR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Cashouts (1Y):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                        Text("${"%.2f".format(totalCashouts)} PKR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }

                    Divider(color = Color.Gray.copy(0.2f))
                    Text("Recent Statement Logs (Last 1 Year):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreen)

                    if (filteredTxns.isEmpty()) {
                        Text("Statement logs khali hain.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                        ) {
                            items(filteredTxns) { txn ->
                                val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(txn.createdAt))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(txn.description, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(dateStr, fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Text(
                                        text = "${if (txn.amountPkr > 0) "+" else ""}${txn.amountPkr} PKR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (txn.amountPkr > 0) SuccessGreen else ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Statement generated successfully! Exporting PDF...", Toast.LENGTH_LONG).show()
                        showStatementDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                ) {
                    Text("Export Statement (PDF)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatementDialog = false }) { Text("Close") }
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
                    itemsIndexed(filteredTasks) { index, task ->
                        if (index > 0 && index % 3 == 0) {
                            AdMobNativeAdRow(modifier = Modifier.padding(vertical = 8.dp))
                        }
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
            Spacer(modifier = Modifier.height(8.dp))
            AdMobBannerAd()
        }
    }
}

// ----------------- DETAIL SHEET: TASK DETAILS & SUBMISSION -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(viewModel: TaskMallahViewModel, onBack: () -> Unit) {
    val task by viewModel.selectedTask.collectAsState()
    val completion by viewModel.selectedTaskCompletion.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var screenshotMockPath by remember { mutableStateOf("") }
    var customTextProofNotes by remember { mutableStateOf("") }

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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Custom Text Notes / Proof ID Field
                            OutlinedTextField(
                                value = customTextProofNotes,
                                onValueChange = { customTextProofNotes = it },
                                label = { Text("Saboot ke mutaliq tafseelat (e.g. proof notes/ID)") },
                                placeholder = { Text("E.g. Verified with username: ABC") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))

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
                }

                if (completion == null && screenshotMockPath.isNotEmpty()) {
                    item {
                        Button(
                            onClick = {
                                if (activity != null) {
                                    AdMobManager.showRewardedInterstitial(activity) {
                                        viewModel.submitTaskCompletionProof(
                                            t.id,
                                            screenshotMockPath,
                                            customTextProofNotes,
                                            t.campaignName,
                                            System.currentTimeMillis()
                                        )
                                    }
                                } else {
                                    viewModel.submitTaskCompletionProof(
                                        t.id,
                                        screenshotMockPath,
                                        customTextProofNotes,
                                        t.campaignName,
                                        System.currentTimeMillis()
                                    )
                                }
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
    val context = LocalContext.current
    val activity = context as? android.app.Activity

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
            onClick = {
                if (activity != null) {
                    AdMobManager.showRewardedAd(activity) {
                        viewModel.watchAdMobAd("REWARDED")
                    }
                } else {
                    viewModel.watchAdMobAd("REWARDED")
                }
            },
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
            onClick = {
                if (activity != null) {
                    AdMobManager.showInterstitial(activity) {
                        viewModel.watchAdMobAd("INTERSTITIAL")
                    }
                } else {
                    viewModel.watchAdMobAd("INTERSTITIAL")
                }
            },
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
    val context = LocalContext.current

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
        var receiptPath by remember { mutableStateOf("") }

        val accountsInfo = mapOf(
            "EasyPaisa" to "EasyPaisa A/C: 03000856623\nTitle: Mallah Creative",
            "JazzCash" to "JazzCash A/C: 03000856623\nTitle: Mallah Creative",
            "HBL" to "HBL A/C: 50123496677887\nTitle: Mallah Creative\nIBAN: PK78HABB0050123496677887",
            "UBL" to "UBL A/C: 90123000856623\nTitle: Raju Bhai\nIBAN: PK99UNIL0090123000856623"
        )

        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = { Text("Deposit Funds Gateway", color = IslamicGreen, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Designated payment accounts par deposit transfer karein:", fontSize = 12.sp, color = Color.Gray)
                    
                    // Channel Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("EasyPaisa", "JazzCash", "HBL", "UBL").forEach { channel ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.5.dp, if (depMethod == channel) IslamicGreen else Color.Gray.copy(0.3f), RoundedCornerShape(8.dp))
                                    .background(if (depMethod == channel) IslamicGreen.copy(0.08f) else Color.Transparent)
                                    .clickable { depMethod = channel }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(channel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (depMethod == channel) IslamicGreen else Color.Gray)
                            }
                        }
                    }

                    // Display details of chosen channel
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Account Details (${depMethod}):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IslamicGreen)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(accountsInfo[depMethod] ?: "", fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }

                    OutlinedTextField(
                        value = depAmount,
                        onValueChange = { depAmount = it },
                        label = { Text("Transfer Amount (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // simulated upload screenshot proof
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { receiptPath = "receipt_tx_${System.currentTimeMillis()}.png" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Receipt", fontSize = 12.sp)
                        }
                        if (receiptPath.isNotEmpty()) {
                            Text("Selected: ...${receiptPath.takeLast(15)}", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("No file chosen", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = depAmount.toDoubleOrNull()
                        if (amt == null || amt < 100) {
                            Toast.makeText(context, "Kam se kam 100 PKR darj karein.", Toast.LENGTH_SHORT).show()
                        } else if (receiptPath.isEmpty()) {
                            Toast.makeText(context, "Raseed (Transfer Receipt) upload karein.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.submitDepositPayment(amt, depMethod, receiptPath)
                            Toast.makeText(context, "Deposit request submitted! Admin approval ke baad balance update ho jayega.", Toast.LENGTH_LONG).show()
                            showDepositDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                ) {
                    Text("Submit Deposit Request")
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
    val adminProfitPool by viewModel.adminProfitPool.collectAsState()

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
                    Text("${String.format("%.2f", adminProfitPool)} PKR", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
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
    var showAvatarDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MuslimAvatarView(
                    avatarName = currentUser?.profilePicUri,
                    modifier = Modifier
                        .size(90.dp)
                        .clickable { showAvatarDialog = true }
                )
                Text(
                    text = "Avatar Badlein (Click)",
                    fontSize = 11.sp,
                    color = IslamicGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp).clickable { showAvatarDialog = true }
                )
            }
        }

        item {
            Text(currentUser?.name ?: "User Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = IslamicGreen)
            Text(currentUser?.email ?: "", fontSize = 13.sp, color = Color.Gray)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, HalalGold.copy(0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFF121212), CircleShape)
                            .border(1.5.dp, HalalGold, CircleShape)
                            .clip(CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_logo),
                            contentDescription = "Official Brand Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text(
                            text = "Mallah Creative Platform",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HalalGoldDark
                        )
                        Text(
                            text = "Official Halal Earning ecosystem. Secured & verified.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f)
                        )
                    }
                }
            }
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

        // --- SAVED PAYMENT ACCOUNTS LINKING & LOCKS ---
        item {
            val savedAccounts by viewModel.savedAccounts.collectAsState()
            var showAddAccountDialog by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Saved Payment Accounts (${savedAccounts.size}/5):", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = IslamicGreen)
                    if (savedAccounts.size < 5) {
                        TextButton(onClick = { showAddAccountDialog = true }) {
                            Text("+ Link New", color = IslamicGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (currentUser?.isPaymentDetailsLocked == true) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aap ke payment accounts locked hain! Rabta: Super Admin.", fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (savedAccounts.isEmpty()) {
                    Text("Koi payment account link nahi hai.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    savedAccounts.forEach { acc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(acc.bankName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = IslamicGreen)
                                    Text("Title: ${acc.accountTitle}", fontSize = 12.sp)
                                    Text("A/C No: ${acc.accountNumber}", fontSize = 12.sp)
                                    Text("IBAN: ${acc.iban}", fontSize = 11.sp, color = Color.Gray)
                                }
                                
                                val isSuperAdmin = currentUser?.email?.lowercase() == "rajubhai3508194@gmail.com" || currentUser?.phone == "03496677887"
                                val canDelete = ! (currentUser?.isPaymentDetailsLocked ?: false) || isSuperAdmin
                                
                                if (canDelete) {
                                    IconButton(onClick = { viewModel.removeSavedPaymentAccount(acc.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                    }
                                } else {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                if (showAddAccountDialog) {
                    var bankName by remember { mutableStateOf("") }
                    var accTitle by remember { mutableStateOf("") }
                    var accNum by remember { mutableStateOf("") }
                    var ibanInput by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddAccountDialog = false },
                        title = { Text("Link Payment Account", color = IslamicGreen) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = { Text("Bank Name (e.g. HBL, EasyPaisa)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = accTitle,
                                    onValueChange = { accTitle = it },
                                    label = { Text("Account Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = accNum,
                                    onValueChange = { accNum = it },
                                    label = { Text("Account Number") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = ibanInput,
                                    onValueChange = { ibanInput = it.uppercase() },
                                    label = { Text("IBAN (e.g. PK99UBL...)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val isValidIban = ibanInput.startsWith("PK") && ibanInput.length == 24 && ibanInput.substring(2).all { it.isLetterOrDigit() }
                                    if (!isValidIban) {
                                        Toast.makeText(context, "Ghalat IBAN! IBAN 'PK' se shuru hona chahiye aur bilkul 24 characters ka hona chahiye.", Toast.LENGTH_LONG).show()
                                    } else if (bankName.isBlank() || accTitle.isBlank() || accNum.isBlank()) {
                                        Toast.makeText(context, "Saare fields darj karein.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.linkNewPaymentAccount(bankName, accTitle, accNum, ibanInput)
                                        showAddAccountDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                            ) {
                                Text("Link Account")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddAccountDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }

        // --- SUPER ADMIN OVERRIDE PAYMENT LOCKS CONTROL ---
        if (currentUser?.email?.lowercase() == "rajubhai3508194@gmail.com" || currentUser?.phone == "03496677887") {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = HalalGold.copy(0.1f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Super Admin - Payment Lock Override", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HalalGoldDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Lock status: ${if (currentUser?.isPaymentDetailsLocked == true) "LOCKED" else "UNLOCKED"}", fontSize = 12.sp)
                            Button(
                                onClick = { viewModel.changePaymentAccountLock(currentUser!!.id, !(currentUser?.isPaymentDetailsLocked ?: false)) },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
                            ) {
                                Text(if (currentUser?.isPaymentDetailsLocked == true) "Unlock Details" else "Lock Details", fontSize = 11.sp)
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

    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("Select Minimalist Avatar", color = IslamicGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val avatars = listOf("Green Crescent", "Golden Dome", "Islamic Geometry", "Kaaba Minimalist", "Raju Bhai")
                    avatars.forEach { av ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateProfilePicture(av)
                                    showAvatarDialog = false
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MuslimAvatarView(avatarName = av, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(av, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarDialog = false }) { Text("Cancel") }
            }
        )
    }
}
