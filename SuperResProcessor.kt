package com.camerapixel.camera.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

class SuperResProcessor : ImageProcessor {
    override val modeName = "Super Res Zoom"

    override fun process(image: Bitmap, params: ProcessParams): Bitmap {
        val zoom = params.zoomFactor.coerceIn(1f, 15f)
        val scaleFactor = 1f + (zoom - 1f) * 0.3f
        val newWidth = (image.width * scaleFactor).toInt()
        val newHeight = (image.height * scaleFactor).toInt()

        val scaled = Bitmap.createScaledBitmap(image, newWidth, newHeight, true)

        val centerCropX = (scaled.width - image.width) / 2
        val centerCropY = (scaled.height - image.height) / 2
        val cropped = Bitmap.createBitmap(
            scaled,
            centerCropX.coerceAtLeast(0),
            centerCropY.coerceAtLeast(0),
            minOf(image.width, scaled.width),
            minOf(image.height, scaled.height)
        )

        val result = cropped.copy(Bitmap.Config.ARGB_8888, true)
        sharpenImage(result)
        return result
    }

    private fun sharpenImage(bitmap: Bitmap) {
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1.3f, 0f, 0f, 0f, -15f,
                0f, 1.3f, 0f, 0f, -15f,
                0f, 0f, 1.3f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            )))
        }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        val edgePaint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0.3f, 0f
            )))
        }
        val overlay = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val overlayCanvas = Canvas(overlay)
        overlayCanvas.drawBitmap(bitmap, 0f, 0f, edgePaint)
        canvas.drawBitmap(overlay, 0f, 0f, Paint().apply { alpha = 60 })
    }
}
