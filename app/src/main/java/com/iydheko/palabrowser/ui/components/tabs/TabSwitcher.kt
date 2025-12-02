package com.iydheko.palabrowser.ui.components.tabs

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iydheko.palabrowser.R
import com.iydheko.palabrowser.ui.screens.browser.WebViewModel

@Composable
fun TabSwitcher(
        tabs: List<WebViewModel.BrowserTab>,
        activeTabId: String?,
        onTabSelected: (String) -> Unit,
        onTabClosed: (String) -> Unit,
        onNewTab: () -> Unit,
        modifier: Modifier = Modifier
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
                modifier = modifier.fillMaxSize()
                                .background(
                                        Color(0xFFE8B86D)
                                ) // Match the background color from screenshot
        ) {
                // Top Bar Area (Placeholder or actual top bar if needed in switcher)
                // For now just spacing
                Spacer(modifier = Modifier.height(40.dp))

                // Horizontal Pager for Tabs
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

                // Bottom Bar Area for Tab Switcher (Plus button)
                Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                ) {
                        // Plus Button
                        IconButton(
                                onClick = onNewTab,
                                modifier = Modifier.size(56.dp).background(Color.White, CircleShape)
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Tab",
                                        tint = Color.Black
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
    Column(
        modifier = Modifier
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                    imageVector = ImageVector.vectorResource(id = R.drawable.globe_24px),
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
        Card(
            modifier =
                Modifier.fillMaxWidth()
                    .aspectRatio(
                        0.6f
                    ) // Portrait aspect ratio
                    .clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                ) // Light cream color
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Thumbnail
                if (tab.thumbnail != null) {
                    Image(
                        bitmap = tab.thumbnail.asImageBitmap(),
                        contentDescription = "Tab Thumbnail",
                        contentScale = ContentScale.Crop,
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
