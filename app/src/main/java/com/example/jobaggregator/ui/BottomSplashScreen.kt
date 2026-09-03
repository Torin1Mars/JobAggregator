package com.example.jobaggregator.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt


enum class DragStates {
    Collapsed,
    Expanded
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSwipingFieldScreen() {
    val density = LocalDensity.current

    val barHeight = 400.dp
    val decaySpec = rememberSplineBasedDecay<Float>()

    //TODO continuing here

    // 2. Configure thresholds and animation specs
    val state = remember {
        AnchoredDraggableState(
            initialValue = DragStates.Collapsed,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 300),
            decayAnimationSpec = decaySpec
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
        Text("Main Content Area", modifier = Modifier.align(Alignment.Center))

        // 3. The bottom swiping panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Shift the panel up/down as the drag state's offset changes
                .offset {
                    IntOffset(
                        x = 0,
                        y = if (state.offset.isNaN()) 0 else state.offset.roundToInt()
                    )
                }
                // Anchors depend on measured size, so set them once known
                .onSizeChanged { layoutSize ->
                    val dragHeight = layoutSize.height.toFloat()
                    val expandedOffset = dragHeight
                    val collapsedOffset = dragHeight*2+100f

                    state.updateAnchors(
                        DraggableAnchors {
                            DragStates.Expanded at expandedOffset
                            DragStates.Collapsed at collapsedOffset
                        }
                    )
                }
                .anchoredDraggable(state, Orientation.Vertical)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(Color.White)
                .height(barHeight)
        ) {
            // Drag handle indicator
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray, RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Swipe me up or down!",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}