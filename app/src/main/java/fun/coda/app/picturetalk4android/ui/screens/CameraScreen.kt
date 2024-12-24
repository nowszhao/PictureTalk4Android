package `fun`.coda.app.picturetalk4android.ui.screens

import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import `fun`.coda.app.picturetalk4android.MainActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraScreen(
    pickImageLauncher: ActivityResultLauncher<String>,
    activity: MainActivity
) {
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER  // 修改缩放类型
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(previewView) {
            previewView?.let { preview ->
                startCamera(
                    activity,
                    preview,
                    lifecycleOwner
                )
            }
        }

        // Control buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = { pickImageLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "选择图片",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Camera button
                IconButton(
                    onClick = { activity.takePhoto() }
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "拍照",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}


private suspend fun startCamera(
    activity: MainActivity,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner
) = suspendCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            // 设置预览
            val preview = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setTargetResolution(Size(720, 1280))  // 16:9 分辨率
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // 设置图片捕获
            val imageCapture = ImageCapture.Builder()
                .setTargetRotation(previewView.display.rotation)
                .setTargetResolution(Size(1080, 1920))  // 更高的拍照分辨率
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            activity.imageCapture = imageCapture

            // 选择后置摄像头
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                continuation.resume(Unit)
            } catch (e: Exception) {
                Log.e("MainActivity", "相机绑定失败", e)
                continuation.resumeWith(Result.failure(e))
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "相机启动失败", e)
            continuation.resumeWith(Result.failure(e))
        }
    }, ContextCompat.getMainExecutor(activity))
}


