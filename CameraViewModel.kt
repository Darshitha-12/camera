package com.camerapixel.ui

import android.app.Application
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.camerapixel.camera.CameraManager
import com.camerapixel.camera.CameraMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraManager = CameraManager(application)

    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    private val _currentMode = MutableStateFlow(CameraMode.PHOTO)
    val currentMode: StateFlow<CameraMode> = _currentMode.asStateFlow()

    private val _flashState = MutableStateFlow(0)
    val flashState: StateFlow<Int> = _flashState.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _lastPhotoPath = MutableStateFlow<String?>(null)
    val lastPhotoPath: StateFlow<String?> = _lastPhotoPath.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _cameraReady = MutableStateFlow(false)
    val cameraReady: StateFlow<Boolean> = _cameraReady.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startCamera(owner: LifecycleOwner, view: PreviewView) {
        lifecycleOwner = owner
        previewView = view
        cameraManager.startCamera(owner, view) {
            _cameraReady.value = true
        }
    }

    fun switchCamera() {
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        cameraManager.switchCamera(owner, view)
    }

    fun setMode(mode: CameraMode) {
        cameraManager.setMode(mode)
        _currentMode.value = mode
    }

    fun setZoom(zoom: Float) {
        cameraManager.setZoom(zoom)
        _zoomLevel.value = zoom
    }

    fun toggleFlash() {
        cameraManager.toggleFlash()
        _flashState.value = when (cameraManager.flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> 0
            ImageCapture.FLASH_MODE_ON -> 1
            else -> 2
        }
    }

    fun capturePhoto() {
        if (_isCapturing.value) return
        _isCapturing.value = true
        cameraManager.capturePhoto(
            onPhotoSaved = { path ->
                _lastPhotoPath.value = path
                _isCapturing.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isCapturing.value = false
            }
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.cleanup()
    }
}
