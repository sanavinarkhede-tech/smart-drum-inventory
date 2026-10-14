package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InventoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart Sealer", appName)
  }

  @Test
  fun `verify inventory repository initializes default drums`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = InventoryRepository(context)
    val drums = repo.drums.value
    assertTrue("Should have initialized at least 6 drums", drums.size >= 6)
  }
}
