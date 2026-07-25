package com.alhasanah.alhasanahmedia.showcase.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.alhasanah.alhasanahmedia.showcase.domain.Level
import com.alhasanah.alhasanahmedia.showcase.domain.ShowcaseEventListener
import com.alhasanah.alhasanahmedia.showcase.domain.usecase.validateInitIndex
import com.alhasanah.alhasanahmedia.showcase.model.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max

/**
 *     Copyright 2023 Taha Ben Ashur (tahaak67)
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 * Created by Taha Ben Ashur (https://github.com/tahaak67) on 1,August,2022
 */

private const val TAG = "ShowcaseLayout "
private const val INDEX_RESET_DELAY = 250L

/**
 * ShowcaseLayout
 *
 * @param isShowcasing to determine if showcase is starting or not.
 * @param isDarkLayout if true the showcase view will be white instead of black.
 * @param initIndex the initial value of counter, set this to 1 if you don't want a greeting screen before showcasing target.
 * @param animationDuration total animation time taken when switching from current to next target in milliseconds.
 * @param onFinish what happens when all items are showcased.
 * @param greeting greeting message to be shown before showcasing the first composable, leave [initIndex] at 0 if you want to use this.
 * @param lineThickness thickness of the arrow line in dp.
 * @param targetShape the shape of the target highlight (RECTANGLE, CIRCLE, or ROUNDED_RECTANGLE).
 * @param cornerRadius the corner radius for the ROUNDED_RECTANGLE shape in dp.
 **/

