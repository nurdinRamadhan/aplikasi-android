package com.alhasanah.alhasanahmedia.ui.rag

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.data.model.RagSource
import com.alhasanah.alhasanahmedia.data.model.WaliChild
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagChatScreen(
    navController: NavController,
    viewModel: RagChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val authState by viewModel.authenticationState.collectAsState()
    val isLoggedIn = authState is AuthenticationState.Authenticated
    val isDark = isAppInDarkTheme()
    val listState = rememberLazyListState()
    val leadingItems =
        (if (isLoggedIn && state.children.size > 1) 1 else 0) +
        (if (state.errorMessage != null) 1 else 0)

    LaunchedEffect(state.messages.size, state.isLoading) {
        val lastIndex = leadingItems + state.messages.lastIndex + if (state.isLoading) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            ChatInputBar(
                input = state.input,
                isLoading = state.isLoading,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppSolidBackground(isDark = isDark)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isLoggedIn && state.children.size > 1) {
                    item {
                        ChildSelector(
                            children = state.children,
                            selectedChildRef = state.selectedChildRef,
                            onChildSelected = viewModel::onChildSelected
                        )
                    }
                }

                state.errorMessage?.let { message ->
                    item { ErrorNotice(message) }
                }

                if (state.messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            EmptyChat()
                        }
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        ChatBubble(
                            message = message,
                            onLoginClick = { navController.navigate(Screen.Login.route) }
                        )
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            LoadingBubble()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAvatar(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
private fun PremiumChatPattern() {
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.035f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gap = 42.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += gap
        }
    }
}

@Composable
private fun ErrorNotice(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun ChildSelector(
    children: List<WaliChild>,
    selectedChildRef: String?,
    onChildSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        children.forEach { child ->
            val selected = child.childRef == selectedChildRef
            AssistChip(
                onClick = { onChildSelected(child.childRef) },
                label = {
                    Text(
                        text = listOfNotNull(child.nama, child.kelas).joinToString(" - ").ifBlank { "Santri" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                )
            )
        }
    }
}

@Composable
private fun EmptyChat() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 38.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AiAvatar(size = 68.dp)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Ada yang bisa saya bantu?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = "Ketik pertanyaan Anda di bawah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onLoginClick: () -> Unit
) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            AiAvatar(size = 28.dp)
            Spacer(Modifier.width(7.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.88f)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 5.dp,
                    bottomEnd = if (isUser) 5.dp else 18.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                },
                border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                    if (message.showLoginAction) {
                        Button(onClick = onLoginClick) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Login")
                        }
                    }
                }
            }

            if (!isUser && message.sources.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.sources.take(3).forEach { source ->
                        SourceCard(source)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(source: RagSource) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Text(
            text = "Sumber: ${source.title.ifBlank { "Dokumen Al-Hasanah" }}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        AiAvatar(size = 28.dp)
        Spacer(Modifier.width(7.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Mengetik...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pesan") },
                minLines = 1,
                maxLines = 4,
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            )
            IconButton(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (input.isNotBlank() && !isLoading) {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Kirim",
                    tint = if (input.isNotBlank() && !isLoading) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
