package com.camerapixel.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.camerapixel.camera.processing.AstroProcessor
import com.camerapixel.camera.processing.HdrProcessor
import com.camerapixel.camera.processing.ImageProcessor
import com.camerapixel.camera.processing.NightSightProcessor
import com.camerapixel.camera.processing.PortraitProcessor
import com.camerapixel.camera.processing.ProcessParams
import com.camerapixel.camera.processing.SuperResProcessor
import com.camerapixel.camera.processing.toBitmap
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CameraMode(val processor: ImageProcessor) {
    PHOTO(HdrProcessor()),
    HDR(HdrProcessor()),
    NIGHT(NightSightProcessor()),
    PORTRAIT(PortraitProcessor()),
    ASTRO(AstroProcessor()),
    SUPER_RES(SuperResProcessor())
}

class CameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var currentMode = CameraMode.PHOTO
    var currentZoom = 1.0f
    var flashMode = ImageCapture.FLASH_MODE_AUTO

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onCameraReady: () -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera(lifecycleOwner, previewView)
            onCameraReady()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .setTargetResolution(Size(1920, 1080))
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture!!)
            .build()

        try {
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCamera(lifecycleOwner, previewView)
    }

    fun setMode(mode: CameraMode) {
        currentMode = mode
    }

    fun setZoom(zoom: Float) {
        currentZoom = zoom.coerceIn(1f, 15f)
    }

    fun toggleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        imageCapture?.flashMode = flashMode
    }

    fun capturePhoto(
        onPhotoSaved: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Camera not ready")
            return
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processAndSaveImage(image, onPhotoSaved, onError)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError("Capture failed: ${exception.message}")
                }
            }
        )
    }

    private fun processAndSaveImage(
        imageProxy: ImageProxy,
        onPhotoSaved: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        cameraExecutor.execute {
            try {
                val params = ProcessParams(
                    iso = 100,
                    exposureTime = 30000000,
                    zoomFactor = currentZoom,
                    sceneBrightness = 0.5f
                )

                val processed = currentMode.processor.process(bitmap, params)

                val finalBitmap = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    val matrix = Matrix().apply { postScale(-1f, 1f) }
                    Bitmap.createBitmap(processed, 0, 0, processed.width, processed.height, matrix, true)
                } else {
                    processed
                }

                val path = saveToGallery(finalBitmap)
                onPhotoSaved(path)
            } catch (e: Exception) {
                onError("Processing failed: ${e.message}")
            }
        }
    }

    private fun saveToGallery(bitmap: Bitmap): String {
        val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CameraPixel")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(outputStream.toByteArray())
                }
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
                return filename
            }
        }

        val picturesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val appDir = java.io.File(picturesDir, "CameraPixel")
        appDir.mkdirs()
        val file = java.io.File(appDir, filename)
        file.outputStream().use { it.write(outputStream.toByteArray()) }
        return file.absolutePath
    }

    fun cleanup() {
        cameraExecutor.shutdown()
    }
}