@Composable
fun ShowcaseLayout(
    isShowcasing: Boolean,
    isDarkLayout: Boolean = false,
    initIndex: Int = 0,
    animationDuration: Int = 1000,
    onFinish: () -> Unit,
    greeting: ShowcaseMsg? = null,
    lineThickness: Dp = 5.dp,
    targetShape: TargetShape = TargetShape.RECTANGLE,
    cornerRadius: Dp = 16.dp,
    content: @Composable ShowcaseScope.() -> Unit
) {
    val validatedInitIndex = remember(initIndex, greeting) { validateInitIndex(initIndex, greeting) }
    var currentIndex by remember {
        mutableIntStateOf(validatedInitIndex)
    }
    val currentContent by rememberUpdatedState(content)
    val resetDelay by derivedStateOf { animationDuration.toLong() + INDEX_RESET_DELAY }
    val scope = ShowcaseScopeImpl(greeting)
    scope.currentContent()

    var singleGreetingMsg by remember { mutableStateOf<ShowcaseMsg?>(null) }
    val showcaseItem = scope.showcaseActionFlow.collectAsState()
    val showCasingItem by remember {
        derivedStateOf {
            if (showcaseItem.value != null) {
                scope.showcaseEventListener?.onEvent(
                    Level.DEBUG,
                    TAG + "showcase single item index: ${showcaseItem.value}"
                )
                currentIndex = showcaseItem.value ?: validatedInitIndex
                true
            } else {
                false
            }
        }
    }
    val singleGreeting = scope.greetingActionFlow.collectAsState()
    val isSingleGreeting by remember {
        derivedStateOf {
            if (singleGreeting.value != null) {
                scope.showcaseEventListener?.onEvent(
                    Level.DEBUG,
                    TAG + "showcase single greeting: ${singleGreeting.value?.text}"
                )
                singleGreetingMsg = singleGreeting.value
                currentIndex = validatedInitIndex
                true
            } else {
                false
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val coroutineScope = rememberCoroutineScope()
        if (isShowcasing || showCasingItem || isSingleGreeting) {
            // FIX: Use remember + LaunchedEffect to skip animation on first frame
            val targetPosition = scope.getPositionFor(currentIndex)
            val targetSize = scope.getSizeFor(currentIndex)
            var isFirstFrame by remember { mutableStateOf(true) }
            val offset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
            val itemSize = remember { Animatable(Size(0f, 0f), Size.VectorConverter) }
            
            LaunchedEffect(key1 = targetPosition, key2 = targetSize) {
                if (isFirstFrame) {
                    // Skip animation on first frame - snap directly to position
                    offset.snapTo(targetPosition)
                    itemSize.snapTo(targetSize)
                    isFirstFrame = false
                } else {
                    // Animate subsequent changes
                    launch {
                        offset.animateTo(targetPosition, tween(animationDuration))
                    }
                    launch {
                        itemSize.animateTo(targetSize, tween(animationDuration))
                    }
                }
            }
            val offsetValue by offset.asState()
            val itemSizeValue by itemSize.asState()
            var message by remember {
                mutableStateOf(scope.getMessageFor(currentIndex))
            }
            val pathPortion = remember {
                Animatable(initialValue = 0f)
            }

            var isArrowDelayOver by remember { mutableStateOf(false) }
            val shouldDrawArrow = (message?.arrow != null && isArrowDelayOver)
            val arrowColor = message?.arrow?.color ?: Color.White
            val density = LocalDensity.current
            var arrowAnimDuration by remember { mutableStateOf(message?.arrow?.animationDuration) }
            val animMsgTextAlpha = remember { Animatable(0f) }
            val animMsgAlpha = remember { Animatable(0f) }
            val animArrow = remember { Animatable(0f) }
            val animArrowHead = remember { Animatable(0f) }
            val canvasAlpha = remember { Animatable(0f) }

            // FIX: Animate msgAlpha when message becomes available (not just on index change)
            LaunchedEffect(key1 = message) {
                message?.let { msg ->
                    // Immediately show message background
                    if (msg.msgBackground != null) {
                        animMsgAlpha.snapTo(1f)
                    }
                    animMsgTextAlpha.snapTo(1f)
                }
            }

            /** to animate canvas alpha and current arrow line */
            LaunchedEffect(key1 = currentIndex) {
                if (currentIndex == validatedInitIndex || showCasingItem || isSingleGreeting) {
                    canvasAlpha.animateTo(
                        1f,
                        animationSpec = tween(durationMillis = animationDuration / 2, easing = FastOutSlowInEasing)
                    )
                }
                message = scope.getMessageFor(currentIndex)
                arrowAnimDuration = message?.arrow?.animationDuration
                isArrowDelayOver = false
                if (message?.arrow != null) {
                    // FIX: Reduce delay for faster arrow display
                    delay(200L)
                    isArrowDelayOver = true
                    launch {
                        message?.arrow?.let { arrow ->
                            pathPortion.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = arrowAnimDuration
                                        ?: arrow.animationDuration
                                )
                            )
                        }
                    }
                    /* animate the arrow */
                    launch {
                        message?.arrow?.let { arrow ->
                            /** show the arrow if anim is false */
                            if (!arrow.animSize) {
                                animArrowHead.snapTo(arrow.headSize)
                            }
                            /** move the arrow */
                            animArrow.animateTo(
                                1f,
                                tween(
                                    durationMillis = arrowAnimDuration
                                        ?: arrow.animationDuration
                                )
                            )
                            /** animate the size of the arrow */
                            if (arrow.animSize) {
                                animArrowHead.animateTo(
                                    arrow.headSize,
                                    tween(
                                        durationMillis = arrowAnimDuration
                                            ?: arrow.animationDuration
                                    )
                                )
                            }
                        }
                    }
                }
                if (currentIndex == validatedInitIndex) {
                    if (isSingleGreeting) {
                        message = singleGreetingMsg
                    }
                    message?.let { msg ->
                        if (msg.msgBackground != null)
                        animMsgAlpha.animateTo(1f, tween(msg.enterAnim.duration))
                        animMsgTextAlpha.animateTo(1f, tween(msg.enterAnim.duration))
                    }
                } else {
                    scope.showcaseEventListener?.onEvent(
                        Level.VERBOSE,
                        TAG + "index:$currentIndex enterAnim: ${message?.enterAnim}"
                    )
                    message?.let { msg ->
                        when (msg.enterAnim) {
                            is MsgAnimation.FadeInOut -> {
                                val duration = msg.enterAnim.duration
                                if (msg.msgBackground != null)
                                animMsgAlpha.animateTo(1f, tween(duration))
                                animMsgTextAlpha.animateTo(1f, tween(duration))
                            }

                            is MsgAnimation.None -> {
                                animMsgAlpha.snapTo(1f)
                                animMsgTextAlpha.snapTo(1f)
                            }

                        }
                    }
                }
            }

            val textMeasurer = rememberTextMeasurer()
            scope.showcaseEventListener?.onEvent(
                Level.VERBOSE,
                TAG + "index: $currentIndex offset :$offset"
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTag = "canvas" }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            /** detect taps on the screen */
                            coroutineScope.launch {

                                /** hide current arrow */
                                arrowAnimDuration?.let { duration ->
                                    if (message?.arrow?.animSize == true) {
                                        animArrowHead.animateTo(0f, tween(duration / 2))
                                    }
                                    launch {
                                        animArrow.animateTo(0f, tween(duration / 2))
                                    }
                                    // subtracting Float.MIN_VALUE here to avoid a tiny part of the path left on IOS and Desktop
                                    pathPortion.animateTo(
                                        0f - Float.MIN_VALUE,
                                        tween(duration / 2)
                                    )
                                }
                                message?.let { msg ->
                                    scope.showcaseEventListener?.onEvent(
                                        Level.VERBOSE,
                                        TAG + "index:$currentIndex exitAnim: ${msg.exitAnim}"
                                    )
                                    scope.showcaseEventListener?.onEvent(
                                        Level.VERBOSE,
                                        TAG + "index:$currentIndex msg: ${message?.text}"
                                    )
                                    when (msg.exitAnim) {
                                        is MsgAnimation.FadeInOut -> {
                                            val duration = msg.enterAnim.duration
                                            animMsgTextAlpha.animateTo(0f, tween(duration))
                                            if (msg.msgBackground != null)
                                            animMsgAlpha.animateTo(0f, tween(duration))
                                        }

                                        is MsgAnimation.None -> {
                                            animMsgTextAlpha.snapTo(0f)
                                            animMsgAlpha.snapTo(0f)
                                        }
                                    }
                                }
                                if (showCasingItem) {
                                    canvasAlpha.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = animationDuration / 2, easing = FastOutSlowInEasing)
                                    )
                                    scope.showcaseItemFinished()
                                    currentIndex = validatedInitIndex
                                    return@launch
                                }
                                if (isSingleGreeting) {
                                    canvasAlpha.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = animationDuration / 2, easing = FastOutSlowInEasing)
                                    )
                                    scope.showGreetingFinished()
                                    currentIndex = validatedInitIndex
                                    return@launch
                                }
                                if (currentIndex + 1 < scope.getHashMapSize()) {
                                    scope.showcaseEventListener?.onEvent(
                                        Level.INFO,
                                        TAG + "moving to index ${currentIndex + 1}"
                                    )
                                    /** move to next item */
                                    currentIndex++
                                } else {
                                    /** showcase finished */
                                    scope.showcaseEventListener?.onEvent(
                                        Level.INFO,
                                        TAG + "finished"
                                    )
                                    canvasAlpha.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = animationDuration / 2, easing = FastOutSlowInEasing)
                                    )
                                    onFinish()
                                    delay(resetDelay)
                                    currentIndex = validatedInitIndex
                                }
                                isArrowDelayOver = false
                            }
                            scope.showcaseEventListener?.onEvent(
                                Level.VERBOSE,
                                TAG + "tapped here $it"
                            )
                        }
                    },
                onDraw = {

                    // FIX: No overlay dimming - just target cutout + message
                    if (currentIndex == 0 || isSingleGreeting) {
                        // Greeting - no overlay needed
                        // (message will be drawn separately)
                    } else {
                        when (targetShape) {
                            TargetShape.RECTANGLE -> {
                                // No overlay - just skip
                            }

                            TargetShape.CIRCLE -> {
                                // No overlay - just skip
                            }

                            TargetShape.ROUNDED_RECTANGLE -> {
                                // No overlay - just skip
                            }
                        }
                    }

                    // Calculate the center and radius of the circle for target
                    val centerX = offsetValue.x + itemSizeValue.width / 2
                    val centerY = offsetValue.y + itemSizeValue.height / 2
                    val radius = maxOf(itemSizeValue.width, itemSizeValue.height) / 2

                    message?.let { msg ->
                        /**
                        Create a measurer for the message with limited constraints and a 'Visible'
                        overflow to make the text go to a new line if the screen width doesn't fit
                        one line.
                         */
                        val textResult = textMeasurer.measure(
                            msg.text,
                            style = msg.textStyle,
                            overflow = TextOverflow.Visible,
                            constraints = Constraints(0, max(1, constraints.maxWidth - 90))
                        )

                        /** Determine if message will be shown on top or below target */
                        val yOffset =
                            if (currentIndex == 0 || isSingleGreeting) (size.height / 2) else with(density) {
                                val currentItemYPosition = scope.getPositionFor(currentIndex).y
                                val currentItemHeight = scope.getSizeFor(currentIndex).height

                                // Calculate additional offset for circle shape
                                val additionalOffset = if (targetShape == TargetShape.CIRCLE) {
                                    // Use the radius of the circle as additional offset
                                    val currentItemWidth = scope.getSizeFor(currentIndex).width
                                    val radius = maxOf(currentItemWidth, currentItemHeight) / 2
                                    // Add some extra margin (50px) to ensure enough space for the arrow
                                    radius + 50
                                } else {
                                    0f
                                }

                                val baseOffset = 230 + additionalOffset

                                when (msg.gravity) {
                                    Gravity.Top -> {
                                        currentItemYPosition - baseOffset
                                    }

                                    Gravity.Bottom -> {
                                        currentItemYPosition + currentItemHeight + baseOffset
                                    }

                                    Gravity.Auto -> {
                                        val topPosition =
                                            currentItemYPosition - baseOffset
                                        if (topPosition < 0) {
                                            scope.showcaseEventListener?.onEvent(
                                                Level.INFO,
                                                TAG + "index: $currentIndex Not enough space on top show msg on bottom"
                                            )
                                            currentItemYPosition + currentItemHeight + baseOffset
                                        } else {
                                            scope.showcaseEventListener?.onEvent(
                                                Level.INFO,
                                                TAG + "index: $currentIndex message can be shown on top"
                                            )
                                            topPosition
                                        }
                                    }
                                }
                            }
                        val halfWidth = (size.width / 2)
                        val messageWidthHalf = textResult.size.width / 2

                        /**
                        Determine the horizontal alignment of the message, we try to center it
                        to the target but if the target is on the edge of the screen the message
                        will get cut off, if that's the case we align the message Start or End to
                        the target Start or End as appropriate
                         */
                        val xOffset = if (currentIndex == 0 || isSingleGreeting || msg.arrow?.curved == true) {
                            halfWidth - messageWidthHalf
                        } else {
                            val currentItemXPosition = scope.getPositionFor(currentIndex).x
                            val currentItemWidth = scope.getSizeFor(currentIndex).width
                            val currentItemHeight = scope.getSizeFor(currentIndex).height

                            // Calculate additional horizontal offset for circle shape
                            val additionalHorizontalOffset = if (targetShape == TargetShape.CIRCLE) {
                                // Use the radius of the circle as additional offset
                                val radius = maxOf(currentItemWidth, currentItemHeight) / 2
                                // Add some extra margin to ensure enough space for the arrow
                                radius * 0.3f // 30% of radius as extra margin
                            } else {
                                0f
                            }

                            val currentItemXMiddlePoint =
                                currentItemXPosition + (currentItemWidth / 2)
                            when {
                                (currentItemXMiddlePoint < halfWidth) -> {
                                    scope.showcaseEventListener?.onEvent(
                                        Level.INFO,
                                        TAG + "index: $currentIndex layout on start half"
                                    )
                                    if ((currentItemXMiddlePoint - messageWidthHalf) < 0) {
                                        currentItemXPosition
                                    } else {
                                        // For left side, move message further left
                                        if (msg.arrow?.targetFrom == Side.Right && targetShape == TargetShape.CIRCLE) {
                                            currentItemXMiddlePoint - messageWidthHalf - additionalHorizontalOffset
                                        } else {
                                            currentItemXMiddlePoint - messageWidthHalf
                                        }
                                    }
                                }

                                (currentItemXMiddlePoint == halfWidth) -> {
                                    scope.showcaseEventListener?.onEvent(
                                        Level.INFO,
                                        TAG + "index: $currentIndex layout in middle"
                                    )
                                    currentItemXMiddlePoint - messageWidthHalf
                                }

                                else -> {
                                    scope.showcaseEventListener?.onEvent(
                                        Level.INFO,
                                        TAG + "index: $currentIndex layout on end half"
                                    )
                                    if (currentItemXMiddlePoint + messageWidthHalf > size.width) {
                                        currentItemXPosition + currentItemWidth - textResult.size.width
                                    } else {
                                        // For right side, move message further right
                                        if (msg.arrow?.targetFrom == Side.Left && targetShape == TargetShape.CIRCLE) {
                                            currentItemXMiddlePoint - messageWidthHalf + additionalHorizontalOffset
                                        } else {
                                            currentItemXMiddlePoint - messageWidthHalf
                                        }
                                    }
                                }
                            }
                        }

                        val textOffset = Offset(xOffset, yOffset)
                        val cardOffset = Offset(textOffset.x - 18, textOffset.y - 18)
                        val cardSize = IntSize(
                            textResult.size.width + 36,
                            textResult.size.height + 36
                        ).toSize()

                        // Draw the message card
                        if (msg.roundedCorner == 0.dp) {
                            drawRect(
                                msg.msgBackground ?: Color.Transparent,
                                topLeft = cardOffset,
                                size = cardSize,
                                alpha = animMsgAlpha.value
                            )
                        } else {
                            drawRoundRect(
                                msg.msgBackground ?: Color.Transparent,
                                topLeft = cardOffset,
                                size = cardSize,
                                cornerRadius = CornerRadius(msg.roundedCorner.value),
                                alpha = animMsgAlpha.value
                            )
                        }

                        // Draw the arrow if needed
                        val hasArrowHead = msg.arrow?.head != null
                        val arrowHeadMargin = (msg.arrow?.headSize ?: Arrow().headSize) + 25

                        if (currentIndex > 0 && shouldDrawArrow) {
                            /** draw arrow line */
                            val arrowPath = Path().apply {
                                if (msg.arrow?.curved == true) {
                                    // Calculate card center
                                    val cardCenterX = cardOffset.x + cardSize.width / 2
                                    val cardCenterY = cardOffset.y + cardSize.height / 2

                                    // Start from the message card based on targetFrom direction
                                    when (msg.arrow?.targetFrom) {
                                        Side.Top -> {
                                            // Start from bottom center of the message card
                                            moveTo(
                                                cardCenterX,
                                                cardOffset.y + cardSize.height
                                            )
                                        }

                                        Side.Bottom -> {
                                            // Start from top center of the message card
                                            moveTo(
                                                cardCenterX,
                                                cardOffset.y
                                            )
                                        }

                                        Side.Left -> {
                                            // Start from right center of the message card
                                            moveTo(
                                                cardOffset.x + cardSize.width,
                                                cardCenterY
                                            )
                                        }

                                        Side.Right -> {
                                            // Start from left center of the message card
                                            moveTo(
                                                cardOffset.x,
                                                cardCenterY
                                            )
                                        }

                                        else -> {
                                            // Default to bottom center if targetFrom is not specified
                                            moveTo(
                                                cardCenterX,
                                                cardOffset.y + cardSize.height
                                            )
                                        }
                                    }

                                    val xPoint =
                                        if ((offsetValue.x + itemSizeValue.width + 80) > size.width) {
                                            offsetValue.x - 80
                                        } else {
                                            (offsetValue.x + itemSizeValue.width + 50)
                                        }
                                    quadraticTo(
                                        size.width / 2, offsetValue.y + itemSizeValue.height + 0,
                                        xPoint,
                                        offsetValue.y + (itemSizeValue.height / 2)
                                    )
                                } else {
                                    // Calculate card center
                                    val cardCenterX = cardOffset.x + cardSize.width / 2
                                    val cardCenterY = cardOffset.y + cardSize.height / 2

                                    when (msg.arrow?.targetFrom) {
                                        Side.Top -> {
                                            // Start from bottom center of the message card
                                            if (targetShape == TargetShape.CIRCLE) {
                                                moveTo(
                                                    cardCenterX,
                                                    cardOffset.y + cardSize.height
                                                )
                                                // For circle, point to the top edge of the circle
                                                lineTo(
                                                    centerX,
                                                    if (hasArrowHead) centerY - radius - arrowHeadMargin else centerY - radius
                                                )
                                            } else {
                                                moveTo(
                                                    offsetValue.x + (itemSizeValue.width / 2),
                                                    offsetValue.y - 180
                                                )
                                                lineTo(
                                                    offsetValue.x + (itemSizeValue.width / 2),
                                                    if (hasArrowHead) offsetValue.y - arrowHeadMargin else offsetValue.y
                                                )
                                            }
                                        }

                                        Side.Bottom -> {
                                            // Start from top center of the message card
                                            if (targetShape == TargetShape.CIRCLE) {
                                                moveTo(
                                                    cardCenterX,
                                                    cardOffset.y
                                                )
                                                // For circle, point to the bottom edge of the circle
                                                lineTo(
                                                    centerX,
                                                    if (hasArrowHead) centerY + radius + arrowHeadMargin else centerY + radius
                                                )
                                            } else {
                                                moveTo(
                                                    offsetValue.x + (itemSizeValue.width / 2),
                                                    offsetValue.y + (itemSizeValue.height + 200)
                                                )
                                                lineTo(
                                                    offsetValue.x + (itemSizeValue.width / 2),
                                                    if (hasArrowHead) offsetValue.y + itemSizeValue.height + arrowHeadMargin else offsetValue.y + itemSizeValue.height
                                                )
                                            }
                                        }

                                        Side.Left -> {
                                            // Start from right center of the message card
                                            if (targetShape == TargetShape.CIRCLE) {
                                                moveTo(
                                                    cardOffset.x + cardSize.width,
                                                    cardCenterY
                                                )
                                                // For circle, point to the left edge of the circle
                                                lineTo(
                                                    if (hasArrowHead) centerX - radius - arrowHeadMargin else centerX - radius,
                                                    centerY
                                                )
                                            } else {
                                                moveTo(
                                                    offsetValue.x - 200,
                                                    offsetValue.y + (itemSizeValue.height / 2)
                                                )
                                                lineTo(
                                                    if (hasArrowHead) offsetValue.x - arrowHeadMargin else offsetValue.x,
                                                    offsetValue.y + (itemSizeValue.height / 2)
                                                )
                                            }
                                        }

                                        Side.Right -> {
                                            // Start from left center of the message card
                                            if (targetShape == TargetShape.CIRCLE) {
                                                moveTo(
                                                    cardOffset.x,
                                                    cardCenterY
                                                )
                                                // For circle, point to the right edge of the circle
                                                lineTo(
                                                    if (hasArrowHead) centerX + radius + arrowHeadMargin else centerX + radius,
                                                    centerY
                                                )
                                            } else {
                                                moveTo(
                                                    offsetValue.x + (itemSizeValue.width + 200),
                                                    offsetValue.y + (itemSizeValue.height / 2)
                                                )
                                                lineTo(
                                                    if (hasArrowHead) offsetValue.x + itemSizeValue.width + arrowHeadMargin else offsetValue.x + itemSizeValue.width,
                                                    offsetValue.y + (itemSizeValue.height / 2)
                                                )
                                            }
                                        }

                                        null -> Unit
                                    }
                                }
                            }

                            val outPath = Path()
                            val pos = FloatArray(2)
                            val tan = FloatArray(2)
                            PathMeasure().apply {
                                setPath(arrowPath, false)
                                getSegment(0f, pathPortion.value * length, outPath, true)
                                getPosition(pathPortion.value * length).apply {
                                    pos[0] = x
                                    pos[1] = y
                                }
                                getTangent(pathPortion.value * length).apply {
                                    tan[0] = x
                                    tan[1] = y
                                }
                                scope.showcaseEventListener?.onEvent(
                                    Level.VERBOSE,
                                    TAG + "pos:${pos} tan:${tan}"
                                )
                            }
                            drawPath(
                                path = outPath,
                                color = arrowColor,
                                style = Stroke(width = lineThickness.toPx(), cap = StrokeCap.Round)
                            )

                            /** draw the arrow head (and rotate if needed) */
                            val arrowSize = animArrowHead.value
                            val x = pos[0]
                            val y = pos[1]
                            val degrees = -atan2(tan[0], tan[1]) * (180f / PI.toFloat()) - 180f
                            scope.showcaseEventListener?.onEvent(
                                Level.VERBOSE,
                                TAG + "max canvas: x:${size.width} y:${size.height}"
                            )
                            when (msg.arrow?.head) {
                                Head.CIRCLE -> {
                                    drawCircle(
                                        center = Offset(x, y),
                                        color = arrowColor,
                                        alpha = animArrow.value,
                                        radius = arrowSize
                                    )

                                }

                                Head.TRIANGLE -> {
                                    rotate(degrees = degrees, pivot = Offset(x, y)) {
                                        drawPath(
                                            path = Path().apply {
                                                moveTo(x, y - arrowSize)
                                                lineTo(x - arrowSize, y + arrowSize)
                                                lineTo(x + arrowSize, y + arrowSize)
                                                close()
                                            },
                                            color = arrowColor,
                                            alpha = animArrow.value
                                        )
                                    }
                                }

                                Head.SQUARE -> {
                                    drawRect(
                                        topLeft = Offset(
                                            x - arrowSize.div(2),
                                            y - arrowSize.div(2)
                                        ),
                                        color = arrowColor,
                                        alpha = animArrow.value,
                                        size = Size(arrowSize, arrowSize)
                                    )
                                }

                                Head.ROUND_SQUARE -> {
                                    val radius = arrowSize.div(4)
                                    drawRoundRect(
                                        topLeft = Offset(
                                            x - arrowSize.div(2),
                                            y - arrowSize.div(2)
                                        ),
                                        color = arrowColor,
                                        alpha = animArrow.value,
                                        size = Size(arrowSize, arrowSize),
                                        cornerRadius = CornerRadius(radius, radius)
                                    )
                                }

                                null -> Unit

                            }
                        }

                        // Draw the message text
                        drawText(
                            textResult,
                            topLeft = textOffset,
                            alpha = animMsgTextAlpha.value
                        )
                    }
                }
            )
            scope.showcaseEventListener?.onEvent(
                Level.VERBOSE,
                TAG + "calc: ${offsetValue.y + itemSizeValue.height - (maxHeight.value / 2)}"
            )
        }

    }
}

