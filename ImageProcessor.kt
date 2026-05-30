package com.camerapixel.camera.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

interface ImageProcessor {
    fun process(image: Bitmap, params: ProcessParams): Bitmap
    val modeName: String
}

data class ProcessParams(
    val iso: Int = 0,
    val exposureTime: Long = 0,
    val zoomFactor: Float = 1f,
    val sceneBrightness: Float = 0.5f,
    val faceDetected: Boolean = false
)

fun ImageProxy.toBitmap(): Bitmap {
    val yuvImage = when (format) {
        ImageFormat.YUV_420_888 -> {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)

            val uPixelStride = planes[1].pixelStride
            val vPixelStride = planes[2].pixelStride
            val uRowStride = planes[1].rowStride
            val vRowStride = planes[2].rowStride

            val uvWidth = (width + 1) / 2
            val uvHeight = (height + 1) / 2

            var uvOffset = ySize
            for (row in 0 until uvHeight) {
                val uRow = uBuffer.position(row * uRowStride)
                val vRow = vBuffer.position(row * vRowStride)
                for (col in 0 until uvWidth) {
                    nv21[uvOffset + col * 2] = vBuffer.get(vRow + col * vPixelStride)
                    nv21[uvOffset + col * 2 + 1] = uBuffer.get(uRow + col * uPixelStride)
                }
                uvOffset += uvWidth * 2
            }

            YuvImage(nv21, ImageFormat.NV21, width, height, null)
        }
        else -> {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
