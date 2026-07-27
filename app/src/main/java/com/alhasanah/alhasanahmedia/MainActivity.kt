package com.alhasanah.alhasanahmedia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alhasanah.alhasanahmedia.navigation.AppNavHost
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.data.repository.NotificationRepository
import com.alhasanah.alhasanahmedia.data.repository.AnnouncementRepository
import com.alhasanah.alhasanahmedia.fcm.MyFirebaseMessagingService
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniPremiumTheme
import com.alhasanah.alhasanahmedia.ui.admin.ADMIN_PANEL_URL
import com.alhasanah.alhasanahmedia.ui.admin.AdminWebViewPreloader
import com.alhasanah.alhasanahmedia.ui.auth.AuthViewModel
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.ComingSoonDialog
import com.alhasanah.alhasanahmedia.ui.components.AnnouncementDialog
import com.alhasanah.alhasanahmedia.ui.components.UpdateDialog
import com.alhasanah.alhasanahmedia.ui.tutorial.TutorialPhase
import com.alhasanah.alhasanahmedia.ui.tutorial.UserTypeSelectionDialog
import com.alhasanah.alhasanahmedia.ui.tutorial.tutorialMsg
import com.alhasanah.alhasanahmedia.ui.tutorial.LocalShowcaseScope
import com.alhasanah.alhasanahmedia.ui.theme.AlhasanahMediaTheme
import com.alhasanah.alhasanahmedia.showcase.ui.ShowcaseLayout
import com.alhasanah.alhasanahmedia.showcase.model.ShowcaseMsg
import com.alhasanah.alhasanahmedia.util.UpdateCheckWorker
import com.alhasanah.alhasanahmedia.util.UpdateChecker
import com.alhasanah.alhasanahmedia.util.AnnouncementPreferences
import com.alhasanah.alhasanahmedia.util.UpdateResult
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

