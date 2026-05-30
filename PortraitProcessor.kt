package com.camerapixel.camera.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PortraitProcessor : ImageProcessor {
    override val modeName = "Portrait"

    override fun process(image: Bitmap, params: ProcessParams): Bitmap {
        val result = image.copy(Bitmap.Config.ARGB_8888, true)
        val centerX = image.width / 2f
        val centerY = image.height / 2f
        applyBokehEffect(result, centerX, centerY)
        enhanceSkinTones(result)
        return result
    }

    private fun applyBokehEffect(bitmap: Bitmap, centerX: Float, centerY: Float) {
        val radius = min(bitmap.width, bitmap.height) * 0.35f
        val canvas = Canvas(bitmap)

        val bokehPaint = Paint().apply {
            shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(0x00000000, 0x00000000, 0x44000000),
                floatArrayOf(0.5f, 0.7f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), bokehPaint)

        val blurPaint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 0.3f, 0f
            )))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, blurPaint)
    }

    private fun enhanceSkinTones(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF

            val maxVal = maxOf(r, g, b)
            val minVal = minOf(r, g, b)
            val diff = maxVal - minVal

            if (diff < 40 && r > 60 && g > 40 && b > 30) {
                val newR = min(255, r + 8)
                val newG = min(255, (g * 1.05f).toInt())
                pixels[i] = (pixels[i] and 0xFF000000.toInt()) or (newR shl 16) or (newG shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }
}
