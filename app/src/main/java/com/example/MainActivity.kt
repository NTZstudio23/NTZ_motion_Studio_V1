package com.example

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studio.BackActionResult
import com.example.studio.JsDialogState
import com.example.studio.MotionStudioWebView
import com.example.studio.NtzFileSaver
import com.example.studio.NtzSplashOverlay
import com.example.studio.StudioWebController
import com.example.ui.theme.NtzBorderStrong
import com.example.ui.theme.NtzBrightCyan
import com.example.ui.theme.NtzDeepInk
import com.example.ui.theme.NtzInput
import com.example.ui.theme.NtzMotionStudioTheme
import com.example.ui.theme.NtzNeonGreen
import com.example.ui.theme.NtzNeonViolet
import com.example.ui.theme.NtzSurface
import com.example.ui.theme.NtzTextPrimary
import com.example.ui.theme.NtzTextSecondary
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
    )
    setContent {
      NtzMotionStudioTheme {
        MotionStudioApp()
      }
    }
  }
}

@Composable
fun MotionStudioApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val webController = remember { StudioWebController() }

  var showSplash by remember { mutableStateOf(true) }
  var isWebReady by remember { mutableStateOf(false) }
  var jsDialog by remember { mutableStateOf<JsDialogState?>(null) }
  var showRenderExitDialog by remember { mutableStateOf(false) }
  var lastBackPressMs by remember { mutableLongStateOf(0L) }

  BackHandler(enabled = true) {
    if (showSplash) return@BackHandler
    webController.handleBack { action ->
      when (action) {
        BackActionResult.HANDLED -> Unit
        BackActionResult.CONFIRM_RENDER_EXIT -> showRenderExitDialog = true
        BackActionResult.EXIT -> {
          val now = System.currentTimeMillis()
          if (now - lastBackPressMs <= 2000L) {
            (context as? Activity)?.finish()
          } else {
            lastBackPressMs = now
            Toast.makeText(
              context,
              context.getString(R.string.back_exit_prompt),
              Toast.LENGTH_SHORT,
            ).show()
          }
        }
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(NtzDeepInk)) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = NtzDeepInk,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
          Snackbar(
            snackbarData = data,
            containerColor = NtzSurface,
            contentColor = NtzTextPrimary,
            actionColor = NtzBrightCyan,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      },
    ) { innerPadding ->
      MotionStudioWebView(
        controller = webController,
        onWebReady = { isWebReady = true },
        onFileSaved = { saved ->
          scope.launch {
            val res =
              snackbarHostState.showSnackbar(
                message = context.getString(R.string.download_saved_msg, saved.fileName),
                actionLabel = context.getString(R.string.action_share),
                duration = SnackbarDuration.Long,
              )
            if (res == SnackbarResult.ActionPerformed) {
              runCatching { NtzFileSaver.shareOrOpenFile(context, saved) }
            }
          }
        },
        onSaveError = { errorMsg ->
          scope.launch {
            snackbarHostState.showSnackbar(
              message = context.getString(R.string.download_failed_msg, errorMsg),
              duration = SnackbarDuration.Long,
            )
          }
        },
        onJsDialog = { dialog ->
          jsDialog = dialog
        },
        modifier = Modifier.fillMaxSize().padding(innerPadding),
      )
    }

    // 6-second intro splash screen displaying logo and "NTZ MOTION STUDIO V.1"
    NtzSplashOverlay(
      visible = showSplash,
      isWebReady = isWebReady,
      onSplashFinished = { showSplash = false },
    )

    // Native Material 3 dialogs for JavaScript alert(), confirm(), and prompt()
    when (val dlg = jsDialog) {
      is JsDialogState.Alert -> {
        AlertDialog(
          onDismissRequest = {
            dlg.result.confirm()
            jsDialog = null
          },
          containerColor = NtzSurface,
          titleContentColor = NtzTextPrimary,
          textContentColor = NtzTextSecondary,
          shape = RoundedCornerShape(16.dp),
          title = {
            Text(
              text = stringResource(R.string.app_name),
              fontWeight = FontWeight.Bold,
            )
          },
          text = { Text(text = dlg.message) },
          confirmButton = {
            Button(
              onClick = {
                dlg.result.confirm()
                jsDialog = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = NtzNeonViolet),
              modifier = Modifier.testTag("js_alert_ok_button"),
            ) {
              Text(stringResource(R.string.action_ok))
            }
          },
        )
      }
      is JsDialogState.Confirm -> {
        AlertDialog(
          onDismissRequest = {
            dlg.result.cancel()
            jsDialog = null
          },
          containerColor = NtzSurface,
          titleContentColor = NtzTextPrimary,
          textContentColor = NtzTextSecondary,
          shape = RoundedCornerShape(16.dp),
          title = {
            Text(
              text = stringResource(R.string.app_name),
              fontWeight = FontWeight.Bold,
            )
          },
          text = { Text(text = dlg.message) },
          confirmButton = {
            Button(
              onClick = {
                dlg.result.confirm()
                jsDialog = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = NtzNeonViolet),
            ) {
              Text(stringResource(R.string.action_ok))
            }
          },
          dismissButton = {
            TextButton(
              onClick = {
                dlg.result.cancel()
                jsDialog = null
              },
            ) {
              Text(stringResource(R.string.action_cancel), color = NtzTextSecondary)
            }
          },
        )
      }
      is JsDialogState.Prompt -> {
        var inputValue by remember(dlg) { mutableStateOf(dlg.defaultValue) }
        AlertDialog(
          onDismissRequest = {
            dlg.result.cancel()
            jsDialog = null
          },
          containerColor = NtzSurface,
          titleContentColor = NtzTextPrimary,
          textContentColor = NtzTextSecondary,
          shape = RoundedCornerShape(16.dp),
          title = {
            Text(
              text = stringResource(R.string.app_name),
              fontWeight = FontWeight.Bold,
            )
          },
          text = {
            Column {
              Text(text = dlg.message)
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                singleLine = true,
                colors =
                  OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NtzInput,
                    unfocusedContainerColor = NtzInput,
                    focusedBorderColor = NtzBrightCyan,
                    unfocusedBorderColor = NtzBorderStrong,
                    focusedTextColor = NtzTextPrimary,
                    unfocusedTextColor = NtzTextPrimary,
                  ),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          },
          confirmButton = {
            Button(
              onClick = {
                dlg.result.confirm(inputValue)
                jsDialog = null
              },
              colors = ButtonDefaults.buttonColors(containerColor = NtzNeonViolet),
            ) {
              Text(stringResource(R.string.action_ok))
            }
          },
          dismissButton = {
            TextButton(
              onClick = {
                dlg.result.cancel()
                jsDialog = null
              },
            ) {
              Text(stringResource(R.string.action_cancel), color = NtzTextSecondary)
            }
          },
        )
      }
      null -> Unit
    }

    if (showRenderExitDialog) {
      AlertDialog(
        onDismissRequest = { showRenderExitDialog = false },
        containerColor = NtzSurface,
        titleContentColor = NtzTextPrimary,
        textContentColor = NtzTextSecondary,
        shape = RoundedCornerShape(16.dp),
        title = {
          Text(
            text = stringResource(R.string.render_exit_warning_title),
            fontWeight = FontWeight.Bold,
          )
        },
        text = { Text(stringResource(R.string.render_exit_warning_msg)) },
        confirmButton = {
          Button(
            onClick = { showRenderExitDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = NtzNeonGreen),
          ) {
            Text(stringResource(R.string.action_stay), color = NtzDeepInk)
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              showRenderExitDialog = false
              (context as? Activity)?.finish()
            },
            border = BorderStroke(1.dp, NtzBorderStrong),
          ) {
            Text(stringResource(R.string.action_exit), color = NtzTextPrimary)
          }
        },
      )
    }
  }
}
