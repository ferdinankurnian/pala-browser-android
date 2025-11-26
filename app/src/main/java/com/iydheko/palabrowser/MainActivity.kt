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


import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut


import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.iydheko.palabrowser.ui.theme.PalaBrowserTheme
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import android.webkit.ValueCallback
import android.net.Uri
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.os.Environment
import android.content.Context
import android.os.Build
import androidx.compose.material3.Switch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

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

private fun extractDomain(url: String): String {
    return try {
        java.net.URL(url).host.removePrefix("www.")
    } catch (_: Exception) {
        url // fallback to full url if it's not a valid URL
    }
}

@Composable
fun WebViewScreen(paddingValues: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("PalaBrowserPrefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    var rememberLastPage by remember { mutableStateOf(sharedPrefs.getBoolean("rememberLastPage", true)) }

    val initialUrl = if (rememberLastPage) {
        sharedPrefs.getString("lastUrl", "https://google.com") ?: "https://google.com"
    } else {
        "https://google.com"
    }

    var url by remember { mutableStateOf(initialUrl) }
    var currentUrl by remember { mutableStateOf(url) }
    var displayUrl by remember { mutableStateOf(extractDomain(url)) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var pageTitle by remember { mutableStateOf("Pala Browser") }
    var favicon by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8B86D)).padding(paddingValues)) {
            BrowserTopBar(title = pageTitle, favicon = favicon)

            BrowserWebView(
                    url = url,
                    onWebViewCreated = { webView = it },
                    onUrlChanged = { newUrl ->
                        currentUrl = newUrl
                        displayUrl = extractDomain(newUrl)
                    },
                    onHistoryUpdate = { historyUrl ->
                        currentUrl = historyUrl
                        displayUrl = extractDomain(historyUrl)
                        if (rememberLastPage) {
                            coroutineScope.launch {
                                sharedPrefs.edit().putString("lastUrl", historyUrl).apply()
                            }
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
                    currentUrl = displayUrl,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    isLoading = isLoading,
                    onBackClick = { webView?.goBack() },
                    onForwardClick = { webView?.goForward() },
                    onReloadClick = { webView?.reload() },
                    onMenuClick = { showMenu = !showMenu }
            )
        }
        BrowserMenuModal(
            showMenu = showMenu,
            rememberLastPage = rememberLastPage,
            onRememberLastPageChanged = {
                rememberLastPage = it
                coroutineScope.launch {
                    sharedPrefs.edit().putBoolean("rememberLastPage", it).apply()
                }
            },
            onClose = { showMenu = false }
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
    onHistoryUpdate: (String) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onCanGoForwardChanged: (Boolean) -> Unit,
    onTitleReceived: (String) -> Unit,
    onFaviconReceived: (Bitmap?) -> Unit,
    onIsLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        var uris: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            if (intentData?.clipData != null) {
                // Handle multiple files from Photo Picker or Document Picker
                val count = intentData.clipData!!.itemCount
                uris = Array(count) { i ->
                    intentData.clipData!!.getItemAt(i).uri
                }
            } else if (intentData?.data != null) {
                // Handle single file
                uris = arrayOf(intentData.data!!)
            } else if (cameraImageUri != null) {
                // Handle camera photo
                uris = arrayOf(cameraImageUri!!)
            }
        }
        // Always call the callback to finalize, even with null.
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
        cameraImageUri = null
    }

    Box(
        modifier =
        modifier
            .padding(horizontal = 6.dp)
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
                    settings.allowFileAccess = true // Important for file upload
                    settings.userAgentString =
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

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

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let { onHistoryUpdate(it) }
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

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback

                                val isMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                                val acceptTypes = fileChooserParams?.acceptTypes?.firstOrNull { it.isNotEmpty() } ?: ""
                                val acceptsImages = acceptTypes.contains("image/")
                                val acceptsVideo = acceptTypes.contains("video/")

                                var intent: Intent? = null

                                if (isPhotoPickerAvailable()) {
                                    if (acceptsImages || acceptsVideo) {
                                        intent = Intent(MediaStore.ACTION_PICK_IMAGES)
                                        if (isMultiple) {
                                            val maxItems = MediaStore.getPickImagesMaxLimit()
                                            intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxItems)
                                        }
                                        // The new Photo Picker UI has a camera option inside, so we don't need a chooser.
                                    }
                                }

                                // Fallback for older devices or non-media file types
                                if (intent == null) {
                                    intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    intent.type = acceptTypes.ifEmpty { "*/*" }

                                    val mimeTypes = fileChooserParams?.acceptTypes?.filter { it.isNotEmpty() }
                                    if ((mimeTypes?.size ?: 0) > 1) {
                                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes!!.toTypedArray())
                                    }

                                    if (isMultiple) {
                                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }

                                    // Add camera as a separate option only in the fallback for single-image requests
                                    if (!isMultiple && acceptsImages) {
                                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                        createImageFile(context)?.let { file ->
                                            cameraImageUri = FileProvider.getUriForFile(
                                                context, "${context.packageName}.provider", file
                                            )
                                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                                        }
                                        val chooser = Intent.createChooser(intent, "Choose File")
                                        if (cameraIntent.resolveActivity(context.packageManager) != null) {
                                            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
                                        }
                                        intent = chooser
                                    }
                                }


                                try {
                                    fileChooserLauncher.launch(intent)
                                    return true
                                } catch (_: Exception) {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = null
                                    return false
                                }
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

private fun isPhotoPickerAvailable(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

private fun createImageFile(context: Context): File? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        File.createTempFile(imageFileName, ".jpg", storageDir)
    } catch (_: Exception) {
        // Handle exception
        null
    }
}

@Composable
fun BrowserMenuModal(
    showMenu: Boolean,
    rememberLastPage: Boolean,
    onRememberLastPageChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() }
            )
        }

        // Menu modal
        AnimatedVisibility(
            visible = showMenu,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFF5F5F5), // Light gray background
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(12.dp, 8.dp)
                            .background(
                                Color(0xFFBDC1C6),
                                CircleShape
                            )
                    )

                    // Menu items - TANPA DIVIDER
                    SimpleMenuItem(
                        icon = Icons.Default.Download,
                        label = "Downloads",
                        onClick = { onClose() }
                    )

                    SimpleMenuItem(
                        icon = Icons.Default.Bookmark,
                        label = "Bookmarks",
                        onClick = { onClose() }
                    )

                    SimpleMenuItem(
                        icon = Icons.Default.Extension,
                        label = "Extensions",
                        onClick = { onClose() }
                    )

                    SimpleMenuItem(
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = { onClose() }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History, // I'll reuse this icon for now
                                contentDescription = "Remember last page",
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "Remember last page",
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 16.sp,
                                color = Color(0xFF202124),
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Switch(
                            checked = rememberLastPage,
                            onCheckedChange = onRememberLastPageChanged
                        )
                    }

                    SimpleMenuItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { onClose() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SimpleMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .size(48.dp)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .background(Color(0xFFF5F5F5))
            .clickable(
                onClick = onClick
            ),
        ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = Color(0xFF202124), // Almost black
                fontWeight = FontWeight.Normal
            )
        }
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

