package com.alhasanah.alhasanahmedia.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alhasanah.alhasanahmedia.data.model.TagihanDto
import com.alhasanah.alhasanahmedia.data.model.TagihanStatus
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniDirectoryScreen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniChatScreen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniProfileEditScreen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniProfileScreen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniInfoScreen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniRegisterScreen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniSettingsScreen
import com.alhasanah.alhasanahmedia.ui.alumni.ForumAlumniScreen
import com.alhasanah.alhasanahmedia.ui.admin.ADMIN_PANEL_URL
import com.alhasanah.alhasanahmedia.ui.admin.AdminPanelScreen
import com.alhasanah.alhasanahmedia.ui.auth.LoginScreen
import com.alhasanah.alhasanahmedia.ui.berita.BeritaDetailScreen
import com.alhasanah.alhasanahmedia.ui.devotion.DevotionScreen
import com.alhasanah.alhasanahmedia.ui.devotion.KitabKuningScreen
import com.alhasanah.alhasanahmedia.ui.donasi.DonasiScreen
import com.alhasanah.alhasanahmedia.ui.falak.FalakEphemerisScreen
import com.alhasanah.alhasanahmedia.ui.falak.GerhanaBulanScreen
import com.alhasanah.alhasanahmedia.ui.falak.HisabHilalScreen
import com.alhasanah.alhasanahmedia.ui.hadith.HadithDetailScreen
import com.alhasanah.alhasanahmedia.ui.hadith.HadithScreen
import com.alhasanah.alhasanahmedia.ui.home.HomeScreen
import com.alhasanah.alhasanahmedia.ui.ibadah.IbadahGuideScreen
import com.alhasanah.alhasanahmedia.ui.islamiccalendar.IslamicCalendarScreen
import com.alhasanah.alhasanahmedia.ui.notifikasi.NotificationScreen
import com.alhasanah.alhasanahmedia.ui.keuangan.KeuanganScreen
import com.alhasanah.alhasanahmedia.ui.payment.PaymentStatus
import com.alhasanah.alhasanahmedia.ui.payment.PaymentInstructionData
import com.alhasanah.alhasanahmedia.ui.payment.PaymentInstructionScreen
import com.alhasanah.alhasanahmedia.ui.payment.PaymentResultData
import com.alhasanah.alhasanahmedia.ui.payment.PaymentResultScreen
import com.alhasanah.alhasanahmedia.ui.prayer.PrayerScheduleScreen
import com.alhasanah.alhasanahmedia.ui.qibla.QiblaScreen
import com.alhasanah.alhasanahmedia.ui.rag.RagChatScreen
import com.alhasanah.alhasanahmedia.ui.santri.AbsensiScreen
import com.alhasanah.alhasanahmedia.ui.santri.AbsensiViewModel
import com.alhasanah.alhasanahmedia.ui.santri.*
import com.alhasanah.alhasanahmedia.ui.splash.SplashScreen
import com.alhasanah.alhasanahmedia.ui.wallet.KantinWalletScreen
import com.alhasanah.alhasanahmedia.ui.wallet.WalletDisputeScreen
import com.alhasanah.alhasanahmedia.ui.wallet.WalletWaliScreen
import com.alhasanah.alhasanahmedia.ui.weather.WeatherScreen
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object AdminPanel : Screen("admin_panel?url={url}") {
        const val baseRoute = "admin_panel"
        fun createRoute(url: String = ADMIN_PANEL_URL) = "admin_panel?url=${url.enc()}"
    }
    object Login : Screen("login")
    object SantriList : Screen("santri_list")
    object Prestasi : Screen("prestasi")
    object BeritaList : Screen("berita")
    object BeritaDetail : Screen("berita_detail/{slug}") { // New Route
        fun createRoute(slug: String) = "berita_detail/$slug"
    }
    object Hafalan : Screen("hafalan/{nis}") {
        fun createRoute(nis: String) = "hafalan/$nis"
    }
    object Murajaah : Screen("murajaah/{nis}") {
        fun createRoute(nis: String) = "murajaah/$nis"
    }
    object Pelanggaran : Screen("pelanggaran/{nis}") {
        fun createRoute(nis: String) = "pelanggaran/$nis"
    }
    object Kesehatan : Screen("kesehatan/{nis}") {
        fun createRoute(nis: String) = "kesehatan/$nis"
    }
    object Perizinan : Screen("perizinan/{nis}") {
        fun createRoute(nis: String) = "perizinan/$nis"
    }
    object Keuangan : Screen("keuangan/{nis}") {
        fun createRoute(nis: String) = "keuangan/$nis"
    }
    object SantriDetail : Screen("santri_detail/{nis}") {
        fun createRoute(nis: String) = "santri_detail/$nis"
    }
    object Notifications : Screen("notifications")
    object HafalanKitab : Screen("hafalan_kitab/{nis}") {
        fun createRoute(nis: String) = "hafalan_kitab/$nis"
    }
    object Absensi : Screen("absensi/{nis}") {
        fun createRoute(nis: String) = "absensi/$nis"
    }
    object AbsensiLengkap : Screen("absensi_lengkap/{nis}") {
        fun createRoute(nis: String) = "absensi_lengkap/$nis"
    }
    object Quran : Screen("quran")
    object Qibla : Screen("qibla")
    object PrayerSchedule : Screen("prayer_schedule")
    object Weather : Screen("weather")
    object Hadith : Screen("hadith")
    object Devotion : Screen("devotion")
    object KitabKuning : Screen("kitab_kuning")
    object IbadahGuide : Screen("ibadah_guide")
    object IslamicCalendar : Screen("islamic_calendar")
    object FalakEphemeris : Screen("falak_ephemeris")
    object HisabHilal : Screen("hisab_hilal")
    object GerhanaBulan : Screen("gerhana_bulan")
    object HadithDetail : Screen("hadith_detail/{id}") {
        fun createRoute(id: Int) = "hadith_detail/$id"
    }
    object RagChat : Screen("rag_chat")
    object AlumniForum : Screen("alumni_forum?threadId={threadId}") {
        const val baseRoute = "alumni_forum"
        fun createRoute(threadId: String? = null) = threadId?.let { "alumni_forum?threadId=$it" } ?: baseRoute
    }
    object AlumniChat : Screen("alumni_chat?conversationId={conversationId}&targetUserId={targetUserId}") {
        const val baseRoute = "alumni_chat"
        fun createRoute(conversationId: String? = null) =
            conversationId?.let { "alumni_chat?conversationId=$it" } ?: baseRoute
        fun createDirectRoute(targetUserId: String) = "alumni_chat?targetUserId=$targetUserId"
    }
    object AlumniDirectory : Screen("alumni_directory")
    object AlumniRegister : Screen("alumni_register")
    object AlumniProfile : Screen("alumni_profile")
    object AlumniProfileDetail : Screen("alumni_profile/{alumniId}") {
        fun createRoute(alumniId: String) = "alumni_profile/$alumniId"
    }
    object AlumniProfileEdit : Screen("alumni_profile_edit")
    object AlumniSettings : Screen("alumni_settings")
    object AlumniNotifications : Screen("alumni_notifications")
    object AlumniInfo : Screen("alumni_info/{page}") {
        fun createRoute(page: String) = "alumni_info/$page"
    }
    object Donasi : Screen("donasi/{nis}") {
        fun createRoute(nis: String) = "donasi/$nis"
    }
    object WalletWali : Screen("wallet_wali/{nis}") {
        fun createRoute(nis: String) = "wallet_wali/$nis"
    }
    object WalletKantin : Screen("wallet_kantin")
    object WalletDispute : Screen("wallet_dispute/{ledgerId}") {
        fun createRoute(ledgerId: String) = "wallet_dispute/$ledgerId"
    }
    object PaymentResult : Screen("payment_result?status={status}&orderId={orderId}&transactionId={transactionId}&message={message}") {
        fun createRoute(status: String, orderId: String, transactionId: String = "", message: String = "") =
            "payment_result?status=${status.enc()}&orderId=${orderId.enc()}&transactionId=${transactionId.enc()}&message=${message.enc()}"
    }
    object PaymentInstruction : Screen(
        "payment_instruction?orderId={orderId}&transactionId={transactionId}&methodCode={methodCode}&methodLabel={methodLabel}&amount={amount}&expiresAt={expiresAt}&qrUrl={qrUrl}&deeplinkUrl={deeplinkUrl}&vaNumber={vaNumber}&bank={bank}&billerCode={billerCode}&billKey={billKey}&paymentCode={paymentCode}&store={store}&message={message}"
    ) {
        fun createRoute(data: PaymentInstructionData): String =
            "payment_instruction" +
                "?orderId=${data.orderId.enc()}" +
                "&transactionId=${data.transactionId.enc()}" +
                "&methodCode=${data.methodCode.enc()}" +
                "&methodLabel=${data.methodLabel.enc()}" +
                "&amount=${data.amount}" +
                "&expiresAt=${data.expiresAt.enc()}" +
                "&qrUrl=${data.qrUrl.enc()}" +
                "&deeplinkUrl=${data.deeplinkUrl.enc()}" +
                "&vaNumber=${data.vaNumber.enc()}" +
                "&bank=${data.bank.enc()}" +
                "&billerCode=${data.billerCode.enc()}" +
                "&billKey=${data.billKey.enc()}" +
                "&paymentCode=${data.paymentCode.enc()}" +
                "&store=${data.store.enc()}" +
                "&message=${data.message.enc()}"
    }
    object SurahDetail : Screen("surah_detail/{nomor}") {
        fun createRoute(nomor: Int) = "surah_detail/$nomor"
    }
    object JuzDetail : Screen("juz_detail/{nomor}") {
        fun createRoute(nomor: Int) = "juz_detail/$nomor"
    }
    object TentangKami : Screen("tentang_kami")
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
    openDrawer: () -> Unit,
    tutorialPhase: com.alhasanah.alhasanahmedia.ui.tutorial.TutorialPhase = com.alhasanah.alhasanahmedia.ui.tutorial.TutorialPhase.NONE
) {
    val santriActivityViewModel: SantriActivityViewModel = koinViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(500)) +
                    fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(500)) +
                    fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(500)) +
                    fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(500)) +
                    fadeOut(animationSpec = tween(500))
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
composable(Screen.Home.route) {
            HomeScreen(
                isLoggedIn = isLoggedIn,
                openDrawer = openDrawer,
                navController = navController,
                tutorialPhase = tutorialPhase
            )
        }
        composable(
            route = Screen.AdminPanel.route,
            arguments = listOf(navArgument("url") {
                type = NavType.StringType
                nullable = true
                defaultValue = ADMIN_PANEL_URL
            })
        ) { backStackEntry ->
            AdminPanelScreen(
                navController = navController,
                initialUrl = backStackEntry.arguments?.getString("url") ?: ADMIN_PANEL_URL
            )
        }
        composable(Screen.Notifications.route) {
            // Kita akan buat layar ini nanti
            NotificationScreen(navController = navController)
        }
        composable(Screen.AlumniNotifications.route) {
            NotificationScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                tutorialPhase = tutorialPhase
            )
        }
        composable(Screen.BeritaList.route) {
            com.alhasanah.alhasanahmedia.ui.berita.BeritaListScreen(navController = navController)
        }
        composable(Screen.SantriList.route) {
            SantriListScreen(navController = navController)
        }
        composable(Screen.Prestasi.route) {
            PrestasiScreen(navController = navController)
        }
         composable(
            route = Screen.BeritaDetail.route, // New Destination
            arguments = listOf(navArgument("slug") { type = NavType.StringType })
        ) { backStackEntry ->
            val slug = backStackEntry.arguments?.getString("slug") ?: ""
            BeritaDetailScreen(slug = slug, onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Keuangan.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: ""
            KeuanganScreen(santriNis = santriNis, navController = navController)
        }
        composable(
            route = Screen.SantriDetail.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            SantriDetailScreen(
                santriNis = santriNis,
                navController = navController,
                viewModel = koinViewModel { parametersOf(santriNis) }
            )
        }

        // New Santri Activity Screens
        composable(
            route = Screen.Hafalan.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            HafalanScreen(
                navController = navController,
                viewModel = santriActivityViewModel,
                santriNis = santriNis
            )
        }
        composable(
            route = Screen.Murajaah.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            MurajaahScreen(
                navController = navController,
                viewModel = santriActivityViewModel,
                santriNis = santriNis
            )
        }
        composable(
            route = Screen.Pelanggaran.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            PelanggaranScreen(
                navController = navController,
                viewModel = santriActivityViewModel,
                santriNis = santriNis
            )
        }
        composable(
            route = Screen.Kesehatan.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            KesehatanScreen(
                navController = navController,
                viewModel = santriActivityViewModel,
                santriNis = santriNis
            )
        }
        composable(
            route = Screen.Perizinan.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            PerizinanScreen(
                navController = navController,
                viewModel = santriActivityViewModel,
                santriNis = santriNis
            )
        }
        composable(
            route = Screen.HafalanKitab.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            HafalanKitabScreen(
                navController = navController,
                viewModel = santriActivityViewModel,
                santriNis = santriNis
            )
        }
        composable(
            route = Screen.Absensi.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            val absensiViewModel: AbsensiViewModel = koinViewModel()
            AbsensiScreen(
                santriNis = santriNis,
                viewModel = absensiViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AbsensiLengkap.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: return@composable
            val absensiLengkapViewModel: com.alhasanah.alhasanahmedia.ui.absensilengkap.AbsensiLengkapViewModel = koinViewModel()
            com.alhasanah.alhasanahmedia.ui.absensilengkap.AbsensiLengkapScreen(
                santriNis = santriNis,
                viewModel = absensiLengkapViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Quran Screens
        composable(Screen.Quran.route) {
            com.alhasanah.alhasanahmedia.ui.quran.QuranScreen(navController = navController)
        }
        composable(Screen.Qibla.route) {
            QiblaScreen(navController = navController)
        }
        composable(Screen.PrayerSchedule.route) {
            PrayerScheduleScreen(navController = navController)
        }
        composable(Screen.Weather.route) {
            WeatherScreen(navController = navController)
        }
        composable(Screen.Hadith.route) {
            HadithScreen(navController = navController)
        }
        composable(Screen.Devotion.route) {
            DevotionScreen(navController = navController)
        }
        composable(Screen.KitabKuning.route) {
            KitabKuningScreen(navController = navController)
        }
        composable(Screen.IbadahGuide.route) {
            IbadahGuideScreen(navController = navController)
        }
        composable(Screen.IslamicCalendar.route) {
            IslamicCalendarScreen(navController = navController)
        }
        composable(Screen.FalakEphemeris.route) {
            FalakEphemerisScreen(
                navController = navController,
                onOpenHisabHilal = { navController.navigate(Screen.HisabHilal.route) },
                onOpenGerhanaBulan = { navController.navigate(Screen.GerhanaBulan.route) },
            )
        }
        composable(Screen.HisabHilal.route) {
            HisabHilalScreen(navController = navController)
        }
        composable(Screen.GerhanaBulan.route) {
            GerhanaBulanScreen(navController = navController)
        }
        composable(
            route = Screen.HadithDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            HadithDetailScreen(
                id = backStackEntry.arguments?.getInt("id") ?: 1,
                navController = navController
            )
        }
        composable(Screen.RagChat.route) {
            RagChatScreen(navController = navController)
        }
        composable(
            route = Screen.AlumniForum.route,
            arguments = listOf(navArgument("threadId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            ForumAlumniScreen(
                navController = navController,
                initialThreadId = backStackEntry.arguments?.getString("threadId")
            )
        }
        composable(
            route = Screen.AlumniChat.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("targetUserId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            AlumniChatScreen(
                navController = navController,
                initialConversationId = backStackEntry.arguments?.getString("conversationId"),
                initialTargetUserId = backStackEntry.arguments?.getString("targetUserId")
            )
        }
        composable(Screen.AlumniDirectory.route) {
            AlumniDirectoryScreen(navController = navController)
        }
        composable(Screen.AlumniRegister.route) {
            AlumniRegisterScreen(navController = navController)
        }
        composable(Screen.AlumniProfile.route) {
            AlumniProfileScreen(navController = navController)
        }
        composable(
            route = Screen.AlumniProfileDetail.route,
            arguments = listOf(navArgument("alumniId") { type = NavType.StringType })
        ) { backStackEntry ->
            AlumniProfileScreen(
                navController = navController,
                alumniId = backStackEntry.arguments?.getString("alumniId")
            )
        }
        composable(Screen.AlumniProfileEdit.route) {
            AlumniProfileEditScreen(navController = navController)
        }
        composable(Screen.AlumniSettings.route) {
            AlumniSettingsScreen(navController = navController)
        }
        composable(
            route = Screen.AlumniInfo.route,
            arguments = listOf(navArgument("page") { type = NavType.StringType })
        ) { backStackEntry ->
            AlumniInfoScreen(
                navController = navController,
                page = backStackEntry.arguments?.getString("page").orEmpty()
            )
        }
        composable(
            route = Screen.SurahDetail.route,
            arguments = listOf(navArgument("nomor") { type = NavType.IntType })
        ) { backStackEntry ->
            val nomor = backStackEntry.arguments?.getInt("nomor") ?: 1
            com.alhasanah.alhasanahmedia.ui.quran.SurahDetailScreen(nomor = nomor, navController = navController)
        }
        composable(
            route = Screen.JuzDetail.route,
            arguments = listOf(navArgument("nomor") { type = NavType.IntType })
        ) { backStackEntry ->
            val nomor = backStackEntry.arguments?.getInt("nomor") ?: 1
            com.alhasanah.alhasanahmedia.ui.quran.JuzDetailScreen(nomor = nomor, navController = navController)
        }
        composable(
            route = Screen.Donasi.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            val santriNis = backStackEntry.arguments?.getString("nis") ?: ""
            DonasiScreen(
                santriNis = santriNis,
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }
        composable(
            route = Screen.WalletWali.route,
            arguments = listOf(navArgument("nis") { type = NavType.StringType })
        ) { backStackEntry ->
            WalletWaliScreen(
                santriNis = backStackEntry.arguments?.getString("nis").orEmpty(),
                navController = navController
            )
        }
        composable(Screen.WalletKantin.route) {
            KantinWalletScreen(navController = navController)
        }
        composable(
            route = Screen.WalletDispute.route,
            arguments = listOf(navArgument("ledgerId") { type = NavType.LongType })
        ) { backStackEntry ->
            WalletDisputeScreen(
                ledgerId = backStackEntry.arguments?.getLong("ledgerId") ?: 0L,
                navController = navController
            )
        }
        composable(
            route = Screen.PaymentResult.route,
            arguments = listOf(
                navArgument("status") { defaultValue = "FAILED" },
                navArgument("orderId") { defaultValue = "-" },
                navArgument("transactionId") { defaultValue = "" },
                navArgument("message") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val status = PaymentStatus.valueOf(
                backStackEntry.arguments?.getString("status") ?: "FAILED"
            )
            PaymentResultScreen(
                resultData = PaymentResultData(
                    status = status,
                    orderId = backStackEntry.arguments?.getString("orderId") ?: "-",
                    transactionId = backStackEntry.arguments?.getString("transactionId") ?: "",
                    message = backStackEntry.arguments?.getString("message") ?: ""
                ),
                onActionClick = {
                    when (status) {
                        PaymentStatus.SUCCESS -> navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                        }
                        PaymentStatus.PENDING -> navController.popBackStack()
                        PaymentStatus.FAILED -> navController.popBackStack()
                    }
                },
                onBackHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.PaymentInstruction.route,
            arguments = listOf(
                navArgument("orderId") { defaultValue = "-" },
                navArgument("transactionId") { defaultValue = "" },
                navArgument("methodCode") { defaultValue = "qris" },
                navArgument("methodLabel") { defaultValue = "QRIS" },
                navArgument("amount") { type = NavType.LongType; defaultValue = 0L },
                navArgument("expiresAt") { defaultValue = "" },
                navArgument("qrUrl") { defaultValue = "" },
                navArgument("deeplinkUrl") { defaultValue = "" },
                navArgument("vaNumber") { defaultValue = "" },
                navArgument("bank") { defaultValue = "" },
                navArgument("billerCode") { defaultValue = "" },
                navArgument("billKey") { defaultValue = "" },
                navArgument("paymentCode") { defaultValue = "" },
                navArgument("store") { defaultValue = "" },
                navArgument("message") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val data = PaymentInstructionData(
                orderId = backStackEntry.arguments?.getString("orderId") ?: "-",
                transactionId = backStackEntry.arguments?.getString("transactionId") ?: "",
                methodCode = backStackEntry.arguments?.getString("methodCode") ?: "qris",
                methodLabel = backStackEntry.arguments?.getString("methodLabel") ?: "QRIS",
                amount = backStackEntry.arguments?.getLong("amount") ?: 0L,
                expiresAt = backStackEntry.arguments?.getString("expiresAt") ?: "",
                qrUrl = backStackEntry.arguments?.getString("qrUrl") ?: "",
                deeplinkUrl = backStackEntry.arguments?.getString("deeplinkUrl") ?: "",
                vaNumber = backStackEntry.arguments?.getString("vaNumber") ?: "",
                bank = backStackEntry.arguments?.getString("bank") ?: "",
                billerCode = backStackEntry.arguments?.getString("billerCode") ?: "",
                billKey = backStackEntry.arguments?.getString("billKey") ?: "",
                paymentCode = backStackEntry.arguments?.getString("paymentCode") ?: "",
                store = backStackEntry.arguments?.getString("store") ?: "",
                message = backStackEntry.arguments?.getString("message") ?: ""
            )
            val supabaseClient: SupabaseClient = koinInject()
            val scope = rememberCoroutineScope()
            var lastKnownStatus by remember(data.orderId) { mutableStateOf(PaymentStatus.PENDING) }

            fun navigatePaymentStatus(status: PaymentStatus) {
                val message = when (status) {
                    PaymentStatus.SUCCESS -> "Pembayaran berhasil dikonfirmasi."
                    PaymentStatus.FAILED -> "Pembayaran gagal atau kedaluwarsa."
                    PaymentStatus.PENDING -> "Pembayaran sedang menunggu konfirmasi Midtrans."
                }
                navController.navigate(
                    Screen.PaymentResult.createRoute(
                        status = status.name,
                        orderId = data.orderId,
                        transactionId = data.transactionId,
                        message = message
                    )
                ) {
                    popUpTo(Screen.PaymentInstruction.route) { inclusive = true }
                    launchSingleTop = true
                }
            }

            LaunchedEffect(data.orderId) {
                while (lastKnownStatus == PaymentStatus.PENDING) {
                    val status = supabaseClient.fetchPaymentStatus(data.orderId)
                    lastKnownStatus = status
                    if (status != PaymentStatus.PENDING) {
                        navigatePaymentStatus(status)
                        break
                    }
                    delay(4_000)
                }
            }

            PaymentInstructionScreen(
                data = data,
                onBack = { navController.popBackStack() },
                onCheckStatus = {
                    scope.launch {
                        val status = supabaseClient.fetchPaymentStatus(data.orderId)
                        lastKnownStatus = status
                        navigatePaymentStatus(status)
                    }
                },
                onBackHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.TentangKami.route) {
            com.alhasanah.alhasanahmedia.ui.about.TentangKamiScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun String.enc(): String = Uri.encode(this)

private suspend fun SupabaseClient.fetchPaymentStatus(orderId: String): PaymentStatus {
    if (orderId.isBlank() || orderId == "-") return PaymentStatus.PENDING
    val transactionStatus = runCatching {
        val rows = from("transaksi_keuangan")
            .select(Columns.raw("status,status_transaksi,kategori")) {
                filter {
                    eq("midtrans_order_id", orderId)
                }
            }
            .decodeList<PaymentTransactionStatusRow>()

        val row = rows.firstOrNull()
        paymentStatusFromValues(row?.status, row?.statusTransaksi)
    }.getOrDefault(PaymentStatus.PENDING)

    if (transactionStatus != PaymentStatus.PENDING) return transactionStatus

    val walletIntentStatus = runCatching {
        val rows = from("wallet_payment_intents")
            .select(Columns.raw("status")) {
                filter {
                    eq("midtrans_order_id", orderId)
                }
            }
            .decodeList<WalletPaymentIntentStatusRow>()

        paymentStatusFromValues(rows.firstOrNull()?.status)
    }.getOrDefault(PaymentStatus.PENDING)

    if (walletIntentStatus != PaymentStatus.PENDING) return walletIntentStatus

    val publicStatus = runCatching {
        postgrest.rpc(
            "get_payment_status_public",
            PublicPaymentStatusParams(orderId = orderId)
        ).decodeList<PublicPaymentStatusRow>()
            .firstOrNull()
            ?.status
            ?.let(PaymentStatus::valueOf)
            ?: PaymentStatus.PENDING
    }.getOrDefault(PaymentStatus.PENDING)

    if (publicStatus != PaymentStatus.PENDING) return publicStatus

    val legacyTagihanId = orderId
        .takeIf { "_" !in it }
        ?.takeIf { it.length >= 32 }

    val tagihanStatus = legacyTagihanId?.let { tagihanId ->
        runCatching {
            from("tagihan_santri")
                .select {
                    filter {
                        eq("id", tagihanId)
                    }
                }
                .decodeList<TagihanDto>()
                .firstOrNull()
                ?.status
        }.getOrNull()
    }

    return when (tagihanStatus) {
        TagihanStatus.LUNAS -> PaymentStatus.SUCCESS
        else -> PaymentStatus.PENDING
    }
}

private fun paymentStatusFromValues(vararg values: String?): PaymentStatus {
    val normalizedValues = values.mapNotNull { it?.trim()?.lowercase() }
    return when {
        normalizedValues.any { it in paymentSuccessStatuses } -> PaymentStatus.SUCCESS
        normalizedValues.any { it in paymentFailedStatuses } -> PaymentStatus.FAILED
        else -> PaymentStatus.PENDING
    }
}

private val paymentSuccessStatuses = setOf(
    "success",
    "settlement",
    "capture",
    "paid",
    "lunas",
    "posted"
)

private val paymentFailedStatuses = setOf(
    "failed",
    "failure",
    "deny",
    "denied",
    "cancel",
    "canceled",
    "cancelled",
    "expire",
    "expired"
)

@Serializable
private data class PaymentTransactionStatusRow(
    @SerialName("status")
    val status: String? = null,
    @SerialName("status_transaksi")
    val statusTransaksi: String? = null,
    @SerialName("kategori")
    val kategori: String? = null
)

@Serializable
private data class WalletPaymentIntentStatusRow(
    @SerialName("status")
    val status: String? = null
)

@Serializable
private data class PublicPaymentStatusParams(
    @SerialName("p_order_id")
    val orderId: String
)

@Serializable
private data class PublicPaymentStatusRow(
    @SerialName("status")
    val status: String
)
