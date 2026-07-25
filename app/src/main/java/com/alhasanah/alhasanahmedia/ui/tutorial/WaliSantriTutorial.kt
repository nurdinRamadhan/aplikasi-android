package com.alhasanah.alhasanahmedia.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alhasanah.alhasanahmedia.ui.tutorial.TutorialColors
import com.alhasanah.alhasanahmedia.showcase.model.ShowcaseMsg
import com.alhasanah.alhasanahmedia.showcase.model.Gravity
import com.alhasanah.alhasanahmedia.showcase.ui.ShowcaseScope
import com.alhasanah.alhasanahmedia.showcase.ui.ShowcaseLayoutDefaults
import com.alhasanah.alhasanahmedia.showcase.model.Arrow
import com.alhasanah.alhasanahmedia.showcase.model.Head
import com.alhasanah.alhasanahmedia.showcase.model.Side

// ═══════════════════════════════════════════════════════════════════════════
//  CompositionLocal untuk memberikan ShowcaseScope ke child composables
// ═══════════════════════════════════════════════════════════════════════════
val LocalShowcaseScope = compositionLocalOf<ShowcaseScope?> { null }

// ═══════════════════════════════════════════════════════════════════════════
//  WaliSantriTutorial — Konstanta & data untuk tutorial wali santri
// ═══════════════════════════════════════════════════════════════════════════

enum class TutorialPhase {
    NONE,
    PHASE_1_STEP_1,
    PHASE_1_STEP_2,
    PHASE_1_STEP_3,
    PHASE_2_STEP_1,
    PHASE_2_STEP_2,
    PHASE_2_STEP_3
}

// ── TutorialOverlayColors — Opacity 40% untuk dark & light mode ──────────────
object TutorialOverlayColors {
    val DarkOverlay = Color.Black.copy(alpha = 0.4f)
    val LightOverlay = Color.Black.copy(alpha = 0.4f)
    val DarkPulse = TutorialColors.DarkTargetCircle
    val LightPulse = TutorialColors.LightTargetCircle
}

fun tutorialMsg(
    text: String,
    isDark: Boolean,
    gravity: Gravity = Gravity.Bottom,
    arrowSide: Side = Side.Top
): ShowcaseMsg {
    val bgColor = if (isDark) TutorialColors.DarkTooltipBackground else TutorialColors.LightTooltipBackground
    val textColor = if (isDark) TutorialColors.DarkTooltipText else TutorialColors.LightTooltipText
    val arrowColor = if (isDark) TutorialColors.GoldBright else TutorialColors.Gold
    return ShowcaseMsg(
        text = text,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        ),
        msgBackground = bgColor,
        roundedCorner = 14.dp,
        gravity = gravity,
        arrow = Arrow(
            targetFrom = arrowSide,
            curved = false,
            head = Head.TRIANGLE,
            headSize = 20f,
            color = arrowColor,
            animSize = true,
            animationDuration = 500
        )
    )
}
