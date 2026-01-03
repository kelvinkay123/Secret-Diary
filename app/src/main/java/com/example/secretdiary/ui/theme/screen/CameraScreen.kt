@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.secretdiary.ui.theme.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
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

    // ✅ Front/Back switch
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    // ✅ Timer (HH:MM:SS) - no auto-stop
    var recordingStartMs by remember { mutableStateOf<Long?>(null) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRecording, recordingStartMs) {
        if (!isRecording || recordingStartMs == null) return@LaunchedEffect
        while (isRecording) {
            elapsedMs = System.currentTimeMillis() - (recordingStartMs ?: System.currentTimeMillis())
            delay(250)
        }
    }

    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    var hasCameraPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasCameraPermission = map[Manifest.permission.CAMERA] == true
        if (!hasCameraPermission) onCancelled()
    }

    LaunchedEffect(Unit) {
        hasCameraPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) launcher.launch(permissions)
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    // ✅ HD is more compatible than HIGHEST on many devices
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // ✅ Keep ONE preview view instance
    val previewView = remember { PreviewView(context) }

    fun resetRecordingUi() {
        recordingStartMs = null
        elapsedMs = 0L
    }

    // ✅ Stop recording if leaving screen (prevents finalize issues)
    DisposableEffect(Unit) {
        onDispose {
            try {
                activeRecording?.stop()
            } catch (_: Exception) {
            }
            activeRecording = null
            isRecording = false
            resetRecordingUi()
        }
    }

    // ✅ If user switches mode while recording, stop safely
    LaunchedEffect(isVideoMode) {
        if (isRecording) {
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
            resetRecordingUi()
        }
    }

    // ✅ If user switches camera while recording, stop safely
    LaunchedEffect(lensFacing) {
        if (isRecording) {
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
            resetRecordingUi()
        }
    }

    // ✅ Bind camera only when permission/mode/lens changes (NOT on every recomposition)
    LaunchedEffect(hasCameraPermission, isVideoMode, lensFacing) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

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
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Requesting camera permission...", color = Color.White)
            }
        }

        // ✅ Small red timer at top center (00:00:00)
        if (isVideoMode && isRecording) {
            val totalSeconds = (elapsedMs / 1000).toInt()
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            val timeText = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Red.copy(alpha = 0.90f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }

        // ✅ Back (top-left)
        IconButton(
            onClick = {
                activeRecording?.stop()
                activeRecording = null
                isRecording = false
                resetRecordingUi()
                onCancelled()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Cancel",
                tint = Color.White
            )
        }

        // ✅ Switch camera (top-right) - BIGGER + pushed down a bit
        IconButton(
            onClick = {
                if (isRecording) return@IconButton
                lensFacing =
                    if (lensFacing == CameraSelector.LENS_FACING_BACK)
                        CameraSelector.LENS_FACING_FRONT
                    else
                        CameraSelector.LENS_FACING_BACK
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 16.dp) // pushed down
                .size(54.dp) // bigger touch area
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(30.dp) // bigger icon
            )
        }

        // Controls (bottom)
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
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (!isVideoMode) Color.Yellow else Color.White
                            )
                            if (!isVideoMode) Text(" Photo", color = Color.Yellow, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier
                                .clickable { isVideoMode = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isVideoMode) Color.Yellow else Color.White
                            )
                            if (isVideoMode) Text(" Video", color = Color.Yellow, fontSize = 12.sp)
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (!hasCameraPermission) return@IconButton

                        if (isVideoMode) {
                            if (isRecording) {
                                activeRecording?.stop()
                                activeRecording = null
                                isRecording = false
                                resetRecordingUi()
                            } else {
                                isRecording = true
                                recordingStartMs = System.currentTimeMillis()
                                elapsedMs = 0L

                                activeRecording = recordVideo(
                                    context = context,
                                    videoCapture = videoCapture,
                                    onVideoSaved = { uri ->
                                        activeRecording = null
                                        isRecording = false
                                        resetRecordingUi()
                                        onMediaCaptured(uri, true)
                                    },
                                    onVideoError = { msg ->
                                        activeRecording = null
                                        isRecording = false
                                        resetRecordingUi()
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
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
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
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
    onVideoSaved: (Uri) -> Unit,
    onVideoError: (String) -> Unit
): Recording {
    // ✅ App-private external movies dir (NOT Gallery)
    val videoDir =
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir

    val videoFile = File(
        videoDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"
    )

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    val hasAudioPermission =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .apply { if (hasAudioPermission) withAudioEnabled() }
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        onVideoSaved(Uri.fromFile(videoFile))
                    } else {
                        Log.e("CameraScreen", "Video capture failed: ${event.error}", event.cause)
                        onVideoError("Video error: ${event.error}")
                    }
                }
                else -> Unit
            }
        }
}
