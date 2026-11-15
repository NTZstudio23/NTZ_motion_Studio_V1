package com.example.studio

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.NtzDeepInk
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

private const val STUDIO_HOST = "appassets.androidplatform.net"
private const val STUDIO_URL = "https://$STUDIO_HOST/assets/ntz_motion_studio.html"

sealed interface JsDialogState {
  data class Alert(val message: String, val result: JsResult) : JsDialogState

  data class Confirm(val message: String, val result: JsResult) : JsDialogState

  data class Prompt(val message: String, val defaultValue: String, val result: JsPromptResult) :
    JsDialogState
}

enum class BackActionResult {
  HANDLED,
  CONFIRM_RENDER_EXIT,
  EXIT,
}

class StudioWebController {
  internal var webView: WebView? = null
  internal var customViewCallback: WebChromeClient.CustomViewCallback? = null
  var hasCustomFullscreen by mutableStateOf(false)
    internal set

  fun exitCustomFullscreen() {
    customViewCallback?.onCustomViewHidden()
    customViewCallback = null
    hasCustomFullscreen = false
  }

  fun handleBack(onResult: (BackActionResult) -> Unit) {
    if (hasCustomFullscreen) {
      exitCustomFullscreen()
      onResult(BackActionResult.HANDLED)
      return
    }
    val wv = webView
    if (wv == null) {
      onResult(BackActionResult.EXIT)
      return
    }
    val script =
      """
      (function() {
        var fakeFs = document.querySelector('.preview-box.fake-fs');
        if (fakeFs) {
          fakeFs.classList.remove('fake-fs');
          return 'handled';
        }
        var modals = document.querySelectorAll('.modal-backdrop.open');
        if (modals.length > 0) {
          modals.forEach(function(m) { m.classList.remove('open'); });
          return 'handled';
        }
        var sheet = document.getElementById('msheet');
        if (sheet && sheet.classList.contains('open')) {
          if (typeof window.closeMSheet === 'function') window.closeMSheet();
          else sheet.classList.remove('open');
          return 'handled';
        }
        var tab = document.body.getAttribute('data-mtab');
        if (tab && tab !== 'studio' && typeof window.setMTab === 'function') {
          window.setMTab('studio');
          return 'handled';
        }
        if (typeof state !== 'undefined' && (state.isRendering || (typeof batch !== 'undefined' && batch.running))) {
          return 'rendering';
        }
        return 'exit';
      })();
      """.trimIndent()
    wv.evaluateJavascript(script) { raw ->
      val cleaned = raw?.trim('"', ' ', '\n', '\r').orEmpty()
      when (cleaned) {
        "handled" -> onResult(BackActionResult.HANDLED)
        "rendering" -> onResult(BackActionResult.CONFIRM_RENDER_EXIT)
        else -> onResult(BackActionResult.EXIT)
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MotionStudioWebView(
  controller: StudioWebController,
  onWebReady: () -> Unit,
  onFileSaved: (SavedFileResult) -> Unit,
  onSaveError: (String) -> Unit,
  onJsDialog: (JsDialogState) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val currentOnWebReady by rememberUpdatedState(onWebReady)
  val currentOnFileSaved by rememberUpdatedState(onFileSaved)
  val currentOnSaveError by rememberUpdatedState(onSaveError)
  val currentOnJsDialog by rememberUpdatedState(onJsDialog)

  val lifecycleOwner = LocalLifecycleOwner.current
  var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var fullscreenView by remember { mutableStateOf<View?>(null) }
  var webViewGeneration by remember { mutableIntStateOf(0) }

  val multiFilePickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
      val callback = pendingFileCallback
      pendingFileCallback = null
      if (callback != null) {
        if (uris.isNotEmpty()) {
          callback.onReceiveValue(uris.toTypedArray())
        } else {
          callback.onReceiveValue(null)
        }
      }
    }

  val webView = remember(webViewGeneration) {
    WebView(context).apply {
      layoutParams =
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      setBackgroundColor(android.graphics.Color.parseColor("#05040B"))
      setLayerType(View.LAYER_TYPE_HARDWARE, null)
      keepScreenOn = true
      isFocusable = true
      isFocusableInTouchMode = true
      isVerticalScrollBarEnabled = true
      isHorizontalScrollBarEnabled = false
      isScrollbarFadingEnabled = true
      scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
      overScrollMode = View.OVER_SCROLL_NEVER

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
      }

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        @Suppress("DEPRECATION")
        databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = false
        displayZoomControls = false
        setSupportZoom(false)
        setNeedInitialFocus(false)
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT
        javaScriptCanOpenWindowsAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          isAlgorithmicDarkeningAllowed = false
        }
      }

      val targetWebView = this
      CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(targetWebView, true)
      }

      val bridge =
        NtzAndroidBridge(
          context = context.applicationContext,
          onFileSaved = { result -> currentOnFileSaved(result) },
          onSaveError = { msg -> currentOnSaveError(msg) },
        )
      addJavascriptInterface(bridge, "AndroidBridge")

      webViewClient =
        object : WebViewClient() {
          override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
          ): WebResourceResponse? {
            val url = request?.url ?: return null
            if (url.host == STUDIO_HOST) {
              return when (url.path) {
                "/assets/ntz_motion_studio.html" -> {
                  try {
                    val stream = context.assets.open("ntz_motion_studio.html")
                    WebResourceResponse(
                      "text/html",
                      "UTF-8",
                      200,
                      "OK",
                      mapOf("Access-Control-Allow-Origin" to "*"),
                      stream,
                    )
                  } catch (_: Exception) {
                    null
                  }
                }
                "/favicon.ico" -> {
                  WebResourceResponse(
                    "image/x-icon",
                    "UTF-8",
                    204,
                    "No Content",
                    emptyMap(),
                    ByteArrayInputStream(ByteArray(0)),
                  )
                }
                else -> {
                  WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    404,
                    "Not Found",
                    emptyMap(),
                    ByteArrayInputStream(ByteArray(0)),
                  )
                }
              }
            }
            return null
          }

