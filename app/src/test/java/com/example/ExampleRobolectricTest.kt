package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.studio.NtzFileSaver
import com.example.studio.SPLASH_DURATION_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name and splash branding match requirements`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("NTZ Motion Studio", context.getString(R.string.app_name))
    assertEquals("NTZ MOTION STUDIO V.1", context.getString(R.string.splash_brand_title))
    assertEquals(6000, SPLASH_DURATION_MS)
  }

  @Test
  fun `web asset is bundled in assets`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val html = context.assets.open("ntz_motion_studio.html").bufferedReader().use { it.readText() }
    assertTrue(html.contains("NTZ MOTION STUDIO V.1 PRO"))
  }

  @Test
  fun `file saver resolves mime types and sanitizes names`() {
    assertEquals("video/mp4", NtzFileSaver.resolveMimeType("demo.mp4", null))
    assertEquals("video/webm", NtzFileSaver.resolveMimeType("demo.webm", null))
    assertEquals("video/quicktime", NtzFileSaver.resolveMimeType("demo.mov", null))
    assertEquals("text/csv", NtzFileSaver.resolveMimeType("adobe-stock.csv", null))
    assertEquals("my_video.mp4", NtzFileSaver.sanitizeFileName("my:video.mp4", "mp4"))
  }
}
