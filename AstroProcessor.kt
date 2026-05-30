package com.camerapixel.camera.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

class AstroProcessor : ImageProcessor {
    override val modeName = "Astrophotography"

    override fun process(image: Bitmap, params: ProcessParams): Bitmap {
        val result = image.copy(Bitmap.Config.ARGB_8888, true)
        boostSkyDetails(result)
        reduceLightPollution(result)
        enhanceStars(result)
        return result
    }

    private fun boostSkyDetails(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1.5f, 0f, 0f, 0f, 10f,
                0f, 1.5f, 0f, 0f, 10f,
                0f, 0f, 2.0f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun reduceLightPollution(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF

            val avg = (r + g + b) / 3
            val isOrangeTint = r > g + 15 && r > b + 25

            if (isOrangeTint) {
                val newR = (r * 0.7f).toInt()
                val newG = (g * 0.9f).toInt()
                pixels[i] = (pixels[i] and 0xFF000000.toInt()) or
                    (newR.coerceIn(0, 255) shl 16) or
                    (newG.coerceIn(0, 255) shl 8) or
                    b.coerceIn(0, 255)
            }

            val newR = (r * 0.9f).toInt()
            val newG = (g * 0.95f).toInt()
            val newB = minOf(255, (b * 1.2f).toInt())
            pixels[i] = (pixels[i] and 0xFF000000.toInt()) or
                (newR.coerceIn(0, 255) shl 16) or
                (newG.coerceIn(0, 255) shl 8) or
                (newB.coerceIn(0, 255))
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun enhanceStars(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val width = bitmap.width

        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF

            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

            if (luminance > 180) {
                val boost = 1.3f
                val newR = minOf(255, (r * boost).toInt())
                val newG = minOf(255, (g * boost).toInt())
                val newB = minOf(255, (b * boost).toInt())
                pixels[i] = (pixels[i] and 0xFF000000.toInt()) or
                    (newR shl 16) or (newG shl 8) or newB
            }
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }
}
