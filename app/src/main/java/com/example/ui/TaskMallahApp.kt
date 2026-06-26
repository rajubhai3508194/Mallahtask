package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.example.R
import com.example.BuildConfig
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

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: ImageVector
)

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

    // Periodically update VPN status safely using CoroutineContext lifecycle bounds
    LaunchedEffect(context) {
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
                    if (BuildConfig.DEBUG) {
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
            
            // --- SECURITY TESTING CONTROLLER PANEL (Protected by BuildConfig.DEBUG) ---
            if (BuildConfig.DEBUG) {
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
            }

            if (showSecurityPanel && BuildConfig.DEBUG) {
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
    val activity = remember(context) { context.findActivity() }

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
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)
            ) {
                Text(if (currentPage == slides.lastIndex) "Get Started" else "Next", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- TEMPORARY SCREEN PLACEHOLDERS TO FIX BUILD ERRORS ---

@Composable
fun AuthSelectionScreen(onLoginClick: () -> Unit, onRegisterClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Auth Selection Screen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLoginClick, colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)) { Text("Go to Login") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRegisterClick, colors = ButtonDefaults.buttonColors(containerColor = HalalGold)) { Text("Go to Register", color = Color.Black) }
        }
    }
}

@Composable
fun RegisterScreen(viewModel: TaskMallahViewModel, onBack: () -> Unit, onSuccess: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Register Screen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSuccess, colors = ButtonDefaults.buttonColors(containerColor = IslamicGreen)) { Text("Simulate OTP Required") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Back") }
        }
    }
}

@Composable
fun LoginScreen(viewModel: TaskMallahViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Login Screen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Back") }
        }
    }
}

@Composable
fun OTPVerifyScreen(viewModel: TaskMallahViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("OTP Verification Screen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Back") }
        }
    }
}

@Composable
fun DashboardPortal(viewModel: TaskMallahViewModel, onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Welcome to TaskMallah Dashboard!", fontSize = 22.sp, color = IslamicGreen, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { 
                Text("Logout") 
            }
        }
    }
}

// Extension to find activity context safely
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