// ─────────────────────────────────────────────────────────────────────────────
// Activity — TIDAK ADA PERUBAHAN LOGIKA
// ─────────────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val _intentFlow = MutableStateFlow<Intent?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Log.d("MainActivity", "All permissions granted")
        } else {
            Log.d("MainActivity", "Some permissions denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        askRequiredPermissions()
        
        _intentFlow.value = intent

        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val themeMode by mainViewModel.themeMode.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = themeMode ?: isSystemDark
            
            val currentIntent by _intentFlow.collectAsState()

            AlhasanahMediaTheme(darkTheme = useDarkTheme) {
                AlhasanahApp(mainViewModel, currentIntent, useDarkTheme)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _intentFlow.value = intent
    }

    private fun askRequiredPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// App root — TIDAK ADA PERUBAHAN LOGIKA
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlhasanahApp(mainViewModel: MainViewModel, intent: Intent?, isDark: Boolean) {
    val authViewModel: AuthViewModel = koinViewModel()
    val notificationRepository: NotificationRepository = koinInject()
    val context = LocalContext.current
    val authState by authViewModel.authenticationState.collectAsState()
    val user by authViewModel.getCurrentUser().collectAsState(initial = null)
    val activeSantriNis by authViewModel.activeSantriNis.collectAsState()
    val currentUserRole by authViewModel.currentUserRole.collectAsState()
    val isLoggedIn = authState is AuthenticationState.Authenticated

    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showLogoutDialog by remember { mutableStateOf(false) }

    // ── Tutorial State ─────────────────────────────────────────────────────
    val userType by mainViewModel.userType.collectAsState()
    val hasCompletedTutorial by mainViewModel.hasCompletedTutorial.collectAsState()
    val hasCompletedTutorialPhase2 by mainViewModel.hasCompletedTutorialPhase2.collectAsState()
    var showUserTypeDialog by remember { mutableStateOf(false) }
    var tutorialPhase by remember { mutableStateOf(TutorialPhase.NONE) }
    var announcementDismissed by remember { mutableStateOf(false) }
    var isShowcasing by remember { mutableStateOf(false) }

    // ── Step 1: Cek announcement + user type, tampilkan secara sequential ──
    val announcementRepository: AnnouncementRepository = koinInject()
    val announcementPreferences = remember { AnnouncementPreferences(context) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var announcements by remember { mutableStateOf<List<com.alhasanah.alhasanahmedia.data.model.Announcement>>(emptyList()) }

    // Unified flow: splash → announcement (if any) → user type dialog
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3500) // Wait for splash

        // Cek announcement dulu
        try {
            val result = announcementRepository.getActiveAnnouncements()
            if (result.isNotEmpty() && !announcementPreferences.hasShownToday()) {
                announcements = result
                showAnnouncementDialog = true
            } else {
                // Tidak ada announcement → langsung ke user type dialog
                if (userType == null) {
                    showUserTypeDialog = true
                }
            }
        } catch (e: Exception) {
            Log.e("Announcement", "Failed to fetch announcements", e)
            // Error → langsung ke user type dialog
            if (userType == null) {
                showUserTypeDialog = true
            }
        }
    }

    // Step 2: Setelah announcement di-dismiss, tampilkan user type dialog
    LaunchedEffect(announcementDismissed) {
        if (announcementDismissed && userType == null) {
            showUserTypeDialog = true
            announcementDismissed = false
        }
    }

    // Check if Phase 1 tutorial should start (wali santri & not completed & not logged in)
    LaunchedEffect(userType, hasCompletedTutorial, isLoggedIn) {
        if (userType == "wali_santri" && !hasCompletedTutorial && !isLoggedIn) {
            kotlinx.coroutines.delay(500)
            tutorialPhase = TutorialPhase.PHASE_1_STEP_1
            isShowcasing = true
        }
    }

    // Check if Phase 2 tutorial should start (wali santri, completed phase 1, just logged in)
    LaunchedEffect(isLoggedIn, userType, hasCompletedTutorial, hasCompletedTutorialPhase2) {
        if (isLoggedIn && userType == "wali_santri" && hasCompletedTutorial && !hasCompletedTutorialPhase2) {
            kotlinx.coroutines.delay(1500)
            tutorialPhase = TutorialPhase.PHASE_2_STEP_1
            isShowcasing = true
            scope.launch { drawerState.open() }
        }
    }

    LaunchedEffect(isLoggedIn, user?.id) {
        if (!isLoggedIn || user == null) return@LaunchedEffect
        // Register any pending FCM token from onNewToken (if session was unavailable)
        MyFirebaseMessagingService.registerPendingToken(context, notificationRepository)
    }

    // Update checker — cek sekali saat login + schedule periodic check
    var updateDialogShown by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.alhasanah.alhasanahmedia.util.UpdateInfo?>(null) }
    LaunchedEffect(isLoggedIn, user?.id) {
        if (!isLoggedIn || user == null) return@LaunchedEffect

        // Schedule periodic background check (every 6 hours)
        UpdateCheckWorker.schedule(context)

        // Immediate check on login
        if (!updateDialogShown) {
            val result = UpdateChecker.checkUpdateAsync()
            if (result is com.alhasanah.alhasanahmedia.util.UpdateResult.Available) {
                updateInfo = result.info
                updateDialogShown = true
            }
        }
    }

    // Auto-check on every app foreground (resume)
    LaunchedEffect(Unit) {
        if (isLoggedIn && !updateDialogShown) {
            val result = UpdateChecker.checkUpdateAsync()
            if (result is com.alhasanah.alhasanahmedia.util.UpdateResult.Available) {
                updateInfo = result.info
                updateDialogShown = true
            }
        }
    }

    if (showAnnouncementDialog && announcements.isNotEmpty()) {
        AnnouncementDialog(
            announcements = announcements,
            onDismiss = {
                showAnnouncementDialog = false
                announcementDismissed = true
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    announcementPreferences.markShownToday()
                }
            }
        )
    }

    // ── User Type Selection Dialog ──────────────────────────────────────────
    if (showUserTypeDialog) {
        UserTypeSelectionDialog(
            onWaliSantriSelected = {
                mainViewModel.setUserType("wali_santri")
                showUserTypeDialog = false
            },
            onGuestSelected = {
                mainViewModel.setUserType("guest")
                showUserTypeDialog = false
            }
        )
    }

    LaunchedEffect(intent, isLoggedIn, activeSantriNis) {
        if (intent != null) {
            // Handle notification tap to open update dialog
            if (intent.getBooleanExtra("open_update_dialog", false)) {
                val version = intent.getStringExtra("update_version") ?: ""
                val changelog = intent.getStringExtra("update_changelog") ?: ""
                val readyToInstall = intent.getBooleanExtra("update_ready_to_install", false)

                if (readyToInstall) {
                    // APK already downloaded — show dialog for install
                    val downloadedApk = UpdateChecker.findDownloadedApk(context)
                    if (downloadedApk != null) {
                        updateInfo = com.alhasanah.alhasanahmedia.util.UpdateInfo(
                            versionName = version,
                            versionCode = 0,
                            changelog = "APK sudah diunduh. Tap untuk install.",
                            downloadUrl = "",
                            fileSize = downloadedApk.length(),
                            releaseDate = ""
                        )
                        updateDialogShown = true
                    }
                } else {
                    // Show available update dialog
                    updateInfo = com.alhasanah.alhasanahmedia.util.UpdateInfo(
                        versionName = version,
                        versionCode = 0,
                        changelog = changelog,
                        downloadUrl = "",
                        fileSize = 0,
                        releaseDate = ""
                    )
                    updateDialogShown = true
                }
                intent.removeExtra("open_update_dialog")
                intent.removeExtra("update_ready_to_install")
                return@LaunchedEffect
            }

            parseAdminPanelDeepLink(intent)?.let { adminUrl ->
                navController.navigate(Screen.AdminPanel.createRoute(adminUrl)) {
                    launchSingleTop = true
                }
                intent.data = null
                return@LaunchedEffect
            }

            val adminNotificationUrl = parseAdminPanelNotification(intent)
            if (adminNotificationUrl != null) {
                navController.navigate(Screen.AdminPanel.createRoute(adminNotificationUrl)) {
                    launchSingleTop = true
                }
                intent.removeExtra("notif_type")
                return@LaunchedEffect
            }
        }

        if (isLoggedIn && intent != null) {
            parseAlumniProfileDeepLink(intent)?.let { alumniId ->
                navController.navigate(Screen.AlumniProfileDetail.createRoute(alumniId)) {
                    launchSingleTop = true
                }
                intent.data = null
                return@LaunchedEffect
            }
            parseWalletDisputeDeepLink(intent)?.let { ledgerId ->
                navController.navigate(Screen.WalletDispute.createRoute(ledgerId)) {
                    launchSingleTop = true
                }
                intent.data = null
                return@LaunchedEffect
            }

            val type = intent.getStringExtra("notif_type")
            val nis = intent.getStringExtra("notif_nis") ?: activeSantriNis
            val threadId = intent.getStringExtra("notif_thread_id")
            val conversationId = intent.getStringExtra("notif_conversation_id")
            val walletLedgerId = intent.getStringExtra("notif_wallet_ledger_id")

            if (type != null) {
                when (type) {
                    "tagihan",
                    "tagihan_due_reminder",
                    "tagihan.payment_installment",
                    "tagihan.payment_success",
                    "tagihan.due_reminder",
                    "tagihan.overdue_reminder" -> nis?.let { navController.navigate(Screen.Keuangan.createRoute(it)) }
                    "pelanggaran" -> nis?.let { navController.navigate(Screen.Pelanggaran.createRoute(it)) }
                    "hafalan"     -> nis?.let { navController.navigate(Screen.Hafalan.createRoute(it)) }
                    "murajaah", "murojaah" -> nis?.let { navController.navigate(Screen.Murajaah.createRoute(it)) }
                    "kesehatan"   -> nis?.let { navController.navigate(Screen.Kesehatan.createRoute(it)) }
                    "perizinan"   -> nis?.let { navController.navigate(Screen.Perizinan.createRoute(it)) }
                    "prestasi",
                    "prestasi_created" -> navController.navigate(Screen.Prestasi.route)
                    "forum_comment",
                    "forum_reaction",
                    "forum_report" -> navController.navigate(Screen.AlumniForum.createRoute(threadId))
                    "alumni_chat_message" -> navController.navigate(Screen.AlumniChat.createRoute(conversationId))
                    "wallet_dispute" -> walletLedgerId?.let { navController.navigate(Screen.WalletDispute.createRoute(it)) }
                    "wallet_transaction",
                    "wallet_low_balance",
                    "wallet_critical_balance",
                    "wallet_large_transaction",
                    "wallet_kantin_payment",
                    "wallet_topup_failed" -> nis?.let { navController.navigate(Screen.WalletWali.createRoute(it)) }
                }
                intent.removeExtra("notif_type")
            }
        }
    }

    if (drawerState.isOpen) {
        BackHandler { scope.launch { drawerState.close() } }
    }

    val tutorialGreeting = when (tutorialPhase) {
        TutorialPhase.PHASE_1_STEP_1 -> ShowcaseMsg(
            text = "Selamat datang! Mari kita kenali fitur aplikasi ini.",
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        TutorialPhase.PHASE_2_STEP_1 -> ShowcaseMsg(
            text = "Berikut adalah fitur-fitur yang tersedia untuk Anda.",
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        else -> null
    }
    val isDarkTutorial = isAppInDarkTheme()
    ShowcaseLayout(
        isShowcasing = isShowcasing,
        isDarkLayout = isDarkTutorial,
        initIndex = if (tutorialGreeting == null) 1 else 0,
        onFinish = {
            isShowcasing = false
            when (tutorialPhase) {
                TutorialPhase.PHASE_1_STEP_1 -> {
                    tutorialPhase = TutorialPhase.PHASE_1_STEP_2
                    scope.launch {
                        drawerState.open()
                        kotlinx.coroutines.delay(350)
                        isShowcasing = true
                    }
                }
                TutorialPhase.PHASE_1_STEP_2 -> {
                    tutorialPhase = TutorialPhase.PHASE_1_STEP_3
                    scope.launch {
                        drawerState.close()
                        navController.navigate(Screen.Login.route) {
                            launchSingleTop = true
                        }
                        kotlinx.coroutines.delay(650)
                        isShowcasing = true
                    }
                }
                TutorialPhase.PHASE_1_STEP_3 -> {
                    mainViewModel.completeTutorial()
                    tutorialPhase = TutorialPhase.NONE
                }
                TutorialPhase.PHASE_2_STEP_1 -> {
                    tutorialPhase = TutorialPhase.PHASE_2_STEP_2
                    scope.launch {
                        drawerState.open()
                        kotlinx.coroutines.delay(350)
                        isShowcasing = true
                    }
                }
                TutorialPhase.PHASE_2_STEP_2 -> {
                    tutorialPhase = TutorialPhase.PHASE_2_STEP_3
                    scope.launch {
                        kotlinx.coroutines.delay(250)
                        isShowcasing = true
                    }
                }
                TutorialPhase.PHASE_2_STEP_3 -> {
                    mainViewModel.completeTutorialPhase2()
                    tutorialPhase = TutorialPhase.NONE
                    scope.launch { drawerState.close() }
                }
                TutorialPhase.NONE -> Unit
            }
        },
        greeting = tutorialGreeting
    ) {
    CompositionLocalProvider(LocalShowcaseScope provides this) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                isLoggedIn      = isLoggedIn,
                user            = user,
                activeSantriNis = activeSantriNis,
                currentUserRole = currentUserRole,
                navController   = navController,
                isDark          = isDark,
                tutorialPhase   = tutorialPhase,
                onTutorialReplay = {
                    scope.launch { drawerState.close() }
                    mainViewModel.resetTutorial()
                    tutorialPhase = TutorialPhase.PHASE_1_STEP_1
                    isShowcasing = true
                },
                closeDrawer     = { scope.launch { drawerState.close() } },
                onLogout        = {
                    scope.launch { drawerState.close() }
                    showLogoutDialog = true
                },
                onToggleTheme   = { mainViewModel.toggleTheme(isDark) }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AppGradientBackground(isDark = isDark)
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                bottomBar = {
                    when {
                        currentRoute in alumniPrimaryBottomRoutes -> {
                            AlumniBottomAppBar(
                                currentRoute = currentRoute,
                                navController = navController
                            )
                        }
                        currentRoute != Screen.Splash.route && currentRoute !in alumniRoutesWithoutGlobalBottom -> {
                            AlhasanahBottomAppBar(
                                currentRoute = currentRoute,
                                activeSantriNis = activeSantriNis,
                                navController = navController
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = Color.Transparent
                ) {
                    AppNavHost(
                        navController = navController,
                        isLoggedIn    = isLoggedIn,
                        openDrawer    = { scope.launch { drawerState.open() } },
                        tutorialPhase = tutorialPhase
                    )
                }
            }
        }
    }
    } // end CompositionLocalProvider
    } // end ShowcaseLayout

    // Update dialog
    if (updateDialogShown) {
        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onDismiss = { updateDialogShown = false }
            )
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                AdminWebViewPreloader.clearSession()
                authViewModel.signOut()
            }
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { entered = true }
        val scale by animateFloatAsState(
            targetValue = if (entered) 1f else 0.96f,
            animationSpec = tween(durationMillis = 180),
            label = "logoutDialogScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = tween(durationMillis = 160),
            label = "logoutDialogAlpha"
        )
        val primary = MaterialTheme.colorScheme.primary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                primary.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.24f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = "Keluar dari Akun?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sesi Anda akan ditutup dari perangkat ini. Data tersimpan aman dan bisa dibuka kembali setelah login.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Keluar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun parseWalletDisputeDeepLink(intent: Intent): String? {
    val uri = intent.data ?: return null
    return if (uri.scheme == "alhasanah" && uri.host == "wallet" && uri.path == "/dispute") {
        uri.getQueryParameter("ledger_id")?.takeIf { it.isNotBlank() }
    } else {
        null
    }
}

