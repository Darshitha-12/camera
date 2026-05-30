package com.camerapixel.camera.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

class NightSightProcessor : ImageProcessor {
    override val modeName = "Night Sight"

    override fun process(image: Bitmap, params: ProcessParams): Bitmap {
        val result = image.copy(Bitmap.Config.ARGB_8888, true)
        brightenImage(result)
        reduceNoise(result)
        enhanceDetails(result)
        return result
    }

    private fun brightenImage(bitmap: Bitmap) {
        val brightnessBoost = 1.8f
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                brightnessBoost, 0f, 0f, 0f, 30f,
                0f, brightnessBoost, 0f, 0f, 30f,
                0f, 0f, brightnessBoost, 0f, 30f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun reduceNoise(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val width = bitmap.width
        val height = bitmap.height

        val smoothed = IntArray(pixels.size)
        val radius = 1

        for (y in 0 until height) {
            for (x in 0 until width) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0

                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val pixel = pixels[ny * width + nx]
                        rSum += (pixel shr 16) and 0xFF
                        gSum += (pixel shr 8) and 0xFF
                        bSum += pixel and 0xFF
                        count++
                    }
                }

                val idx = y * width + x
                val a = pixels[idx] and 0xFF000000.toInt()
                smoothed[idx] = a or
                    ((rSum / count) shl 16) or
                    ((gSum / count) shl 8) or
                    (bSum / count)
            }
        }
        bitmap.setPixels(smoothed, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun enhanceDetails(bitmap: Bitmap) {
        val unsharpMask = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0.5f, 0f
            )))
        }
        val overlay = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(overlay)
        canvas.drawBitmap(bitmap, 0f, 0f, unsharpMask)

        val resultCanvas = Canvas(bitmap)
        resultCanvas.drawBitmap(overlay, 0f, 0f, Paint().apply {
            alpha = 80
        })
    }
}
