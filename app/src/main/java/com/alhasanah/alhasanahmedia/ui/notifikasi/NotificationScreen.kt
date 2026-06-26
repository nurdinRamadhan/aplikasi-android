package com.alhasanah.alhasanahmedia.ui.notifikasi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import android.util.Log
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.alumni.AlumniPremiumTheme
import com.alhasanah.alhasanahmedia.ui.components.AppGradientBackground
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.androidx.compose.koinViewModel

// ─── Luxury Color Palette ─────────────────────────────────────────────────────
private val IvoryWhite    = Color(0xFFFFFBF0)
private val PureWhite     = Color(0xFFFFFFFF)
private val GoldPrimary   = Color(0xFFD4A017)
private val GoldLight     = Color(0xFFE8C55A)
private val GoldDeep      = Color(0xFFAA7C1F)
private val GoldShimmer   = Color(0xFFFAF0C0)
private val CharcoalDeep  = Color(0xFF1A1A1A)
private val TextPrimary   = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF6B6B6B)
private val DividerGold   = Color(0xFFE8C55A).copy(alpha = 0.22f)
private val CardSurface   = Color(0xFFFFFDF5)

// ── Semantic colors per category (muted to stay on-brand) ────────────────────
private val ColorTagihan    = Color(0xFF2E7D32) // deep green
private val ColorPelanggaran= Color(0xFFC62828) // deep red
private val ColorPerizinan  = Color(0xFF1565C0) // deep blue
private val ColorKesehatan  = Color(0xFFE65100) // deep orange
private val ColorPrestasi   = Color(0xFF00897B) // refined teal
private val ColorDefault    = GoldDeep

private val GoldGradient = Brush.linearGradient(
    colors = listOf(GoldDeep, GoldPrimary, GoldLight, GoldPrimary, GoldDeep)
)
private val IvoryGradient = Brush.verticalGradient(
    colors = listOf(IvoryWhite, PureWhite)
)
private val HeaderGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF111111), Color(0xFF1F1F1F))
)

// ─── DATA MODEL ───────────────────────────────────────────────────────────────
@Serializable
data class NotificationItem(
    @SerialName("id")           val id: String,
    @SerialName("title")        val title: String,
    @SerialName("body")         val body: String,
    @SerialName("data")         val data: JsonElement,
    @SerialName("status")       val status: String,
    @SerialName("source_table") val source_table: String? = null,
    @SerialName("created_at")   val created_at: String
)

@Serializable
private data class UpdateNotificationStatusDto(
    @SerialName("status")
    val status: String
)

