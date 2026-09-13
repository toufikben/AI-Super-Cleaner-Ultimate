package com.aisupercleaner.ultimate.core.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object ImageHasher {

    private const val HASH_WIDTH = 9
    private const val HASH_HEIGHT = 8

    fun dHash(file: File): Long? = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var sampleSize = 1
        while (options.outWidth / (sampleSize * 2) >= HASH_WIDTH * 4 &&
            options.outHeight / (sampleSize * 2) >= HASH_HEIGHT * 4
        ) { sampleSize *= 2 }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, HASH_WIDTH, HASH_HEIGHT, true)
        val hash = computeDHash(scaled)
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()
        hash
    }.getOrNull()

    private fun computeDHash(bitmap: Bitmap): Long {
        val pixels = IntArray(HASH_WIDTH * HASH_HEIGHT)
        bitmap.getPixels(pixels, 0, HASH_WIDTH, 0, 0, HASH_WIDTH, HASH_HEIGHT)

        val gray = IntArray(pixels.size) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            (r * 299 + g * 587 + b * 114) / 1000
        }

        var hash = 0L
        var bitIndex = 0
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                if (gray[y * HASH_WIDTH + x] > gray[y * HASH_WIDTH + x + 1]) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun areSimilar(hashA: Long, hashB: Long, threshold: Int = 10): Boolean =
        hammingDistance(hashA, hashB) <= threshold
}
