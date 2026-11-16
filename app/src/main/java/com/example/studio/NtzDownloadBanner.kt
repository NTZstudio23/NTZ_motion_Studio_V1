package com.example.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NtzBrightCyan
import com.example.ui.theme.NtzCard
import com.example.ui.theme.NtzNeonGreen
import com.example.ui.theme.NtzTextPrimary
import com.example.ui.theme.NtzTextSecondary
import com.example.ui.theme.SoraFontFamily
import kotlinx.coroutines.delay

sealed interface DownloadBannerState {
  val id: Long

  data class Success(
    val saved: SavedFileResult,
    override val id: Long = System.nanoTime(),
  ) : DownloadBannerState

  data class Error(
    val message: String,
    override val id: Long = System.nanoTime(),
  ) : DownloadBannerState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NtzDownloadBannerOverlay(
  banner: DownloadBannerState?,
  onShare: (SavedFileResult) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(banner?.id) {
    if (banner != null) {
      val durationMs = if (banner is DownloadBannerState.Success) 3500L else 4500L
      delay(durationMs)
      onDismiss()
    }
  }

  Box(
    modifier = modifier.fillMaxWidth().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    AnimatedVisibility(
      visible = banner != null,
      enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
      val activeBanner = banner ?: return@AnimatedVisibility
      val dismissState =
        rememberSwipeToDismissBoxState(
          confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
              onDismiss()
              true
            } else {
              false
            }
          }
        )

      LaunchedEffect(activeBanner.id) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
          dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
      }

      val isSuccess = activeBanner is DownloadBannerState.Success
      val accentColor = if (isSuccess) NtzNeonGreen else Color(0xFFEF4444)

      SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().testTag("download_banner"),
      ) {
        Surface(
          color = NtzCard.copy(alpha = 0.96f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
          shadowElevation = 10.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Icon(
              imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isSuccess) "Tersimpan di Download" else "Gagal menyimpan",
                fontFamily = SoraFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = NtzTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  when (activeBanner) {
                    is DownloadBannerState.Success -> activeBanner.saved.fileName
                    is DownloadBannerState.Error -> activeBanner.message
                  },
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = NtzTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            if (activeBanner is DownloadBannerState.Success) {
              TextButton(
                onClick = {
                  onShare(activeBanner.saved)
                  onDismiss()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("share_download_button"),
              ) {
                Text(
                  text = stringResource(R.string.action_share),
                  fontFamily = SoraFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = NtzBrightCyan,
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("dismiss_download_banner"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.action_dismiss),
                tint = NtzTextSecondary,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}
