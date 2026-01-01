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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(
    onMediaCaptured: (Uri, Boolean) -> Unit, // Boolean is true if Video, false if Image
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // UI States
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }

    // CameraX Use Cases
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Video Recorder setup
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    // Permissions (Camera + Audio)
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            if (permissionsMap.values.all { it }) {
                // All permissions granted
            } else {
                onCancelled()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) {
            launcher.launch(permissions)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView
            },
            update = { previewView ->
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    // FIX: Use the explicit setter method
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind everything before rebinding to switch modes safely
                    cameraProvider.unbindAll()

                    if (isVideoMode) {
                        // Bind Video
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            videoCapture
                        )
                    } else {
                        // Bind Photo
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", e)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Controls UI (Overlay)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Mode Switcher (Photo / Video)
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
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = if(!isVideoMode) Color.Yellow else Color.White)
                            if(!isVideoMode) Text(" Photo", color = Color.Yellow, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .clickable { isVideoMode = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = if(isVideoMode) Color.Yellow else Color.White)
                            if(isVideoMode) Text(" Video", color = Color.Yellow, fontSize = 12.sp)
                        }
                    }
                }

                // Shutter / Record Button
                IconButton(
                    onClick = {
                        if (isVideoMode) {
                            if (isRecording) {
                                // STOP RECORDING
                                activeRecording?.stop()
                                isRecording = false
                            } else {
                                // START RECORDING
                                isRecording = true
                                activeRecording = recordVideo(context, videoCapture) { uri ->
                                    isRecording = false
                                    onMediaCaptured(uri, true) // True = Video
                                }
                            }
                        } else {
                            // TAKE PHOTO
                            takePhoto(context, imageCapture) { uri ->
                                onMediaCaptured(uri, false) // False = Photo
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(80.dp)
                ) {
                    // Visual change for recording state
                    Box(
                        modifier = Modifier
                            .size(if (isVideoMode && isRecording) 40.dp else 70.dp)
                            .clip(if (isVideoMode && isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(if (isVideoMode) Color.Red else Color.White)
                            .border(2.dp, Color.White, if (isVideoMode && isRecording) RoundedCornerShape(8.dp) else CircleShape)
                    )
                }
            }
        }

        // Back Button
        IconButton(
            onClick = {
                activeRecording?.stop() // Ensure recording stops if we exit
                onCancelled()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Cancel",
                tint = Color.White
            )
        }
    }
}

// --- HELPER FUNCTIONS ---

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
            // Check permission before enabling audio to avoid crashes
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }
        .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    Toast.makeText(context, "Recording Started", Toast.LENGTH_SHORT).show()
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg = "Video saved: ${videoFile.absolutePath}"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onVideoSaved(Uri.fromFile(videoFile))
                    } else {
                        // Handle error (e.g. out of space)
                        Log.e("CameraScreen", "Video capture failed: ${recordEvent.error}")
                        Toast.makeText(context, "Video Error: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
}