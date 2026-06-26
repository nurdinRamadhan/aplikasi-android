package com.alhasanah.alhasanahmedia.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.ui.components.AppSolidBackground
import com.alhasanah.alhasanahmedia.util.isAppInDarkTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val loginState by authViewModel.loginState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val isDark = isAppInDarkTheme()

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            navController.navigate(Screen.Home.route) {
                launchSingleTop = true
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AppSolidBackground(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(top = 34.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ── Branding Section ─────────────────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))
            Box(contentAlignment = Alignment.Center) {
                // Subtle Glow Behind Logo
                Box(
                    modifier = Modifier
                        .size(154.dp)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(1.dp, primaryColor.copy(alpha = 0.30f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "AL-HASANAH",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.sp,
                    color = if (isDark) primaryColor.copy(alpha = 0.94f) else Color(0xFF8B6914)
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MEDIA",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = primaryColor.copy(alpha = 0.82f),
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Black
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Portal akses wali santri dan alumni",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))
            LoginHeroDivider(isDark = isDark)

            Spacer(modifier = Modifier.height(42.dp))

            // ── Input Fields Section ──────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LoginTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Alamat Email",
                    icon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                LoginTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Kata Sandi",
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Text(
                    text = "Login sebagai admin?",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable {
                            navController.navigate(Screen.AdminPanel.createRoute())
                        }
                        .padding(top = 2.dp)
                )
            }

            // ── Error Message ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = loginState is LoginState.Error,
                enter = fadeIn()
            ) {
                val message = (loginState as? LoginState.Error)?.message
                    ?: "Login gagal. Coba lagi."
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Premium CTA Button ───────────────────────────────────────────
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (isPressed) 0.97f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessHigh),
                label = "btnScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(btnScale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFAA7C1F), Color(0xFFD4A017), Color(0xFFE8C55A))
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = loginState !is LoginState.Loading
                    ) {
                        authViewModel.resetLoginState()
                        authViewModel.signIn(email, password)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF1F2326),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "MASUK KE AKUN",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF1F2326),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "ALHASANAH MEDIA • v1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Light
                )
            )
        }
    }
}

@Composable
private fun LoginHeroDivider(isDark: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val lineColor = if (isDark) Color.White.copy(0.08f) else MaterialTheme.colorScheme.outlineVariant
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(0.58f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, lineColor))))
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.size(4.dp))
        Box(modifier = Modifier.size(8.dp).background(primary.copy(0.82f), CircleShape))
        Spacer(modifier = Modifier.size(4.dp))
        Box(modifier = Modifier.size(5.dp).background(primary.copy(0.60f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(lineColor, Color.Transparent))))
    }
}

@Composable
fun LoginBackgroundPattern(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 60.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height),
                strokeWidth = 0.5.dp.toPx()
            )
            x += step
        }
    }
}

@Composable
fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { 
            Text(
                label, 
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            ) 
        },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { 
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            ) 
        },
        trailingIcon = {
            if (isPassword) {
                IconButton(onClick = onPasswordToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f),
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    )
}
