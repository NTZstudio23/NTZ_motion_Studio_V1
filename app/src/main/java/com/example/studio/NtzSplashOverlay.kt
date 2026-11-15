package com.example.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NtzBrightCyan
import com.example.ui.theme.NtzDeepInk
import com.example.ui.theme.NtzNeonBlue
import com.example.ui.theme.NtzNeonCyan
import com.example.ui.theme.NtzNeonMagenta
import com.example.ui.theme.NtzNeonPink
import com.example.ui.theme.NtzNeonViolet
import com.example.ui.theme.NtzTextPrimary
import com.example.ui.theme.NtzTextSecondary
import com.example.ui.theme.SoraFontFamily
import com.example.ui.theme.UnboundedFontFamily
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

const val SPLASH_DURATION_MS = 6000

@Composable
fun NtzSplashOverlay(
  visible: Boolean,
  isWebReady: Boolean,
  onSplashFinished: () -> Unit,
) {
  val progress = remember { Animatable(0f) }
  val logoEntrance = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      logoEntrance.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
      )
    }
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = SPLASH_DURATION_MS, easing = LinearEasing),
    )
    onSplashFinished()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
  val glowPulse by
    infiniteTransition.animateFloat(
      initialValue = 0.72f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "glowPulse",
    )

  AnimatedVisibility(
    visible = visible,
    exit = fadeOut(animationSpec = tween(durationMillis = 450)),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(NtzDeepInk)
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                awaitPointerEvent()
              }
            }
          }
          .testTag("splash_overlay"),
      contentAlignment = Alignment.Center,
    ) {
      // Ambient radial neon glows matching the web CSS body background
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(NtzNeonCyan.copy(alpha = 0.20f * glowPulse), Color.Transparent),
              center = Offset(size.width * 0.14f, size.height * 0.10f),
              radius = size.maxDimension * 0.55f,
            ),
          radius = size.maxDimension * 0.55f,
          center = Offset(size.width * 0.14f, size.height * 0.10f),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(NtzNeonPink.copy(alpha = 0.19f * glowPulse), Color.Transparent),
              center = Offset(size.width * 0.88f, size.height * 0.16f),
              radius = size.maxDimension * 0.50f,
            ),
          radius = size.maxDimension * 0.50f,
          center = Offset(size.width * 0.88f, size.height * 0.16f),
        )
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(NtzNeonViolet.copy(alpha = 0.22f * glowPulse), Color.Transparent),
              center = Offset(size.width * 0.50f, size.height * 0.90f),
              radius = size.maxDimension * 0.60f,
            ),
          radius = size.maxDimension * 0.60f,
          center = Offset(size.width * 0.50f, size.height * 0.90f),
        )
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 460.dp)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        // Exact NTZ chromatic-echo N logo mark from the web HTML
        NtzWebLogoCanvas(
          entrance = logoEntrance.value,
          glowPulse = glowPulse,
          modifier = Modifier.size(width = 148.dp, height = 110.dp).testTag("splash_logo"),
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Skewed chromatic echo "NTZ" display wordmark matching web lockup
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "NTZ",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            letterSpacing = 2.sp,
            color = NtzNeonCyan.copy(alpha = 0.85f),
            modifier = Modifier.offset(x = (-2.5).dp),
          )
          Text(
            text = "NTZ",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            letterSpacing = 2.sp,
            color = NtzNeonMagenta.copy(alpha = 0.85f),
            modifier = Modifier.offset(x = 2.5.dp),
          )
          Text(
            text = "NTZ",
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            letterSpacing = 2.sp,
            color = Color.White,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Exact title requested: "NTZ MOTION STUDIO V.1"
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(10.dp))
              .background(
                Brush.horizontalGradient(
                  colors =
                    listOf(
                      NtzNeonBlue.copy(alpha = 0.22f),
                      NtzNeonViolet.copy(alpha = 0.25f),
                      NtzNeonPink.copy(alpha = 0.22f),
                    )
                )
              )
              .border(
                width = 1.dp,
                brush =
                  Brush.horizontalGradient(
                    colors = listOf(NtzNeonCyan.copy(alpha = 0.55f), NtzNeonMagenta.copy(alpha = 0.55f))
                  ),
                shape = RoundedCornerShape(10.dp),
              )
              .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
          Text(
            text = stringResource(R.string.splash_brand_title),
            fontFamily = UnboundedFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.8.sp,
            color = NtzTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("splash_title"),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.splash_subtitle),
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Normal,
          fontSize = 11.sp,
          letterSpacing = 2.4.sp,
          color = NtzTextSecondary,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Timeline ruler line matching web .topbar::after
        TimelineRulerDivider(modifier = Modifier.fillMaxWidth().height(12.dp))

        Spacer(modifier = Modifier.height(20.dp))

        // 6-second progress indicator
        val pct = (progress.value * 100f).roundToInt().coerceIn(0, 100)
        val remainingSec = ((1f - progress.value) * (SPLASH_DURATION_MS / 1000f)).coerceAtLeast(0f)
        val statusText =
          when {
            progress.value < 0.28f -> stringResource(R.string.splash_loading_init)
            progress.value < 0.65f -> stringResource(R.string.splash_loading_engine)
            progress.value < 0.92f -> stringResource(R.string.splash_loading_encoder)
            else ->
              if (isWebReady) stringResource(R.string.splash_loading_ready)
              else stringResource(R.string.splash_loading_encoder)
          }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(99.dp))
              .background(Color(0xFF16132A))
              .border(1.dp, Color(0x33A78BFA), RoundedCornerShape(99.dp))
        ) {
          Box(
            modifier =
              Modifier.fillMaxHeight()
                .fillMaxWidth(fraction = progress.value.coerceIn(0.02f, 1f))
                .clip(RoundedCornerShape(99.dp))
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(NtzNeonCyan, NtzNeonViolet, NtzNeonMagenta)
                  )
                )
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = statusText,
            fontFamily = SoraFontFamily,
            fontSize = 12.sp,
            color = NtzTextSecondary,
            modifier = Modifier.weight(1f).alpha(0.95f),
          )
          Text(
            text = String.format(Locale.US, "%d%% · %.1fs", pct, remainingSec),
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = NtzBrightCyan,
          )
        }
      }
    }
  }
}

