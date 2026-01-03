@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.secretdiary.ui.theme.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(
    onMediaCaptured: (Uri, Boolean) -> Unit, // true = video, false = image
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    var hasPermissions by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasPermissions = map.values.all { it }
        if (!hasPermissions) onCancelled()
    }

    LaunchedEffect(Unit) {
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) launcher.launch(permissions)
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {

        if (hasPermissions) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        // ✅ setter method (fixes warning)
                        preview.surfaceProvider = previewView.surfaceProvider

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()

                            if (isVideoMode) {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    videoCapture
                                )
                            } else {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Use case binding failed", e)
                            Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting camera permission...", color = Color.White)
            }
        }

        // Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                if (!isRecording) {
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { isVideoMode = false }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = if (!isVideoMode) Color.Yellow else Color.White)
                            if (!isVideoMode) Text(" Photo", color = Color.Yellow, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .clickable { isVideoMode = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Videocam, null, tint = if (isVideoMode) Color.Yellow else Color.White)
                            if (isVideoMode) Text(" Video", color = Color.Yellow, fontSize = 12.sp)
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (!hasPermissions) return@IconButton

                        if (isVideoMode) {
                            if (isRecording) {
                                activeRecording?.stop()
                                activeRecording = null
                                isRecording = false
                            } else {
                                isRecording = true
                                activeRecording = recordVideo(context, videoCapture) { uri ->
                                    activeRecording = null
                                    isRecording = false
                                    onMediaCaptured(uri, true)
                                }
                            }
                        } else {
                            takePhoto(context, imageCapture) { uri ->
                                onMediaCaptured(uri, false)
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isVideoMode && isRecording) 40.dp else 70.dp)
                            .clip(if (isVideoMode && isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(if (isVideoMode) Color.Red else Color.White)
                            .border(
                                2.dp,
                                Color.White,
                                if (isVideoMode && isRecording) RoundedCornerShape(8.dp) else CircleShape
                            )
                    )
                }
            }
        }

        IconButton(
            onClick = {
                activeRecording?.stop()
                activeRecording = null
                isRecording = false
                onCancelled()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = Color.White)
        }
    }
}

// ---------- Helpers ----------

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageSaved: (Uri) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onImageSaved(Uri.fromFile(photoFile))
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "Photo error: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun recordVideo(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    onVideoSaved: (Uri) -> Unit
): Recording {
    val videoFile = File(
        context.filesDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".mp4"
    )

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start ->
                    Toast.makeText(context, "Recording started", Toast.LENGTH_SHORT).show()

                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        onVideoSaved(Uri.fromFile(videoFile))
                    } else {
                        Log.e("CameraScreen", "Video capture failed: ${event.error}")
                        Toast.makeText(context, "Video error: ${event.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
}
