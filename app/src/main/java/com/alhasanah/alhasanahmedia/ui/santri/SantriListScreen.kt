package com.alhasanah.alhasanahmedia.ui.santri

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.navigation.Screen
import com.alhasanah.alhasanahmedia.data.model.SantriModel
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeader
import com.alhasanah.alhasanahmedia.ui.components.AppPageHeaderSize
import org.koin.androidx.compose.koinViewModel

@Composable
fun SantriListScreen(
    navController: NavController,
    viewModel: SantriListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigationState by viewModel.navigationState.collectAsState()

    // Handle navigation automatically
    LaunchedEffect(navigationState) {
        when (val navState = navigationState) {
            is SantriNavigationState.GoToDetail -> {
                // Navigate to detail and pop this list screen from the back stack
                navController.navigate(Screen.SantriDetail.createRoute(navState.santriId)) {
                    popUpTo(Screen.SantriList.route) { inclusive = true }
                }
            }
            else -> { /* Do nothing for Idle or ShowList */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is SantriListUiState.Loading -> {
                CircularProgressIndicator()
            }
            is SantriListUiState.Success -> {
                // Only show the list if the navigation state says so
                if (navigationState is SantriNavigationState.ShowList) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 128.dp)
                    ) {
                        item {
                            AppPageHeader(
                                title = "DAFTAR SANTRI",
                                subtitle = "${state.santriList.size} santri terhubung",
                                isDark = androidx.compose.foundation.isSystemInDarkTheme(),
                                onBack = { navController.popBackStack() },
                                size = AppPageHeaderSize.Standard
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        items(state.santriList) { santri ->
                            SantriListItem(
                                santri = santri,
                                onClick = {
                                    navController.navigate(Screen.SantriDetail.createRoute(santri.id))
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
            is SantriListUiState.Error -> {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun SantriListItem(
    santri: SantriModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = "Santri Icon", modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = santri.namaLengkap, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = "NIS: ${santri.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Go to detail")
        }
    }
}