// ─── VIEW MODEL ───────────────────────────────────────────────────────────────
class NotificationViewModel(
    private val postgrest: Postgrest,
    private val auth: Auth
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { fetchNotifications() }

    fun fetchNotifications() {
        val user = auth.currentUserOrNull() ?: run {
            Log.e("NotifVM", "User not logged in")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = postgrest["notification_queue"].select {
                    filter { eq("user_id", user.id) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                val list = response.decodeList<NotificationItem>()
                _notifications.value = list
                Log.d("NotifVM", "Berhasil mengambil ${list.size} notifikasi")
            } catch (e: Exception) {
                Log.e("NotifVM", "Gagal decode notifikasi: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                postgrest["notification_queue"].delete {
                    filter { eq("id", notificationId) }
                }
                _notifications.value = _notifications.value.filter { it.id != notificationId }
                Log.d("NotifVM", "Notifikasi $notificationId berhasil dihapus")
            } catch (e: Exception) {
                Log.e("NotifVM", "Gagal menghapus notifikasi", e)
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                postgrest["notification_queue"].update(UpdateNotificationStatusDto("read")) {
                    filter { eq("id", notificationId) }
                }
                _notifications.value = _notifications.value.map { item ->
                    if (item.id == notificationId) item.copy(status = "read") else item
                }
            } catch (e: Exception) {
                Log.e("NotifVM", "Gagal menandai notifikasi dibaca", e)
            }
        }
    }
}

// ─── MAIN SCREEN ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    viewModel: NotificationViewModel = koinViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val isDark = isAppInDarkTheme()

    LaunchedEffect(Unit) { viewModel.fetchNotifications() }

    AlumniPremiumTheme {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppGradientBackground(isDark = isDark)
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Premium Top Bar ──────────────────────────────────────────────
            LuxuryNotifTopBar(
                count = notifications.size,
                isDark = isDark,
                onBack = { navController.popBackStack() }
            )

            // ── Content ──────────────────────────────────────────────────────
            when {
                isLoading && notifications.isEmpty() -> LuxuryNotifLoading(isDark = isDark)
                notifications.isEmpty()              -> LuxuryNotifEmpty(isDark = isDark)
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 16.dp, bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ── Summary chip row ─────────────────────────────────
                        item {
                            NotifSummaryRow(notifications = notifications, isDark = isDark)
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // ── Notification cards ───────────────────────────────
                        itemsIndexed(notifications) { index, item ->
                            NotificationCard(
                                item    = item,
                                index   = index,
                                isDark  = isDark,
                                onClick = {
                                    viewModel.markAsRead(item.id)
                                    handleNotificationClick(item, navController)
                                },
                                onDelete = { viewModel.deleteNotification(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

// ─── TOP BAR ─────────────────────────────────────────────────────────────────
@Composable
private fun LuxuryNotifTopBar(count: Int, isDark: Boolean, onBack: () -> Unit) {
    AppPageHeader(
        title = "NOTIFIKASI",
        subtitle = if (count > 0) "$count pesan masuk" else "Pusat notifikasi",
        isDark = isDark,
        onBack = onBack,
        size = AppPageHeaderSize.Compact,
        rightAction = {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.12f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = GoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Unread count badge
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    )
}

// ─── SUMMARY ROW ─────────────────────────────────────────────────────────────
@Composable
private fun NotifSummaryRow(notifications: List<NotificationItem>, isDark: Boolean) {
    // Count per category
    val countMap = notifications.groupBy { it.source_table ?: "other" }

    val categories = listOf(
        Triple("tagihan_santri",     "Tagihan",     ColorTagihan),
        Triple("pelanggaran_santri", "Pelanggaran", ColorPelanggaran),
        Triple("perizinan_santri",   "Perizinan",   ColorPerizinan),
        Triple("kesehatan_santri",   "Kesehatan",   ColorKesehatan),
        Triple("prestasi_santri",    "Prestasi",    ColorPrestasi),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { (key, label, color) ->
            val c = countMap[key]?.size ?: 0
            if (c > 0) {
                val chipColor = if (isDark) color.copy(alpha = 0.8f) else color
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(chipColor.copy(alpha = if (isDark) 0.12f else 0.08f))
                        .border(1.dp, chipColor.copy(alpha = 0.30f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(chipColor, CircleShape)
                        )
                        Text(
                            text = "$label ($c)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = chipColor
                        )
                    }
                }
            }
        }
    }
}

// ─── NOTIFICATION CARD ────────────────────────────────────────────────────────
@Composable
fun NotificationCard(
    item: NotificationItem,
    index: Int,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    // Per-category theming
    val (icon, accentColor, categoryLabel) = when (item.source_table) {
        "tagihan_santri"     -> Triple(Icons.Default.CreditCard,      ColorTagihan,     "Tagihan")
        "pelanggaran_santri" -> Triple(Icons.Default.Gavel,           ColorPelanggaran, "Pelanggaran")
        "perizinan_santri"   -> Triple(Icons.Default.Assignment,      ColorPerizinan,   "Perizinan")
        "kesehatan_santri"   -> Triple(Icons.Default.MedicalServices, ColorKesehatan,   "Kesehatan")
        "prestasi_santri"    -> Triple(Icons.Default.EmojiEvents,     ColorPrestasi,    "Prestasi")
        "forum_comments",
        "forum_reactions",
        "forum_reports"      -> Triple(Icons.Default.Forum,           GoldPrimary,      "Forum Alumni")
        "chat_messages"      -> Triple(Icons.Default.Chat,            GoldPrimary,      "Chat Alumni")
        else                 -> Triple(Icons.Default.Notifications,   ColorDefault,     "Umum")
    }

    val isUnread = item.status != "read"
    val cardBg = when {
        isDark && index % 2 == 0 -> Color(0xFF1E1E1E)
        isDark -> Color(0xFF222222)
        index % 2 == 0 -> CardSurface
        else -> PureWhite
    }

    val textColorPrimary = if (isDark) PureWhite else TextPrimary
    val textColorSecondary = if (isDark) Color.White.copy(alpha = 0.60f) else TextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isUnread) 4.dp else 1.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = accentColor.copy(alpha = 0.08f),
                spotColor   = accentColor.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                width = if (isUnread) 1.5.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        GoldPrimary.copy(alpha = if (isUnread) 0.35f else 0.16f),
                        GoldPrimary.copy(alpha = if (isUnread) 0.10f else 0.05f),
                        GoldPrimary.copy(alpha = if (isUnread) 0.35f else 0.16f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .drawBehind {
                // Left accent bar — color per category
                drawRoundRect(
                    color = accentColor.copy(alpha = if (isUnread) 1f else 0.45f),
                    topLeft = Offset(0f, size.height * 0.18f),
                    size = Size(3.5f, size.height * 0.64f),
                    cornerRadius = CornerRadius(4f)
                )
                // Unread gold top shimmer line
                if (isUnread) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                GoldPrimary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(40f, 0f),
                        end   = Offset(size.width - 40f, 0f),
                        strokeWidth = 1.5f
                    )
                }
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Icon Badge ───────────────────────────────────────────────────
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.09f))
                        .border(1.dp, accentColor.copy(alpha = 0.28f), CircleShape)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                // Unread dot
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .border(1.5.dp, cardBg, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Text Block ───────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Category pill + date row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.10f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryLabel.uppercase(),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = accentColor
                        )
                    }
                    Text(
                        text = formatNotifDate(item.created_at),
                        fontSize = 9.sp,
                        color = textColorSecondary.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Title
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isUnread) textColorPrimary else textColorPrimary.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.1.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Body
                Text(
                    text = item.body,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = textColorSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // ── Delete Button ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (isDark) Color(0xFFC62828).copy(alpha = 0.12f)
                        else Color(0xFFFFEBEE).copy(alpha = 0.70f)
                    )
                    .border(
                        1.dp,
                        Color(0xFFC62828).copy(alpha = 0.18f),
                        RoundedCornerShape(9.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = Color(0xFFC62828).copy(alpha = if (isDark) 0.85f else 0.75f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

// ─── EMPTY STATE ─────────────────────────────────────────────────────────────
@Composable
private fun LuxuryNotifEmpty(isDark: Boolean) {
    val textColorPrimary = if (isDark) PureWhite else TextPrimary
    val textColorSecondary = if (isDark) Color.White.copy(alpha = 0.60f) else TextSecondary

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Layered rings with bell icon
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(GoldShimmer.copy(alpha = if (isDark) 0.05f else 0.25f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.20f), CircleShape)
                )
                // Inner ring
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GoldShimmer.copy(alpha = if (isDark) 0.08f else 0.40f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.35f), CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.70f),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Belum Ada Notifikasi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Semua notifikasi dari pesantren\nakan muncul di sini",
                fontSize = 12.sp,
                color = textColorSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Decorative gold divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(GoldPrimary.copy(alpha = 0.30f))
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(5.dp)
                        .background(GoldPrimary.copy(alpha = 0.50f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(GoldPrimary.copy(alpha = 0.30f))
                )
            }
        }
    }
}

// ─── LOADING STATE ────────────────────────────────────────────────────────────
@Composable
private fun LuxuryNotifLoading(isDark: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color      = GoldPrimary,
                trackColor = if (isDark) GoldPrimary.copy(alpha = 0.18f) else GoldShimmer,
                strokeWidth = 3.dp,
                modifier   = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text  = "Memuat Notifikasi…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─── NAVIGATION HANDLER (unchanged logic) ────────────────────────────────────
private fun handleNotificationClick(item: NotificationItem, navController: NavController) {
    try {
        val json = item.data.jsonObject
        val type = json["type"]?.jsonPrimitive?.content
        val nis  = json["nis"]?.jsonPrimitive?.content
            ?: json["santri_nis"]?.jsonPrimitive?.content
        val threadId = json["thread_id"]?.jsonPrimitive?.content
        val conversationId = json["conversation_id"]?.jsonPrimitive?.content
        if (type != null) {
            when (type) {
                "tagihan",
                "tagihan_due_reminder",
                "tagihan.payment_installment",
                "tagihan.payment_success",
                "tagihan.due_reminder",
                "tagihan.overdue_reminder" -> nis?.let { navController.navigate(Screen.Keuangan.createRoute(it)) }
                "pelanggaran"-> nis?.let { navController.navigate(Screen.Pelanggaran.createRoute(it)) }
                "hafalan"    -> nis?.let { navController.navigate(Screen.Hafalan.createRoute(it)) }
                "murajaah", "murojaah" -> nis?.let { navController.navigate(Screen.Murajaah.createRoute(it)) }
                "kesehatan"  -> nis?.let { navController.navigate(Screen.Kesehatan.createRoute(it)) }
                "perizinan"  -> nis?.let { navController.navigate(Screen.Perizinan.createRoute(it)) }
                "prestasi_created", "prestasi" -> navController.navigate(Screen.Prestasi.route)
                "forum_comment",
                "forum_reaction",
                "forum_report" -> navController.navigate(Screen.AlumniForum.createRoute(threadId))
                "alumni_chat_message" -> navController.navigate(Screen.AlumniChat.createRoute(conversationId))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ─── DATE FORMATTER ──────────────────────────────────────────────────────────
private fun formatNotifDate(raw: String): String {
    // raw: "2024-09-15T08:30:00" → "15 Sep 2024"
    return try {
        val parts = raw.take(10).split("-")
        val months = listOf(
            "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
        )
        val m = parts[1].toIntOrNull() ?: 0
        "${parts[2]} ${months.getOrNull(m) ?: ""} ${parts[0]}"
    } catch (e: Exception) {
        raw.take(10)
    }
}
