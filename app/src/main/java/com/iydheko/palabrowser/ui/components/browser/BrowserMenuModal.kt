package com.iydheko.palabrowser.ui.components.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun BrowserMenuModal(
        showMenu: Boolean,
        rememberLastPage: Boolean,
        onRememberLastPageChanged: (Boolean) -> Unit,
        onClose: () -> Unit,
        navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }
    val dismissThreshold = with(LocalDensity.current) { 150.dp.toPx() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showMenu) {
        if (showMenu) {
            offsetY.snapTo(0f)
            focusRequester.requestFocus()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = showMenu, enter = fadeIn(), exit = fadeOut()) {
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) }
            )
        }

        AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Column(
                        modifier =
                                Modifier.focusRequester(focusRequester)
                                        .focusable()
                                        .offset { IntOffset(0, offsetY.value.toInt()) }
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures(
                                                    onDragEnd = {
                                                        coroutineScope.launch {
                                                            if (offsetY.value > dismissThreshold) {
                                                                onClose()
                                                            } else {
                                                                offsetY.animateTo(
                                                                        targetValue = 0f,
                                                                        animationSpec = spring()
                                                                )
                                                            }
                                                        }
                                                    },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        coroutineScope.launch {
                                                            offsetY.snapTo(
                                                                    (offsetY.value + dragAmount)
                                                                            .coerceAtLeast(0f)
                                                            )
                                                        }
                                                    }
                                            )
                                        }
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFF7D6AA))
                                        .clickable(
                                                indication = null,
                                                interactionSource =
                                                        remember { MutableInteractionSource() }
                                        ) {
                                            // Prevent click through
                                        }
                                        .padding(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Box(
                            modifier =
                                    Modifier.align(Alignment.CenterHorizontally)
                                            .width(64.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF957E60))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MenuIconItem(icon = Icons.Outlined.PushPin, label = "Pin this site")
                        MenuIconItem(icon = Icons.Outlined.Translate, label = "Translate")
                        MenuIconItem(icon = Icons.Outlined.FindInPage, label = "Find")
                        MenuIconItem(icon = Icons.Outlined.DesktopWindows, label = "Desktop site")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                    ) {
                        MenuItemRow(icon = Icons.Outlined.Download, label = "Downloads")
                        HorizontalDivider(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                modifier = Modifier.padding(start = 56.dp)
                        )

                        MenuItemRow(icon = Icons.Outlined.BookmarkBorder, label = "Bookmarks")
                        HorizontalDivider(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                modifier = Modifier.padding(start = 56.dp)
                        )

                        MenuItemRow(icon = Icons.Outlined.Extension, label = "Extensions")
                        HorizontalDivider(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                modifier = Modifier.padding(start = 56.dp)
                        )

                        MenuItemRow(icon = Icons.Outlined.History, label = "History")
                        HorizontalDivider(
                                color = Color.LightGray.copy(alpha = 0.2f),
                                modifier = Modifier.padding(start = 56.dp)
                        )

                        MenuItemRow(icon = Icons.Outlined.Settings, label = "Settings") {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            onClose()
                        }
                    }

                    // Optional: Remember Last Page Switch
                    /*
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Remember Last Page")
                        Switch(checked = rememberLastPage, onCheckedChange = onRememberLastPageChanged)
                    }
                    */
                }
            }
        }
    }
}

@Composable
fun MenuIconItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                    Modifier.width(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClick() }
                            .padding(8.dp)
    ) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF42382B),
                    modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MenuItemRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF42382B),
                modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}
