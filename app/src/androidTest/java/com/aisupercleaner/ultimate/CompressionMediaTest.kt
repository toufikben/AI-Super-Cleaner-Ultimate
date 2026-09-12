package com.aisupercleaner.ultimate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aisupercleaner.ultimate.data.ImageOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CompressionMediaTest {
    @Test fun jpegCodecRoundTripsThroughBitmapFactory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("codec_", ".jpg", context.cacheDir)
        try {
            val source = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
            file.outputStream().use { assertEquals(true, source.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            val decoded = BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull(decoded)
            assertEquals(4, decoded.width)
            assertEquals(3, decoded.height)
            source.recycle()
            decoded.recycle()
        } finally { file.delete() }
    }

    @Test fun exifRotate90IsAppliedToCompressedBitmap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("exif_", ".jpg", context.cacheDir)
        try {
            val source = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
            file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val decoded = BitmapFactory.decodeFile(file.absolutePath)
            val oriented = file.inputStream().use { ImageOrientation.apply(decoded, it) }
            assertEquals(3, oriented.width)
            assertEquals(4, oriented.height)
            source.recycle()
            oriented.recycle()
        } finally { file.delete() }
    }
}