private fun parseAlumniProfileDeepLink(intent: Intent): String? {
    val uri = intent.data ?: return null
    val segments = uri.pathSegments
    return when {
        uri.scheme == "alhasanahmedia" &&
            uri.host == "alumni" &&
            segments.size >= 2 &&
            segments[0] == "profile" -> segments[1]

        uri.scheme in setOf("http", "https") &&
            uri.host == "alhasanah.media" &&
            segments.size >= 3 &&
            segments[0] == "alumni" &&
            segments[1] == "profile" -> segments[2]

        else -> null
    }?.takeIf { it.isNotBlank() }
}

private fun parseAdminPanelNotification(intent: Intent): String? {
    val type = intent.getStringExtra("notif_type") ?: return null
    if (type !in setOf("admin_panel", "admin", "admin_webview")) return null
    return listOf(
        intent.getStringExtra("notif_admin_url"),
        intent.getStringExtra("admin_url"),
        intent.getStringExtra("notif_url"),
        intent.getStringExtra("url")
    ).firstOrNull { !it.isNullOrBlank() }?.let(::sanitizeAdminUrl) ?: ADMIN_PANEL_URL
}

private fun parseAdminPanelDeepLink(intent: Intent): String? {
    val uri = intent.data ?: return null
    return when {
        uri.scheme == "https" && uri.host == "alhasanah-media.vercel.app" -> sanitizeAdminUrl(uri.toString())
        uri.scheme == "alhasanahmedia" && uri.host == "admin" -> {
            uri.getQueryParameter("url")?.let(::sanitizeAdminUrl)
                ?: uri.getQueryParameter("path")?.let { path ->
                    sanitizeAdminUrl(ADMIN_PANEL_URL.trimEnd('/') + "/" + path.trimStart('/'))
                }
                ?: ADMIN_PANEL_URL
        }
        else -> null
    }
}

