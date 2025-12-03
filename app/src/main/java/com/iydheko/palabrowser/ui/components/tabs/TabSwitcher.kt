package com.iydheko.palabrowser.ui.components.tabs

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iydheko.palabrowser.R
import com.iydheko.palabrowser.ui.screens.browser.WebViewModel

@Composable
fun TabSwitcher(
        tabs: List<WebViewModel.BrowserTab>,
        activeTabId: String?,
        onTabSelected: (String) -> Unit,
        onTabClosed: (String) -> Unit,
        onNewTab: () -> Unit,
        modifier: Modifier = Modifier,
        paddingValues: PaddingValues = PaddingValues(0.dp),
        onMenuClick: () -> Unit,
) {
        val initialPage = tabs.indexOfFirst { it.id == activeTabId }.takeIf { it != -1 } ?: 0
        val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }

        LaunchedEffect(activeTabId) {
                val index = tabs.indexOfFirst { it.id == activeTabId }
                if (index != -1 && index != pagerState.currentPage) {
                        pagerState.animateScrollToPage(index)
                }
        }
        Column(
                modifier =
                        modifier.fillMaxSize()
                                .padding(paddingValues)
                                .background(
                                        Color(0xFFE8B86D)
                                ) // Match the background color from screenshot
        ) {
                // Top Bar Area (Placeholder or actual top bar if needed in switcher)
                // For now just spacing
                Spacer(modifier = Modifier.height(40.dp))

                // Horizontal Pager for Tabs
                if (tabs.isNotEmpty()) {
                        HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 40.dp),
                                pageSpacing = 16.dp,
                                pageSize = PageSize.Fill,
                                modifier = Modifier.weight(1f)
                        ) { page ->
                                val tab = tabs[page]
                                TabCard(
                                        tab = tab,
                                        isActive = tab.id == activeTabId,
                                        onClick = { onTabSelected(tab.id) },
                                        onClose = { onTabClosed(tab.id) }
                                )
                        }
                } else {
                        // Empty state - just take up the space
                        Spacer(modifier = Modifier.weight(1f))
                }
                Row(
                        modifier =
                                modifier.fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Box(
                                modifier =
                                        Modifier.size(48.dp).clip(CircleShape).clickable(
                                                        enabled = tabs.isNotEmpty()
                                                ) {
                                                if (tabs.isNotEmpty()) {
                                                        onTabSelected(
                                                                tabs[pagerState.currentPage].id
                                                        )
                                                }
                                        },
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector =
                                                ImageVector.vectorResource(
                                                        id = R.drawable.select_window_2_24px
                                                ),
                                        contentDescription = "Select Tabs",
                                        modifier = Modifier.size(24.dp),
                                        tint =
                                                if (tabs.isEmpty()) Color.Gray.copy(alpha = 0.3f)
                                                else Color.Unspecified
                                )
                        }
                        Surface(
                                modifier = Modifier.weight(1f).height(48.dp).animateContentSize(),
                                color = Color.White,
                                shape = RoundedCornerShape(50),
                                onClick = onNewTab,
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Tab",
                                                tint = Color.Black
                                        )
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
}

@Composable
fun TabCard(
        tab: WebViewModel.BrowserTab,
        isActive: Boolean,
        onClick: () -> Unit,
        onClose: () -> Unit
) {
        Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        if (tab.favicon != null) {
                                Image(
                                        bitmap = tab.favicon.asImageBitmap(),
                                        contentDescription = "Favicon",
                                        modifier = Modifier.size(24.dp)
                                )
                        } else {
                                Icon(
                                        imageVector =
                                                ImageVector.vectorResource(
                                                        id = R.drawable.globe_24px
                                                ),
                                        contentDescription = "Home",
                                        tint = Color.Gray
                                )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                }
                val thumbnail = tab.thumbnail
                Card(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .then(
                                                if (thumbnail != null) {
                                                        Modifier.aspectRatio(
                                                                thumbnail.width.toFloat() /
                                                                        thumbnail.height.toFloat()
                                                        )
                                                } else {
                                                        Modifier.aspectRatio(
                                                                0.6f
                                                        ) // Fallback for no thumbnail
                                                }
                                        )
                                        .clickable(onClick = onClick),
                        shape = RoundedCornerShape(24.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF8E1)
                                ) // Light cream color
                ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                                // Thumbnail
                                if (thumbnail != null) {
                                        Image(
                                                bitmap = thumbnail.asImageBitmap(),
                                                contentDescription = "Tab Thumbnail",
                                                contentScale =
                                                        ContentScale
                                                                .Crop, // Crop is fine now as aspect
                                                // ratios match
                                                modifier = Modifier.fillMaxSize(),
                                                alignment = Alignment.TopStart
                                        )
                                } else {
                                        // Placeholder if no thumbnail
                                        Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { Text(text = tab.title, color = Color.Gray) }
                                }
                                // Close Button
                                IconButton(
                                        onClick = onClose,
                                        modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .size(32.dp)
                                                        .background(Color(0x80000000), CircleShape)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Tab",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                        )
                                }
                        }
                }
        }
}
