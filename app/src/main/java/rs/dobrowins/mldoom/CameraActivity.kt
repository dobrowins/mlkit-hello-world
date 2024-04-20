@file:Suppress("UNREACHABLE_CODE")

package rs.dobrowins.mldoom

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalGetImage
class CameraActivity : AppCompatActivity() {

    private val isPreviewEnabled = false
    private val isDebugEnabled = true
    private val isFightMechanicEnabled = true
    private val health = AtomicInteger(100)
    private val isProcessing = AtomicBoolean(false)
    private val framesDropped = AtomicInteger(0)
    private val options: FaceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(PERFORMANCE_MODE_FAST)
        .setClassificationMode(CLASSIFICATION_MODE_ALL)
        .build()
    private val faceDetector = FaceDetection.getClient(options)
    private val executor = Executors.newFixedThreadPool(1)
    private val imageAnalysis: ImageAnalysis = ImageAnalysis.Builder()
        .setImageQueueDepth(1)
        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        fuckOffIfNoPermissionGranted()
    }

    override fun onStart() {
        super.onStart()

        val previewView = findViewById<PreviewView>(R.id.preview_view)
        val imageView = findViewById<ImageView>(R.id.image_view)
        if (isFightMechanicEnabled) {
            imageView.setOnClickListener {
                if (health.get() == 0) {
                    health.set(100)
                    return@setOnClickListener
                }
                health.set((health.get() - (health.get() * 0.13)).toInt())
            }
        }
        val tvSmileProbability = findViewById<TextView>(R.id.tv_smile_prob)
        val tvHeadY = findViewById<TextView>(R.id.tv_y)
        val tvIsLookingRight = findViewById<TextView>(R.id.tv_is_looking_right)
        val tvHealth = findViewById<TextView>(R.id.tv_health)

        val analyzer = ImageAnalysis.Analyzer { imageProxy ->

            fun Image.toBitmap(): Bitmap {
                val yBuffer = planes[0].buffer // Y
                val vuBuffer = planes[2].buffer // VU

                val ySize = yBuffer.remaining()
                val vuSize = vuBuffer.remaining()

                val nv21 = ByteArray(ySize + vuSize)

                yBuffer.get(nv21, 0, ySize)
                vuBuffer.get(nv21, ySize, vuSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
                val imageBytes = out.toByteArray()
                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }

            imageProxy.image?.toBitmap()
                ?.let { bitmap: Bitmap ->
                    val inputImage = InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                    if (isProcessing.get()) {
                        framesDropped.incrementAndGet()
                        return@let
                    }
                    isProcessing.set(true)
                    framesDropped.set(0)
                    faceDetector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            isProcessing.set(false)
                            if (faces.isEmpty()) return@addOnSuccessListener
                            val face = faces.first()
                            val smilingProbability = face.smilingProbability
                            val isLookingToTheRightOfTheCamera = face.headEulerAngleY > 0f

                            if (isDebugEnabled) {
                                tvSmileProbability.visibility = View.VISIBLE
                                tvSmileProbability.text = "Smile probability: ${smilingProbability.toString()}"
                                tvHeadY.text = "Head Y = ${face.headEulerAngleY}"
                                tvHeadY.visibility = View.VISIBLE
                                tvIsLookingRight.text = "Is looking to the right of the camera = $isLookingToTheRightOfTheCamera"
                                tvIsLookingRight.visibility = View.VISIBLE
                                tvHealth.visibility = View.VISIBLE
                                tvHealth.text = "Health: ${health.get()}"
                            }

                            val statusDrawable = when {
                                smilingProbability != null && smilingProbability > 0.8f -> {
                                    if (!isFightMechanicEnabled) {
                                        R.drawable.doomguy_faces_smile
                                    } else {
                                        when (health.get()) {
                                            in 0..40 -> R.drawable.doomguy_faces_smile_bloody_more_carnage
                                            in 41..70 -> R.drawable.doomguy_faces_smile_bloody_more
                                            in 71..90 -> R.drawable.doomguy_faces_smile_bloody
                                            else -> R.drawable.doomguy_faces_smile
                                        }
                                    }
                                }

                                isLookingToTheRightOfTheCamera -> {
                                    R.drawable.doomguy_faces_norm_right
                                }

                                else -> {
                                    R.drawable.doomguy_faces_norm_left
                                }
                            }
                            imageView.setImageResource(statusDrawable)
                            Log.d("FRAMES DROPPED", "${framesDropped.get()}")
                        }
                        .addOnFailureListener { exception ->
                            isProcessing.set(false)
                            exception.printStackTrace()
                            Log.d("FRAMES DROPPED", "${framesDropped.get()}")
                        }
                }
            imageProxy.close()
        }

        imageAnalysis.setAnalyzer(executor, analyzer)

        onProcessCameraProvided { result ->
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            // bind analyzor to lifecycle
            if (isPreviewEnabled) {
                val preview: Preview = Preview.Builder()
                    .setTargetFrameRate(Range(0, 24))
                    .build()
                previewView.visibility = View.VISIBLE
                preview.setSurfaceProvider(previewView!!.surfaceProvider)
                result.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)
            } else {
                result.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis)
            }
        }(this)
    }

    override fun onStop() {
        super.onStop()
        imageAnalysis.clearAnalyzer()
    }

    private fun fuckOffIfNoPermissionGranted() {
        val cameraPermission = "android.permission.CAMERA"
        val isCameraPermissionGranted = checkSelfPermission(cameraPermission) == PERMISSION_GRANTED
        if (!isCameraPermissionGranted) {
            requestPermissions(arrayOf(cameraPermission), 69420)
        }
    }
}

