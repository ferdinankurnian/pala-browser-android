package com.iydheko.palabrowser

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import com.iydheko.palabrowser.ui.theme.PalaBrowserTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            PalaBrowserTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Transparent) {
                        innerPadding ->
                    WebViewScreen(paddingValues = innerPadding)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebViewScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    var url by remember { mutableStateOf("https://google.com") }
    var currentUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var pageTitle by remember { mutableStateOf("Pala Browser") }
    var favicon by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8B86D)).padding(paddingValues)) {
        BrowserTopBar(title = pageTitle, favicon = favicon)

        BrowserWebView(
                url = url,
                onWebViewCreated = { webView = it },
                onUrlChanged = {
                    currentUrl = it
                    // Extract domain for bottom bar
                    try {
                        val domain = java.net.URL(it).host.removePrefix("www.")
                        currentUrl = domain
                    } catch (e: Exception) {
                        currentUrl = it
                    }
                },
                onCanGoBackChanged = { canGoBack = it },
                onCanGoForwardChanged = { canGoForward = it },
                onTitleReceived = { pageTitle = it },
                onFaviconReceived = { favicon = it },
                onIsLoadingChanged = { isLoading = it },
                modifier = Modifier.weight(1f)
        )

        BrowserBottomBar(
                currentUrl = currentUrl,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isLoading = isLoading,
                onBackClick = { webView?.goBack() },
                onForwardClick = { webView?.goForward() },
                onReloadClick = { webView?.reload() },
                onMenuClick = { /* TODO */}
        )
    }
}

@Composable
fun BrowserTopBar(title: String, favicon: Bitmap?) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            if (favicon != null) {
                Image(
                        bitmap = favicon.asImageBitmap(),
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
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BrowserWebView(
        url: String,
        onWebViewCreated: (WebView) -> Unit,
        onUrlChanged: (String) -> Unit,
        onCanGoBackChanged: (Boolean) -> Unit,
        onCanGoForwardChanged: (Boolean) -> Unit,
        onTitleReceived: (String) -> Unit,
        onFaviconReceived: (Bitmap?) -> Unit,
        onIsLoadingChanged: (Boolean) -> Unit,
        modifier: Modifier = Modifier
) {
    Box(
            modifier =
                    modifier.padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
    ) {
        AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams =
                                ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true

                        webViewClient =
                                object : WebViewClient() {
                                    override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        onUrlChanged(url ?: "")
                                        onCanGoBackChanged(view?.canGoBack() ?: false)
                                        onCanGoForwardChanged(view?.canGoForward() ?: false)
                                        onTitleReceived("Loading...")
                                        onFaviconReceived(null)
                                        onIsLoadingChanged(true)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        onIsLoadingChanged(false)
                                    }
                                }

                        webChromeClient =
                                object : WebChromeClient() {
                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        title?.let { onTitleReceived(it) }
                                    }

                                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                        super.onReceivedIcon(view, icon)
                                        onFaviconReceived(icon)
                                    }
                                }

                        loadUrl(url)
                        onWebViewCreated(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BrowserBottomBar(
        currentUrl: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        isLoading: Boolean,
        onBackClick: () -> Unit,
        onForwardClick: () -> Unit,
        onReloadClick: () -> Unit,
        onMenuClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Refresh button rotation"
    )

    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onMenuClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.select_window_2_24px),
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
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                AnimatedVisibility(visible = canGoForward) {
                    IconButton(onClick = onForwardClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
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
                        imageVector = if (isLoading) {
                            ImageVector.vectorResource(id = R.drawable.progress_activity_24px)
                        } else {
                            Icons.Filled.Refresh
                        },
                        contentDescription = "Reload",
                        modifier = Modifier.graphicsLayer(rotationZ = if (isLoading) rotation else 0f)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onMenuClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "More menu",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