          override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
          ): Boolean {
            val uri = request?.url ?: return false
            val scheme = uri.scheme?.lowercase().orEmpty()
            if (uri.host == STUDIO_HOST || scheme == "blob" || scheme == "data" || scheme == "about") {
              return false
            }
            return try {
              val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              context.startActivity(intent)
              true
            } catch (_: Exception) {
              true
            }
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            view?.evaluateJavascript(NtzAndroidBridge.BRIDGE_JS, null)
            currentOnWebReady()
          }

          override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?,
          ): Boolean {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = null
            fullscreenView = null
            controller.hasCustomFullscreen = false
            controller.customViewCallback = null
            currentOnSaveError("Renderer WebView dimuat ulang otomatis untuk menjaga kestabilan")
            webViewGeneration++
            return true
          }
        }

      webChromeClient =
        object : WebChromeClient() {
          override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?,
          ): Boolean {
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = filePathCallback
            return try {
              multiFilePickerLauncher.launch(arrayOf("*/*"))
              true
            } catch (e: Exception) {
              pendingFileCallback?.onReceiveValue(null)
              pendingFileCallback = null
              currentOnSaveError(e.message ?: "Gagal membuka pemilih file")
              false
            }
          }

          override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (view == null) {
              callback?.onCustomViewHidden()
              return
            }
            if (fullscreenView != null) {
              callback?.onCustomViewHidden()
              return
            }
            fullscreenView = view
            controller.customViewCallback = callback
            controller.hasCustomFullscreen = true
          }

          override fun onHideCustomView() {
            fullscreenView = null
            controller.customViewCallback = null
            controller.hasCustomFullscreen = false
          }

          override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?,
          ): Boolean {
            if (result == null) return false
            currentOnJsDialog(JsDialogState.Alert(message.orEmpty(), result))
            return true
          }

          override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?,
          ): Boolean {
            if (result == null) return false
            currentOnJsDialog(JsDialogState.Confirm(message.orEmpty(), result))
            return true
          }

          override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?,
          ): Boolean {
            if (result == null) return false
            currentOnJsDialog(
              JsDialogState.Prompt(message.orEmpty(), defaultValue.orEmpty(), result)
            )
            return true
          }

          override fun onPermissionRequest(request: PermissionRequest?) {
            request?.grant(request.resources)
          }

          override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
        }

      setDownloadListener { url, _, contentDisposition, mimetype, _ ->
        when {
          url.startsWith("blob:") -> {
            val guessed =
              URLUtil.guessFileName(url, contentDisposition, mimetype)
                .replace("'", "\\'")
            val safeUrl = url.replace("'", "\\'")
            evaluateJavascript("window.__ntzSaveBlobUrl('$safeUrl', '$guessed');", null)
          }
          url.startsWith("data:") -> {
            saveDataUrlToDownloads(
              context = context,
              dataUrl = url,
              fileName = URLUtil.guessFileName(url, contentDisposition, mimetype),
              mimeType = mimetype,
              onSaved = currentOnFileSaved,
              onError = currentOnSaveError,
            )
          }
          url.startsWith("http://") || url.startsWith("https://") -> {
            try {
              val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
              val req =
                DownloadManager.Request(url.toUri()).apply {
                  setMimeType(mimetype)
                  setTitle(fileName)
                  setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                  )
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                  } else {
                    setDestinationInExternalFilesDir(
                      context,
                      Environment.DIRECTORY_DOWNLOADS,
                      fileName,
                    )
                  }
                }
              val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
              dm.enqueue(req)
            } catch (e: Exception) {
              currentOnSaveError(e.message ?: "Gagal mengunduh file")
            }
          }
        }
      }

      loadUrl(STUDIO_URL)
    }
  }

  DisposableEffect(webView, lifecycleOwner) {
    controller.webView = webView
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> webView.onResume()
        Lifecycle.Event.ON_PAUSE -> webView.onPause()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      pendingFileCallback?.onReceiveValue(null)
      pendingFileCallback = null
      controller.webView = null
      webView.stopLoading()
      webView.destroy()
    }
  }

  Box(modifier = modifier.fillMaxSize().background(NtzDeepInk)) {
    key(webViewGeneration) {
      AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize().testTag("studio_webview"),
      )
    }

    val fsView = fullscreenView
    if (fsView != null && controller.hasCustomFullscreen) {
      Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
        AndroidView(
          factory = { fsView },
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

private fun saveDataUrlToDownloads(
  context: Context,
  dataUrl: String,
  fileName: String,
  mimeType: String?,
  onSaved: (SavedFileResult) -> Unit,
  onError: (String) -> Unit,
) {
  try {
    val commaIdx = dataUrl.indexOf(',')
    if (commaIdx < 0) throw IllegalArgumentException("Data URL tidak valid")
    val header = dataUrl.substring(5, commaIdx)
    val payload = dataUrl.substring(commaIdx + 1)
    val isBase64 = header.contains(";base64", ignoreCase = true)
    val resolvedMime = header.substringBefore(';').ifEmpty { mimeType ?: "application/octet-stream" }
    val bytes =
      if (isBase64) Base64.decode(payload, Base64.DEFAULT)
      else Uri.decode(payload).toByteArray(Charsets.UTF_8)

    val cacheDir = File(context.cacheDir, "ntz_exports").apply { mkdirs() }
    val tempFile = File.createTempFile("ntz_data_", ".tmp", cacheDir)
    try {
      FileOutputStream(tempFile).use { it.write(bytes) }
      val result = NtzFileSaver.publishTempFileToDownloads(context, tempFile, fileName, resolvedMime)
      onSaved(result)
    } finally {
      runCatching { tempFile.delete() }
    }
  } catch (e: Exception) {
    onError(e.message ?: "Gagal menyimpan data file")
  }
}