class ShowcaseScopeImpl(greeting: ShowcaseMsg?) : ShowcaseScope {
    private val showcaseDataHashMap = HashMap<Int, ShowcaseData>()
    override var showcaseEventListener: ShowcaseEventListener? = null
    private val _showcaseActionFlow = MutableStateFlow<Int?>(null)
    val showcaseActionFlow = _showcaseActionFlow.asStateFlow()
    private val _greetingActionFlow = MutableStateFlow<ShowcaseMsg?>(null)
    val greetingActionFlow = _greetingActionFlow.asStateFlow()

    @Composable
    override fun Showcase(
        index: Int,
        message: ShowcaseMsg?,
        itemContent: @Composable () -> Unit
    ) {
        require(index >= 1) { "Index must be 1 or greater" }
        Box(modifier = Modifier.onGloballyPositioned {
            showcaseDataHashMap[index] = ShowcaseData(it.size, it.positionInRoot(), message, it)

            showcaseEventListener?.onEvent(Level.VERBOSE, TAG + "Index: $index")
            showcaseEventListener?.onEvent(
                Level.VERBOSE,
                TAG + "size: ${it.size} position: ${it.positionInRoot()}"
            )
            showcaseEventListener?.onEvent(
                Level.VERBOSE,
                TAG + "showcase map: $showcaseDataHashMap"
            )

        }) {

            itemContent()
        }
    }

