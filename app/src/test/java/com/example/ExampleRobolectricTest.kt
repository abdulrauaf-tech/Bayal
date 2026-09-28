package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine3d.Vector3
import com.example.game.StormZone
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Battle Royale 3D", appName)
  }

  @Test
  fun `vector3 distance calculation`() {
    val v1 = Vector3(0f, 0f, 0f)
    val v2 = Vector3(3f, 4f, 0f)
    assertEquals(5f, v1.distanceTo(v2), 0.001f)
  }

  @Test
  fun `storm zone inside check`() {
    val storm = StormZone(center = Vector3(0f, 0f, 0f), currentRadius = 50f)
    assertTrue(storm.isInside(Vector3(10f, 0f, 10f)))
  }
}