@Composable
private fun NtzWebLogoCanvas(
  entrance: Float,
  glowPulse: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    // Web SVG viewBox is 54 x 40
    val scaleX = size.width / 54f
    val scaleY = size.height / 40f
    val s = minOf(scaleX, scaleY)
    val offsetX = (size.width - 54f * s) / 2f
    val offsetY = (size.height - 40f * s) / 2f

    // Subtle halo behind logo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              NtzNeonViolet.copy(alpha = 0.28f * glowPulse),
              NtzNeonCyan.copy(alpha = 0.12f * glowPulse),
              Color.Transparent,
            )
        ),
      radius = size.minDimension * 0.65f,
      center = Offset(size.width * 0.54f, size.height * 0.5f),
    )

    withTransform({
      translate(left = offsetX, top = offsetY)
      scale(scaleX = s, scaleY = s, pivot = Offset.Zero)
    }) {
      // Three speed streaks from the web SVG
      val streakProgress = entrance.coerceIn(0f, 1f)
      val streakBrush =
        Brush.horizontalGradient(
          colors = listOf(NtzNeonCyan.copy(alpha = 0f), NtzNeonCyan),
          startX = 0f,
          endX = 15f,
        )
      drawRoundRect(
        brush = streakBrush,
        topLeft = Offset(15f * (1f - streakProgress), 13f),
        size = Size(15f * streakProgress, 2f),
        cornerRadius = CornerRadius(1f, 1f),
        alpha = streakProgress,
      )
      drawRoundRect(
        brush = streakBrush,
        topLeft = Offset(3f + 12f * (1f - streakProgress), 19f),
        size = Size(12f * streakProgress, 2f),
        cornerRadius = CornerRadius(1f, 1f),
        alpha = streakProgress,
      )
      drawRoundRect(
        brush = streakBrush,
        topLeft = Offset(1f + 14f * (1f - streakProgress), 25f),
        size = Size(14f * streakProgress, 2f),
        cornerRadius = CornerRadius(1f, 1f),
        alpha = streakProgress,
      )

      // Exact N monogram path: M16 34V6h6l12 16V6h6v28h-6L22 18v16z
      val nPath =
        Path().apply {
          moveTo(16f, 34f)
          lineTo(16f, 6f)
          lineTo(22f, 6f)
          lineTo(34f, 22f)
          lineTo(34f, 6f)
          lineTo(40f, 6f)
          lineTo(40f, 34f)
          lineTo(34f, 34f)
          lineTo(22f, 18f)
          lineTo(22f, 34f)
          close()
        }

      // Skew -12 deg: tan(-12 deg) = -0.21255656
      val skewMatrix = Matrix()
      skewMatrix.values[Matrix.SkewX] = -0.21255656f

      val cyanOffset = -3f - 17f * (1f - entrance)
      val magentaOffset = 3f + 17f * (1f - entrance)

      withTransform({
        translate(left = 8.25f, top = 0f)
        transform(skewMatrix)
      }) {
        // Cyan chromatic echo
        withTransform({ translate(left = cyanOffset, top = 0f) }) {
          drawPath(path = nPath, color = NtzNeonCyan.copy(alpha = entrance))
        }
        // Magenta chromatic echo
        withTransform({ translate(left = magentaOffset, top = 0f) }) {
          drawPath(path = nPath, color = NtzNeonMagenta.copy(alpha = 0.85f * entrance))
        }
        // Center face (#ffffff -> #d9ccff)
        drawPath(
          path = nPath,
          brush =
            Brush.linearGradient(
              colors = listOf(Color.White, Color(0xFFD9CCFF)),
              start = Offset(16f, 6f),
              end = Offset(40f, 34f),
            ),
        )
      }
    }
  }
}

@Composable
private fun TimelineRulerDivider(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val yBottom = size.height
    drawLine(
      brush =
        Brush.horizontalGradient(
          colors =
            listOf(
              Color.Transparent,
              NtzNeonCyan,
              NtzNeonViolet,
              NtzNeonMagenta,
              Color.Transparent,
            )
        ),
      start = Offset(0f, yBottom - 1.dp.toPx()),
      end = Offset(size.width, yBottom - 1.dp.toPx()),
      strokeWidth = 1.5.dp.toPx(),
    )
    val step = 12.dp.toPx()
    var x = 0f
    var idx = 0
    while (x <= size.width) {
      val major = idx % 5 == 0
      val tickHeight = if (major) 8.dp.toPx() else 4.dp.toPx()
      val tickAlpha = if (major) 0.55f else 0.28f
      drawLine(
        color = Color(0xFFA78BFA).copy(alpha = tickAlpha),
        start = Offset(x, yBottom),
        end = Offset(x, yBottom - tickHeight),
        strokeWidth = 1.dp.toPx(),
      )
      x += step
      idx++
    }
  }
}