    override fun Modifier.showcase(index: Int, message: ShowcaseMsg?): Modifier {
        require(index >= 1) { "Index must be 1 or greater" }
        return this.then(
            onGloballyPositioned {
                if (it.isAttached) {
                    showcaseDataHashMap[index] =
                        ShowcaseData(it.size, it.positionInRoot(), message, it)
                    showcaseEventListener?.onEvent(Level.VERBOSE, TAG + "Index: $index")
                    showcaseEventListener?.onEvent(
                        Level.VERBOSE,
                        TAG + "size: ${it.size} position: ${it.positionInRoot()}"
                    )
                    showcaseEventListener?.onEvent(
                        Level.VERBOSE,
                        TAG + "showcase map: $showcaseDataHashMap"
                    )
                }
            }
        )
    }

    override fun registerEventListener(eventListener: ShowcaseEventListener) {
        this.showcaseEventListener = eventListener
    }

    override suspend fun showcaseItem(index: Int) {
        showcaseEventListener?.onEvent(Level.DEBUG, TAG + "showcase item $index")
        _showcaseActionFlow.emit(index)
    }

    suspend fun showcaseItemFinished() {
        showcaseEventListener?.onEvent(Level.DEBUG, TAG + "showcase item finished")
        _showcaseActionFlow.emit(null)
    }

