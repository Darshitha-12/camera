package com.camerapixel.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.camerapixel.camera.CameraMode
import com.camerapixel.ui.theme.PixelBlue

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val currentMode by viewModel.currentMode.collectAsState()
    val flashState by viewModel.flashState.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val lastPhotoPath by viewModel.lastPhotoPath.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val cameraReady by viewModel.cameraReady.collectAsState()

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission && cameraReady) {
            CameraPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            ZoomSlider(
                zoom = zoomLevel,
                onZoomChange = { viewModel.setZoom(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlashButton(flashState) { viewModel.toggleFlash() }
                    SwitchCameraButton { viewModel.switchCamera() }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModeSelector(
                    currentMode = currentMode,
                    onModeSelected = { viewModel.setMode(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GalleryThumbnail(
                        lastPhotoPath = lastPhotoPath,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.width(48.dp))

                    CaptureButton(
                        isCapturing = isCapturing,
                        onClick = { viewModel.capturePhoto() }
                    )

                    Spacer(modifier = Modifier.width(48.dp))

                    Box(modifier = Modifier.size(48.dp))
                }
            }
        }

        if (!cameraReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Starting camera...",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                post {
                    viewModel.startCamera(this@apply as androidx.lifecycle.LifecycleOwner, this)
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ModeSelector(
    currentMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x99000000))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CameraMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            val bgColor by animateColorAsState(
                if (isSelected) PixelBlue else Color.Transparent,
                label = "modeBg"
            )

            Text(
                text = when (mode) {
                    CameraMode.PHOTO -> "Photo"
                    CameraMode.HDR -> "HDR+"
                    CameraMode.NIGHT -> "Night"
                    CameraMode.PORTRAIT -> "Portrait"
                    CameraMode.ASTRO -> "Astro"
                    CameraMode.SUPER_RES -> "Zoom"
                },
                color = if (isSelected) Color.White else Color(0xAAFFFFFF),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CaptureButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(enabled = !isCapturing) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isCapturing) Color(0x66FFFFFF) else Color.White)
        )
    }
}

@Composable
private fun FlashButton(
    flashState: Int,
    onClick: () -> Unit
) {
    val icon = when (flashState) {
        0 -> Icons.Default.FlashAuto
        1 -> Icons.Default.FlashOn
        else -> Icons.Default.FlashOff
    }

    IconButton(icon = icon, contentDescription = "Flash", onClick = onClick)
}

@Composable
private fun SwitchCameraButton(onClick: () -> Unit) {
    IconButton(
        icon = Icons.Default.CameraFront,
        contentDescription = "Switch camera",
        onClick = onClick
    )
}

@Composable
private fun GalleryThumbnail(
    lastPhotoPath: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (lastPhotoPath != null) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
private fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x66000000))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ZoomSlider(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${zoom.toInt()}x",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = zoom,
            onValueChange = onZoomChange,
            valueRange = 1f..15f,
            modifier = Modifier
                .height(200.dp)
                .width(32.dp),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = PixelBlue,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
