package com.alhasanah.alhasanahmedia.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.alhasanah.alhasanahmedia.ui.tutorial.TutorialPhase

@Composable
fun HomeScreen(
    isLoggedIn: Boolean,
    openDrawer: () -> Unit,
    navController: NavController,
    tutorialPhase: TutorialPhase = TutorialPhase.NONE
) {
    // Scaffold has been moved to HomeContent, we pass all necessary parameters down
    HomeContent(
        isLoggedIn = isLoggedIn,
        openDrawer = openDrawer,
        onNotificationClick = { /* TODO */ },
        navController = navController,
        tutorialPhase = tutorialPhase
    )
}
