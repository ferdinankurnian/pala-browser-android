package com.iydheko.palabrowser.ui.components.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iydheko.palabrowser.R

@Composable
fun BrowserBottomBar(
        currentUrl: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isLoading: Boolean,
        onBackClick: () -> Unit,
        onForwardClick: () -> Unit,
        onReloadClick: () -> Unit,
        onMenuClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
        val rotation by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                ),
                        label = "Refresh button rotation"
                )

        Row(
                modifier = modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onMenuClick),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector =
                                        ImageVector.vectorResource(
                                                id = R.drawable.select_window_2_24px
                                        ),
                                contentDescription = "Select Tabs",
                                modifier = Modifier.size(24.dp)
                        )
                }
                Surface(
                        modifier = Modifier.weight(1f).animateContentSize(),
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                IconButton(onClick = onBackClick, enabled = canGoBack) {
                                        Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                contentDescription = "Back"
                                        )
                                }

                                AnimatedVisibility(visible = canGoForward) {
                                        IconButton(onClick = onForwardClick) {
                                                Icon(
                                                        imageVector =
                                                                Icons.AutoMirrored.Outlined
                                                                        .ArrowForward,
                                                        contentDescription = "Forward"
                                                )
                                        }
                                }

                                Text(
                                        text = currentUrl,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.SemiBold
                                )

                                IconButton(onClick = onReloadClick) {
                                        Icon(
                                                imageVector =
                                                        if (isLoading) {
                                                                ImageVector.vectorResource(
                                                                        id =
                                                                                R.drawable
                                                                                        .progress_activity_24px
                                                                )
                                                        } else {
                                                                Icons.Outlined.Refresh
                                                        },
                                                contentDescription = "Reload",
                                                modifier =
                                                        Modifier.graphicsLayer(
                                                                rotationZ =
                                                                        if (isLoading) rotation
                                                                        else 0f
                                                        )
                                        )
                                }
                        }
                }
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onMenuClick),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More menu",
                                modifier = Modifier.size(24.dp)
                        )
                }
        }
}