    override suspend fun showGreeting(message: ShowcaseMsg) {
        _greetingActionFlow.emit(message)
        showcaseEventListener?.onEvent(
            Level.DEBUG,
            TAG + "greeting ${message.text.substring(0..10)}"
        )
    }

    suspend fun showGreetingFinished() {
        _greetingActionFlow.emit(null)
        showcaseEventListener?.onEvent(
            Level.DEBUG,
            TAG + "greeting show finished"
        )
    }

    init {
        showcaseDataHashMap[0] = ShowcaseData(IntSize(0, 0), Offset(0f, 0f), greeting)
    }

    fun getSizeFor(index: Int): Size {
        val size = showcaseDataHashMap[index]?.size?.toSize() ?: Size(0f, 0f)
        showcaseEventListener?.onEvent(Level.VERBOSE, TAG + "showcase map size: $size")
        return size
    }

    fun getPositionFor(index: Int): Offset {
        if (index == 0) {
            return showcaseDataHashMap[1]?.position ?: Offset(0f, 0f)
        }
        val p = showcaseDataHashMap[index]?.position ?: Offset(0f, 0f)
        return p
    }

    fun getHashMapSize(): Int {
        return showcaseDataHashMap.size
    }

    fun getMessageFor(currentIndex: Int): ShowcaseMsg? {
        return showcaseDataHashMap[currentIndex]?.message
    }

}
