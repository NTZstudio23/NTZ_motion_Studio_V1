package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val UnboundedFontFamily = FontFamily(
  Font(R.font.unbounded, FontWeight.Normal),
  Font(R.font.unbounded, FontWeight.Medium),
  Font(R.font.unbounded, FontWeight.Bold),
  Font(R.font.unbounded, FontWeight.Black),
)

val SoraFontFamily = FontFamily(
  Font(R.font.sora, FontWeight.Normal),
  Font(R.font.sora, FontWeight.SemiBold),
  Font(R.font.sora, FontWeight.Bold),
)

val JetBrainsMonoFontFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val Typography =
  Typography(
    displaySmall =
      TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        lineHeight = 32.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 2.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = SoraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = SoraFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
      ),
  )