private fun sanitizeAdminUrl(raw: String): String {
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return ADMIN_PANEL_URL
    return if (uri.scheme == "https" && uri.host == "alhasanah-media.vercel.app") {
        uri.toString()
    } else {
        ADMIN_PANEL_URL
    }
}

private val alumniPrimaryBottomRoutes = setOf(
    Screen.AlumniForum.route,
    Screen.AlumniChat.route,
    Screen.AlumniNotifications.route,
    Screen.AlumniProfile.route
)

private val alumniRoutesWithoutGlobalBottom = setOf(
    Screen.AdminPanel.route,
    Screen.AlumniForum.route,
    Screen.AlumniChat.route,
    Screen.AlumniDirectory.route,
    Screen.AlumniRegister.route,
    Screen.AlumniProfile.route,
    Screen.AlumniProfileDetail.route,
    Screen.AlumniProfileEdit.route,
    Screen.AlumniSettings.route,
    Screen.AlumniNotifications.route,
    Screen.AlumniInfo.route
)

private data class BottomAppBarItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val matchRoutes: Set<String> = setOf(route)
)

@Composable
private fun AlhasanahBottomAppBar(
    currentRoute: String?,
    activeSantriNis: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()
    val items = listOf(
        BottomAppBarItem(
            label = "Beranda",
            icon = Icons.Outlined.Home,
            route = Screen.Home.route
        ),
        BottomAppBarItem(
            label = "Al-Qur'an",
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            route = Screen.Quran.route,
            matchRoutes = setOf(Screen.Quran.route, Screen.SurahDetail.route, Screen.JuzDetail.route)
        ),
        BottomAppBarItem(
            label = "Artikel",
            icon = Icons.AutoMirrored.Outlined.Article,
            route = Screen.BeritaList.route,
            matchRoutes = setOf(Screen.BeritaList.route, Screen.BeritaDetail.route)
        ),
        BottomAppBarItem(
            label = "Infaq",
            icon = Icons.Outlined.FavoriteBorder,
            route = Screen.Donasi.createRoute(activeSantriNis ?: "public"),
            matchRoutes = setOf(Screen.Donasi.route)
        ),
        BottomAppBarItem(
            label = "Tanya AI",
            icon = Icons.Outlined.AutoAwesome,
            route = Screen.RagChat.route
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.92f),
        border = BorderStroke(1.dp, primary.copy(alpha = if (isDark) 0.34f else 0.22f)),
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute in item.matchRoutes
                BottomAppBarButton(
                    item = item,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigateBottomAppBar(item.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AlumniBottomAppBar(
    currentRoute: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    AlumniPremiumTheme {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val isDark = isSystemInDarkTheme()
    val items = listOf(
        BottomAppBarItem(
            label = "Threads",
            icon = Icons.Outlined.Forum,
            route = Screen.AlumniForum.baseRoute,
            matchRoutes = setOf(Screen.AlumniForum.route)
        ),
        BottomAppBarItem(
            label = "Chat",
            icon = Icons.Outlined.ChatBubbleOutline,
            route = Screen.AlumniChat.baseRoute,
            matchRoutes = setOf(Screen.AlumniChat.route)
        ),
        BottomAppBarItem(
            label = "Notifikasi",
            icon = Icons.Outlined.Notifications,
            route = Screen.AlumniNotifications.route
        ),
        BottomAppBarItem(
            label = "Profil",
            icon = Icons.Outlined.Person,
            route = Screen.AlumniProfile.route
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = if (isDark) Color.Black else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(
            width = if (isDark) 0.5.dp else 1.dp,
            color = if (isDark) outline.copy(alpha = 0.72f) else primary.copy(alpha = 0.18f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = if (isDark) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute in item.matchRoutes
                BottomAppBarButton(
                    item = item,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigateAlumniBottomAppBar(item.route)
                        }
                    }
                )
            }
        }
    }
    }
}

private fun NavHostController.navigateAlumniBottomAppBar(route: String) {
    if (isCurrentAlumniRoute(route)) return
    runCatching {
        navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
        }
    }.onFailure {
        Log.w("MainActivity", "Alumni bottom navigation ignored: $route", it)
    }
}

private fun NavHostController.isCurrentAlumniRoute(route: String): Boolean {
    val current = currentBackStackEntry?.destination?.route ?: return false
    return when (route) {
        Screen.AlumniForum.baseRoute -> current == Screen.AlumniForum.route || current == Screen.AlumniForum.baseRoute
        Screen.AlumniChat.baseRoute -> current == Screen.AlumniChat.route || current == Screen.AlumniChat.baseRoute
        else -> current == route
    }
}

private fun NavHostController.navigateBottomAppBar(route: String) {
    if (route == Screen.Home.route) {
        navigate(Screen.Home.route) {
            launchSingleTop = true
            popUpTo(Screen.Home.route) {
                inclusive = true
            }
        }
        return
    }

    navigate(route) {
        launchSingleTop = true
        popUpTo(Screen.Home.route) {
            inclusive = false
        }
    }
}

@Composable
private fun BottomAppBarButton(
    item: BottomAppBarItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
    var lastClickAt by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                val now = System.currentTimeMillis()
                if (now - lastClickAt >= 450L) {
                    lastClickAt = now
                    onClick()
                }
            }
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (selected) primary.copy(alpha = 0.14f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = contentColor,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                fontSize = 10.sp
            ),
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  DRAWER SHELL
// Perubahan: background lebih hangat di dark mode, corner 28dp, footer premium
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppDrawerContent(
    isLoggedIn: Boolean,
    user: UserInfo?,
    activeSantriNis: String?,
    currentUserRole: String?,
    navController: NavHostController,
    isDark: Boolean,
    tutorialPhase: TutorialPhase = TutorialPhase.NONE,
    onTutorialReplay: () -> Unit = {},
    closeDrawer: () -> Unit,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    ModalDrawerSheet(
        modifier              = Modifier.fillMaxWidth(0.85f),
        drawerContainerColor  = if (isDark) Color(0xFF0C0B10) else MaterialTheme.colorScheme.surface,
        drawerShape           = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerTonalElevation  = 0.dp
    ) {
        DrawerContentColumn(
            isLoggedIn = isLoggedIn,
            user = user,
            activeSantriNis = activeSantriNis,
            currentUserRole = currentUserRole,
            navController = navController,
            tutorialPhase = tutorialPhase,
            onTutorialReplay = onTutorialReplay,
            closeDrawer = closeDrawer,
            onLogout = onLogout,
            onToggleTheme = onToggleTheme
        )
    }
}

@Composable
private fun DrawerContentColumn(
    isLoggedIn: Boolean,
    user: UserInfo?,
    activeSantriNis: String?,
    currentUserRole: String?,
    navController: NavHostController,
    tutorialPhase: TutorialPhase,
    onTutorialReplay: () -> Unit,
    closeDrawer: () -> Unit,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight()) {

        // ── Header ─────────────────────────────────────────────────────
        DrawerHeader(user = user, isDark = isAppInDarkTheme())

        // ── Scrollable menu body ────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                DrawerBody(
                    isLoggedIn      = isLoggedIn,
                    activeSantriNis = activeSantriNis,
                    currentUserRole = currentUserRole,
                    navController   = navController,
                    tutorialPhase   = tutorialPhase,
                    onTutorialReplay = onTutorialReplay,
                    closeDrawer     = closeDrawer,
                    onLogout        = onLogout,
                    onToggleTheme   = onToggleTheme
                )
            }
        }

        // ── Footer brand — ornamen + versi ─────────────────────────────
        DrawerFooter()
    }
}

