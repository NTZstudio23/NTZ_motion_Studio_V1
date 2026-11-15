package com.example.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NtzAndroidBridge(
  private val context: Context,
  private val onFileSaved: (SavedFileResult) -> Unit,
  private val onSaveError: (String) -> Unit,
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  private data class PendingTransfer(
    val fileName: String,
    val mimeType: String,
    val tempFile: File,
    val stream: BufferedOutputStream,
  )

  private val transfers = ConcurrentHashMap<String, PendingTransfer>()

  init {
    runCatching {
      val cacheDir = File(context.cacheDir, "ntz_exports")
      if (cacheDir.exists()) {
        cacheDir.listFiles()?.forEach { file ->
          if (file.isFile) file.delete()
        }
      }
    }
  }

  @JavascriptInterface
  fun startSave(fileName: String, mimeType: String): String {
    return try {
      val id = UUID.randomUUID().toString()
      val cacheDir = File(context.cacheDir, "ntz_exports").apply { mkdirs() }
      val tempFile = File(cacheDir, "$id.tmp")
      val stream = BufferedOutputStream(FileOutputStream(tempFile), 65536)
      transfers[id] = PendingTransfer(fileName, mimeType, tempFile, stream)
      id
    } catch (e: Exception) {
      postError(e.message ?: "Gagal memulai penyimpanan file")
      ""
    }
  }

  @JavascriptInterface
  fun appendChunk(transferId: String, base64Chunk: String) {
    val transfer = transfers[transferId] ?: return
    try {
      if (base64Chunk.isNotEmpty()) {
        val bytes = Base64.decode(base64Chunk, Base64.DEFAULT)
        transfer.stream.write(bytes)
      }
    } catch (e: Exception) {
      cleanupTransfer(transferId)
      postError(e.message ?: "Gagal menulis bagian file")
    }
  }

  @JavascriptInterface
  fun finishSave(transferId: String) {
    val transfer = transfers.remove(transferId) ?: return
    try {
      transfer.stream.flush()
      transfer.stream.close()
      val result =
        NtzFileSaver.publishTempFileToDownloads(
          context = context,
          tempFile = transfer.tempFile,
          requestedName = transfer.fileName,
          providedMime = transfer.mimeType,
        )
      mainHandler.post { onFileSaved(result) }
    } catch (e: Exception) {
      postError(e.message ?: "Gagal menyimpan file ke Downloads")
    } finally {
      runCatching { transfer.tempFile.delete() }
    }
  }

  @JavascriptInterface
  fun cancelSave(transferId: String) {
    cleanupTransfer(transferId)
  }

  @JavascriptInterface
  fun onSaveError(message: String) {
    postError(message)
  }

  @JavascriptInterface
  fun copyToClipboard(text: String): Boolean {
    return try {
      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("NTZ Motion Studio", text)
      cm.setPrimaryClip(clip)
      true
    } catch (_: Exception) {
      false
    }
  }

  @JavascriptInterface
  fun readFromClipboard(): String {
    return try {
      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = cm.primaryClip ?: return ""
      if (clip.itemCount <= 0) return ""
      clip.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    } catch (_: Exception) {
      ""
    }
  }

  private fun cleanupTransfer(transferId: String) {
    val transfer = transfers.remove(transferId) ?: return
    runCatching { transfer.stream.close() }
    runCatching { transfer.tempFile.delete() }
  }

  private fun postError(message: String) {
    mainHandler.post { onSaveError(message) }
  }

  companion object {
    val BRIDGE_JS: String =
      """
      (function() {
        if (window.__ntzAndroidHooked) return;
        window.__ntzAndroidHooked = true;

        window.__ntzSaveBlobUrl = async function(blobUrl, filename) {
          if (!window.AndroidBridge) return;
          var id = '';
          try {
            var resp = await fetch(blobUrl);
            var blob = await resp.blob();
            var mime = blob.type || 'application/octet-stream';
            var name = filename || ('ntz-export-' + Date.now());
            id = window.AndroidBridge.startSave(name, mime);
            if (!id) return;
            var CHUNK = 524288;
            for (var offset = 0; offset < blob.size; offset += CHUNK) {
              var slice = blob.slice(offset, Math.min(offset + CHUNK, blob.size));
              var b64 = await new Promise(function(resolve, reject) {
                var r = new FileReader();
                r.onload = function() {
                  var res = String(r.result || '');
                  var idx = res.indexOf(',');
                  resolve(idx >= 0 ? res.slice(idx + 1) : res);
                };
                r.onerror = function() { reject(r.error || new Error('FileReader error')); };
                r.readAsDataURL(slice);
              });
              window.AndroidBridge.appendChunk(id, b64);
            }
            window.AndroidBridge.finishSave(id);
          } catch (e) {
            if (id) {
              try { window.AndroidBridge.cancelSave(id); } catch (_) {}
            }
            try {
              window.AndroidBridge.onSaveError(String(e && e.message ? e.message : e));
            } catch (_) {}
          }
        };

        var origClick = HTMLAnchorElement.prototype.click;
        HTMLAnchorElement.prototype.click = function() {
          if (this.href && this.href.indexOf('blob:') === 0 && window.AndroidBridge) {
            window.__ntzSaveBlobUrl(this.href, this.download || '');
            return;
          }
          return origClick.apply(this, arguments);
        };

        if (window.AndroidBridge) {
          var origClip = navigator.clipboard;
          var bridgeClipboard = {
            writeText: async function(text) {
              try { window.AndroidBridge.copyToClipboard(String(text == null ? '' : text)); } catch (_) {}
              if (origClip && origClip.writeText) {
                try { await origClip.writeText(text); } catch (_) {}
              }
            },
            readText: async function() {
              if (origClip && origClip.readText) {
                try {
                  var t = await origClip.readText();
                  if (t) return t;
                } catch (_) {}
              }
              try {
                return window.AndroidBridge.readFromClipboard() || '';
              } catch (_) {
                return '';
              }
            }
          };
          try {
            Object.defineProperty(navigator, 'clipboard', {
              get: function() { return bridgeClipboard; },
              configurable: true
            });
          } catch (_) {
            try {
              if (origClip) {
                origClip.writeText = bridgeClipboard.writeText;
                origClip.readText = bridgeClipboard.readText;
              }
            } catch (__) {}
          }
        }
      })();
      """.trimIndent()
  }
}
