package com.iydheko.palabrowser.ui.components.browser

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.iydheko.palabrowser.utils.createImageFile
import com.iydheko.palabrowser.utils.isPhotoPickerAvailable

@Composable
fun BrowserWebView(
        webViewInstance: WebView,
        modifier: Modifier = Modifier,
        onTitleChange: (String?) -> Unit,
        onIconChange: (Bitmap?) -> Unit
) {
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val fileChooserLauncher =
        rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            var uris: Array<Uri>? = null
            if (result.resultCode == Activity.RESULT_OK) {
                val intentData = result.data
                if (intentData?.clipData != null) {
                    // Handle multiple files from Photo Picker or Document Picker
                    val count = intentData.clipData!!.itemCount
                    uris = Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
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

    // Lifecycle Management
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewInstance) {
        val observer =
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> webViewInstance.onResume()
                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> webViewInstance.onPause()
                        else -> {}
                    }
                }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
            modifier =
                    modifier.padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
    ) {
        val context = LocalContext.current

        // Optimize WebChromeClient creation
        val chromeClient =
                remember(onTitleChange, onIconChange) {
                    object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            onTitleChange(title)
                        }

                        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                            super.onReceivedIcon(view, icon)
                            onIconChange(icon)
                        }

                        override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                        ): Boolean {
                            fileChooserCallback?.onReceiveValue(null)
                            fileChooserCallback = filePathCallback

                            val isMultiple =
                                    fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                            val acceptTypes =
                                    fileChooserParams?.acceptTypes?.firstOrNull { it.isNotEmpty() }
                                            ?: ""
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
                                    // The new Photo Picker UI has a camera option
                                    // inside, so we don't need a chooser.
                                }
                            }

                            // Fallback for older devices or non-media file types
                            if (intent == null) {
                                intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                intent.type = acceptTypes.ifEmpty { "*/*" }

                                val mimeTypes =
                                        fileChooserParams?.acceptTypes?.filter { it.isNotEmpty() }
                                if ((mimeTypes?.size ?: 0) > 1) {
                                    intent.putExtra(
                                            Intent.EXTRA_MIME_TYPES,
                                            mimeTypes!!.toTypedArray()
                                    )
                                }

                                if (isMultiple) {
                                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }

                                // Add camera as a separate option only in the fallback
                                // for single-image requests
                                if (!isMultiple && acceptsImages) {
                                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                    createImageFile(context)?.let { file ->
                                        cameraImageUri =
                                                FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.provider",
                                                        file
                                                )
                                        cameraIntent.putExtra(
                                                MediaStore.EXTRA_OUTPUT,
                                                cameraImageUri
                                        )
                                    }
                                    val chooser = Intent.createChooser(intent, "Choose File")
                                    if (cameraIntent.resolveActivity(context.packageManager) != null
                                    ) {
                                        chooser.putExtra(
                                                Intent.EXTRA_INITIAL_INTENTS,
                                                arrayOf(cameraIntent)
                                        )
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
                }

        AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    // Detach from any previous parent to avoid "The specified child already has a
                    // parent" error
                    (webViewInstance.parent as? ViewGroup)?.removeView(webViewInstance)

                    webViewInstance.apply {
                        layoutParams =
                                ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                )
                    }
                },
                update = { view -> view.webChromeClient = chromeClient }
        )
    }
}
