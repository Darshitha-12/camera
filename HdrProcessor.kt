package com.camerapixel.camera.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

class HdrProcessor : ImageProcessor {
    override val modeName = "HDR+"

    override fun process(image: Bitmap, params: ProcessParams): Bitmap {
        val result = image.copy(Bitmap.Config.ARGB_8888, true)
        applyToneMapping(result)
        enhanceShadows(result)
        boostSaturation(result)
        return result
    }

    private fun applyToneMapping(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF

            val tr = (255.0 * (r / 255.0).coerceIn(0.0, 1.0).let {
                (it * (1.0 + it / 1.0)) / (it + 1.0)
            }).toInt().coerceIn(0, 255)
            val tg = (255.0 * (g / 255.0).coerceIn(0.0, 1.0).let {
                (it * (1.0 + it / 1.0)) / (it + 1.0)
            }).toInt().coerceIn(0, 255)
            val tb = (255.0 * (b / 255.0).coerceIn(0.0, 1.0).let {
                (it * (1.0 + it / 1.0)) / (it + 1.0)
            }).toInt().coerceIn(0, 255)

            pixels[i] = (pixels[i] and 0xFF000000.toInt()) or (tr shl 16) or (tg shl 8) or tb
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun enhanceShadows(bitmap: Bitmap) {
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1.3f, 0f, 0f, 0f, 10f,
                0f, 1.3f, 0f, 0f, 10f,
                0f, 0f, 1.3f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun boostSaturation(bitmap: Bitmap) {
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 0f,
                0f, 1.2f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
}