@Composable
fun DrawerFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp, top = 4.dp)
    ) {
        // Ornamen triple-dot
        val footerDotColor = MaterialTheme.colorScheme.primary
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Canvas(Modifier.size(3.dp)) {
                drawCircle(footerDotColor.copy(alpha = 0.35f))
            }
            Canvas(Modifier.size(4.5.dp)) {
                drawCircle(footerDotColor.copy(alpha = 0.55f))
            }
            Canvas(Modifier.size(3.dp)) {
                drawCircle(footerDotColor.copy(alpha = 0.35f))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "ALHASANAH MEDIA  ·  v1.0",
            style     = MaterialTheme.typography.labelSmall.copy(
                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                letterSpacing = 1.8.sp,
                fontWeight    = FontWeight.Medium,
                fontSize      = 9.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  DRAWER HEADER
// Perubahan:
//   • Pattern: diagonal garis halus (bukan dot grid yang kaku)
//   • Gold shimmer border di bagian bawah header
//   • Avatar: dua cincin (glow luar + border emas)
//   • Status: pill badge dengan dot warna semantis
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrawerHeader(user: UserInfo?, isDark: Boolean) {
    val primaryGold   = MaterialTheme.colorScheme.primary
    val secondaryGold = MaterialTheme.colorScheme.secondary

    val headerBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF261500),   // Coklat emas hangat
                Color(0xFF0D0A15),   // Deep midnight
            ),
            start = Offset(0f, 0f),
            end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(primaryGold, secondaryGold),
            start  = Offset(0f, 0f),
            end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBrush)
            .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)
    ) {

        // ── Pola diagonal halus (menggantikan dot grid yang kaku) ────────────
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .alpha(0.06f)
        ) {
            val gap = 22.dp.toPx()
            val lineCount = ((size.width + size.height) / gap).toInt() + 4
            repeat(lineCount) { i ->
                val x = i * gap - size.height
                drawLine(
                    color       = Color.White,
                    start       = Offset(x, 0f),
                    end         = Offset(x + size.height, size.height),
                    strokeWidth = 0.6.dp.toPx()
                )
            }
        }

        // ── Gold shimmer border bawah header ─────────────────────────────────
        val primaryGoldShimmer = primaryGold
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryGoldShimmer.copy(alpha = 0.4f),
                        primaryGoldShimmer.copy(alpha = 0.9f),
                        primaryGoldShimmer.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = size.height
            )
        }

        // ── Konten header ─────────────────────────────────────────────────────
        val headerContentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Avatar dengan dual-ring (glow luar + border emas)
            Box(contentAlignment = Alignment.Center) {
                // Cincin glow terluar (transparan penuh, hanya efek)
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryGold.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                // Cincin border emas tajam
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape    = CircleShape,
                    color    = Color.White.copy(alpha = 0.10f),
                    border   = BorderStroke(1.8.dp, primaryGold.copy(alpha = 0.85f))
                ) {}
                // Avatar icon
                Icon(
                    painter           = painterResource(id = R.drawable.ic_user_placeholder),
                    contentDescription = "User Avatar",
                    modifier          = Modifier
                        .size(68.dp)
                        .clip(CircleShape),
                    tint = Color.Unspecified
                )
            }

            // Nama + status badge
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text  = user?.email?.substringBefore('@')?.uppercase() ?: "WALI SANTRI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight    = FontWeight.Black,
                        color         = headerContentColor,
                        letterSpacing = 0.8.sp
                    )
                )

                // Pill badge — warna dot semantis (hijau = verified, amber = guest)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = headerContentColor.copy(alpha = 0.13f)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = if (user != null) Color(0xFF4ADE80) else Color(0xFFFFD060),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text  = if (user != null) "Berhasil Login" else "Guest Mode",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = headerContentColor.copy(alpha = 0.92f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  DRAWER BODY — LOGIKA IDENTIK, hanya divider yang diupgrade
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrawerBody(
    isLoggedIn: Boolean,
    activeSantriNis: String?,
    currentUserRole: String?,
    navController: NavHostController,
    tutorialPhase: TutorialPhase = TutorialPhase.NONE,
    onTutorialReplay: () -> Unit = {},
    closeDrawer: () -> Unit,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val isNavEnabled = activeSantriNis != null
    val isKantin = currentUserRole.equals("kantin", ignoreCase = true)
    val drawerScope = rememberCoroutineScope()
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var comingSoonTitle by remember { mutableStateOf("") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var drawerUpdateInfo by remember { mutableStateOf<com.alhasanah.alhasanahmedia.util.UpdateInfo?>(null) }
    var showUpdateResultDialog by remember { mutableStateOf(false) }
    var updateResultMessage by remember { mutableStateOf("") }
    var absensiSubmenuExpanded by remember { mutableStateOf(false) }
    val showcaseScope = LocalShowcaseScope.current

    if (isLoggedIn) {
        DrawerSectionLabel("MENU UTAMA")
        DrawerMenuItemElegant(
            icon    = Icons.Outlined.Home,
            text    = "Beranda",
            onClick = { closeDrawer(); navController.navigate(Screen.Home.route) }
        )
        DrawerMenuItemElegant(
            icon    = Icons.Outlined.AutoAwesome,
            text    = "Tanya AI",
            onClick = {
                closeDrawer()
                comingSoonTitle = "Tanya AI"
                showComingSoonDialog = true
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tutorial Target: Phase 2 Step 2 - "FITUR SANTRI" section
        val fiturSantriLabelModifier = if (tutorialPhase == TutorialPhase.PHASE_2_STEP_2 && showcaseScope != null) {
            with(showcaseScope) {
                Modifier.showcase(
                    index = 1,
                    message = tutorialMsg(
                        text = "Menu Wali Santri — Scroll ke bawah untuk melihat semua menu: Profil, Absensi, Hafalan, dan lainnya.",
                        isDark = isSystemInDarkTheme()
                    )
                )
            }
        } else {
            Modifier
        }

        Box(modifier = fiturSantriLabelModifier) {
            DrawerSectionLabel("FITUR SANTRI")
        }

        // Tutorial Target: Phase 2 Step 3 - "Profil Santri"
        val profilSantriModifier = if (tutorialPhase == TutorialPhase.PHASE_2_STEP_3 && showcaseScope != null) {
            with(showcaseScope) {
                Modifier.showcase(
                    index = 1,
                    message = tutorialMsg(
                        text = "Profil Santri — Lihat data lengkap santri Anda, termasuk info pribadi dan akademik.",
                        isDark = isSystemInDarkTheme()
                    )
                )
            }
        } else {
            Modifier
        }

        Box(modifier = profilSantriModifier) {
            DrawerMenuItemElegant(icon = Icons.Outlined.Person, text = "Profil Santri", isEnabled = isNavEnabled) {
                closeDrawer(); navController.navigate(Screen.SantriDetail.createRoute(activeSantriNis!!))
            }
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.CheckCircle, text = "Absensi", isEnabled = isNavEnabled, isExpandable = true, isExpanded = absensiSubmenuExpanded, onToggleExpand = { absensiSubmenuExpanded = !absensiSubmenuExpanded }) {}
        if (absensiSubmenuExpanded) {
            DrawerSubMenuItem(text = "Ringkasan Absensi", isEnabled = isNavEnabled) {
                closeDrawer(); navController.navigate(Screen.Absensi.createRoute(activeSantriNis!!))
            }
            DrawerSubMenuItem(text = "Absensi Lengkap", isEnabled = isNavEnabled) {
                closeDrawer(); navController.navigate(Screen.AbsensiLengkap.createRoute(activeSantriNis!!))
            }
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.MenuBook, text = "Progres Hafalan", isEnabled = isNavEnabled) {
            closeDrawer(); navController.navigate(Screen.Hafalan.createRoute(activeSantriNis!!))
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.AutoStories, text = "Murojaah Hafalan", isEnabled = isNavEnabled) {
            closeDrawer(); navController.navigate(Screen.Murajaah.createRoute(activeSantriNis!!))
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.ReportProblem, text = "Catatan Kedisiplinan", isEnabled = isNavEnabled) {
            closeDrawer(); navController.navigate(Screen.Pelanggaran.createRoute(activeSantriNis!!))
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.MedicalServices, text = "Rekam Medis", isEnabled = isNavEnabled) {
            closeDrawer(); navController.navigate(Screen.Kesehatan.createRoute(activeSantriNis!!))
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.Assignment, text = "Izin Santri", isEnabled = isNavEnabled) {
            closeDrawer(); navController.navigate(Screen.Perizinan.createRoute(activeSantriNis!!))
        }

        Spacer(modifier = Modifier.height(8.dp))
        DrawerSectionLabel("KEUANGAN")
        DrawerMenuItemElegant(icon = Icons.Outlined.CreditCard, text = "Tagihan & SPP", isEnabled = isNavEnabled) {
            closeDrawer(); navController.navigate(Screen.Keuangan.createRoute(activeSantriNis!!))
        }
        DrawerMenuItemElegant(icon = Icons.Outlined.AccountBalanceWallet, text = "Dompet Santri", isEnabled = isNavEnabled) {
            closeDrawer()
            comingSoonTitle = "Dompet Santri"
            showComingSoonDialog = true
        }
        if (isKantin) {
            DrawerMenuItemElegant(icon = Icons.Outlined.PointOfSale, text = "Kantin Merchant", isEnabled = true) {
                closeDrawer(); navController.navigate(Screen.WalletKantin.route)
            }
        }

    } else {
        DrawerMenuItemElegant(
            icon    = Icons.Outlined.AutoAwesome,
            text    = "Tanya AI",
            onClick = {
                closeDrawer()
                comingSoonTitle = "Tanya AI"
                showComingSoonDialog = true
            }
        )

        // Tutorial Target: Phase 1 Step 2 - "Masuk ke Akun"
        val loginMenuItemModifier = if (tutorialPhase == TutorialPhase.PHASE_1_STEP_2 && showcaseScope != null) {
            with(showcaseScope) {
                Modifier.showcase(
                    index = 1,
                    message = tutorialMsg(
                        text = "Masuk ke Akun — Ketuk untuk masuk dengan akun wali santri Anda.",
                        isDark = isSystemInDarkTheme()
                    )
                )
            }
        } else {
            Modifier
        }

        DrawerMenuItemElegant(
            icon    = Icons.Outlined.Login,
            text    = "Masuk ke Akun",
            modifier = loginMenuItemModifier,
            onClick = { closeDrawer(); navController.navigate(Screen.Login.route) }
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── Divider dengan gradient emas memudar (bukan garis abu polos) ──────────
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
    ) {
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.25f),
                    primaryColor.copy(alpha = 0.35f),
                    primaryColor.copy(alpha = 0.25f),
                    Color.Transparent
                )
            ),
            start       = Offset(0f, 0f),
            end         = Offset(size.width, 0f),
            strokeWidth = size.height
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    DrawerMenuItemElegant(
        icon    = Icons.Outlined.Brightness4,
        text    = "Ganti Tema",
        onClick = onToggleTheme
    )

    DrawerMenuItemElegant(
        icon    = Icons.Outlined.SystemUpdate,
        text    = "Cek Update",
        onClick = {
            closeDrawer()
            drawerScope.launch {
                val result = com.alhasanah.alhasanahmedia.util.UpdateChecker.checkUpdateAsync()
                when (result) {
                    is com.alhasanah.alhasanahmedia.util.UpdateResult.Available -> {
                        drawerUpdateInfo = result.info
                        showUpdateDialog = true
                    }
                    is com.alhasanah.alhasanahmedia.util.UpdateResult.UpToDate -> {
                        updateResultMessage = "Aplikasi sudah versi terbaru ✓"
                        showUpdateResultDialog = true
                    }
                    is com.alhasanah.alhasanahmedia.util.UpdateResult.Error -> {
                        updateResultMessage = "Gagal cek update: ${result.message}"
                        showUpdateResultDialog = true
                    }
                }
            }
        }
    )

    DrawerMenuItemElegant(
        icon    = Icons.Outlined.Info,
        text    = "Tentang Kami",
        onClick = { closeDrawer(); navController.navigate(Screen.TentangKami.route) }
    )

    if (isLoggedIn) {
        DrawerMenuItemElegant(
            icon      = Icons.Outlined.Logout,
            text      = "Keluar",
            textColor = MaterialTheme.colorScheme.error,
            iconColor = MaterialTheme.colorScheme.error,
            onClick   = onLogout
        )
    }

    // Coming Soon dialog
    if (showComingSoonDialog) {
        ComingSoonDialog(
            title = comingSoonTitle,
            onDismiss = { showComingSoonDialog = false }
        )
    }

    // Update dialog
    if (showUpdateDialog && drawerUpdateInfo != null) {
        com.alhasanah.alhasanahmedia.ui.components.UpdateDialog(
            info = drawerUpdateInfo!!,
            onDismiss = {
                showUpdateDialog = false
                drawerUpdateInfo = null
            }
        )
    }

    // Update result dialog (up-to-date / error)
    if (showUpdateResultDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUpdateResultDialog = false },
            title = { Text("Cek Update") },
            text = { Text(updateResultMessage) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showUpdateResultDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  SECTION LABEL
// Perubahan: bar aksesn emas di kiri + typography lebih halus
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrawerSectionLabel(text: String) {
    Row(
        modifier          = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bar aksen emas vertikal
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(11.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    RoundedCornerShape(2.dp)
                )
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color         = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                fontWeight    = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize      = 9.5.sp
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ██  MENU ITEM PREMIUM
// Perubahan:
//   • Icon dalam rounded-square container (frosted glass tinted)
//   • Custom Row + clickable (bukan NavigationDrawerItem generic)
//   • Hierarchy: SemiBold aktif vs Regular non-aktif
//   • Disabled state: alpha berbeda + icon container lebih pudar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DrawerMenuItemElegant(
    icon     : ImageVector,
    text     : String,
    isEnabled: Boolean = true,
    modifier : Modifier = Modifier,
    textColor: Color   = MaterialTheme.colorScheme.onSurface,
    iconColor: Color   = MaterialTheme.colorScheme.primary,
    isExpandable: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onClick  : () -> Unit
) {
    val contentAlpha   = if (isEnabled) 1.0f  else 0.35f
    val containerAlpha = if (isEnabled) 0.12f else 0.05f
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevron"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isEnabled) {
                if (isExpandable && onToggleExpand != null) {
                    onToggleExpand()
                } else {
                    onClick()
                }
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon dalam frosted rounded-square container
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = iconColor.copy(alpha = containerAlpha),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector       = icon,
                contentDescription = text,
                tint              = iconColor.copy(alpha = contentAlpha),
                modifier          = Modifier.size(19.dp)
            )
        }

        // Label
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal,
                color      = textColor.copy(alpha = contentAlpha)
            ),
            modifier = Modifier.weight(1f)
        )

        // Expand/Collapse chevron
        if (isExpandable) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Tutup" else "Buka",
                tint = textColor.copy(alpha = contentAlpha * 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
fun DrawerSubMenuItem(
    text     : String,
    isEnabled: Boolean = true,
    onClick  : () -> Unit
) {
    val contentAlpha = if (isEnabled) 1.0f else 0.35f
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 4.dp, top = 1.dp, bottom = 1.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha * 0.6f),
                    shape = CircleShape
                )
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isEnabled) FontWeight.Medium else FontWeight.Normal,
                color      = textColor.copy(alpha = contentAlpha * 0.9f)
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper & Preview — TIDAK ADA PERUBAHAN
// ─────────────────────────────────────────────────────────────────────────────
private fun border(
    width: androidx.compose.ui.unit.Dp,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape
) = androidx.compose.foundation.BorderStroke(width, color)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AlhasanahMediaTheme(darkTheme = false) {
        // Preview placeholder — ViewModel tidak tersedia di preview
    }
}
