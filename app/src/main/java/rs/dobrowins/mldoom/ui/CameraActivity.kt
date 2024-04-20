package rs.dobrowins.mldoom.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import rs.dobrowins.mldoom.R
import rs.dobrowins.mldoom.analysis.factory.AnalysisType
import rs.dobrowins.mldoom.analysis.factory.ImageAnalysisFactory
import rs.dobrowins.mldoom.andThen
import rs.dobrowins.mldoom.domain.CameraActivityViewModel
import rs.dobrowins.mldoom.domain.models.CameraActivityIntent
import rs.dobrowins.mldoom.domain.models.FaceState
import rs.dobrowins.mldoom.domain.models.Status

@ExperimentalGetImage
class CameraActivity : AppCompatActivity() {

    private val isDebugEnabled = true

    private val vm = CameraActivityViewModel()

    private val classificationImageAnalysis = ImageAnalysisFactory.create(
        analysisType = AnalysisType.CLASSIFICATION,
        onSuccess = CameraActivityIntent::UpdateAnalysis andThen vm::handleIntent,
    )

    private val landmarkImageAnalysis = ImageAnalysisFactory.create(
        analysisType = AnalysisType.LANDMARK,
        onSuccess = CameraActivityIntent::UpdateLandmarks andThen vm::handleIntent,
    )

    private val tvSmileProbability get() = findViewById<TextView>(R.id.tv_smile_prob)
    private val tvHeadY get() = findViewById<TextView>(R.id.tv_y)
    private val tvIsLookingRight get() = findViewById<TextView>(R.id.tv_is_looking_right)
    private val tvHealth get() = findViewById<TextView>(R.id.tv_health)
    private val tvEyeStatus get() = findViewById<TextView>(R.id.tv_eye_status)
    private val ivStatusDrawable get() = findViewById<ImageView>(R.id.image_view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        checkCameraPermissions()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.state
                        .buffer(capacity = CONFLATED)
                        .distinctUntilChanged()
                        .collectLatest(::showState)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        bindAnalysisToLifecycle(classificationImageAnalysis, landmarkImageAnalysis).invoke(this)

        ivStatusDrawable.setOnClickListener {
            vm.handleIntent(CameraActivityIntent.StatusDrawableClick)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindAnalysisFromLifecycle()(this)
    }

    @SuppressLint("SetTextI18n")
    private fun showState(faceState: FaceState) {

        val smilingProbability = faceState.smilingProbability
        val headY = faceState.headY
        val isLookingToTheRightOfTheCamera = faceState.isLookingToTheRightOfTheCamera
        val health = faceState.health
        val rightEyeOpenProb = faceState.rightEyeOpenProbability
        val leftEyeOpenProb = faceState.leftEyeOpenProbability
        val isOneEyeOpened = faceState.isOneEyeOpened

        if (isDebugEnabled) {
            tvSmileProbability.visibility = View.VISIBLE
            tvSmileProbability.text = "Smile probability: $smilingProbability"
            tvHeadY.text = "Head Y = $headY"
            tvHeadY.visibility = View.VISIBLE
            tvIsLookingRight.text = "Is looking to the right of the camera = $isLookingToTheRightOfTheCamera"
            tvIsLookingRight.visibility = View.VISIBLE
            tvHealth.visibility = View.VISIBLE
            tvHealth.text = "Health: $health"
            tvEyeStatus.visibility = View.VISIBLE
            tvEyeStatus.text = "leop = $leftEyeOpenProb, reop = $rightEyeOpenProb, isOneEyeOpen = $isOneEyeOpened"
        }

        val statusDrawable = when (faceState.status) {
            Status.FACE_LEFT -> R.drawable.doomguy_faces_norm_left
            Status.FACE_RIGHT -> R.drawable.doomguy_faces_norm_right
            Status.SMILE -> R.drawable.doomguy_faces_smile
            Status.GOD -> R.drawable.doomguy_faces_godmode
            Status.BLOODY -> R.drawable.doomguy_faces_smile_bloody
            Status.BLOODY_MORE -> R.drawable.doomguy_faces_smile_bloody_more
            Status.CARNAGE -> R.drawable.doomguy_faces_smile_bloody_more_carnage
        }
        ivStatusDrawable.setImageResource(statusDrawable)
    }

    private fun checkCameraPermissions() {
        val cameraPermission = "android.permission.CAMERA"
        val isCameraPermissionGranted = checkSelfPermission(cameraPermission) == PERMISSION_GRANTED
        if (!isCameraPermissionGranted) {
            requestPermissions(arrayOf(cameraPermission), 69420)
        }
    }

    private fun unbindAnalysisFromLifecycle() = ProviderCallback(
        future = ProcessCameraProvider.getInstance(this),
        onSuccess = ProcessCameraProvider::unbindAll
    )

    private fun bindAnalysisToLifecycle(vararg imageAnalysis: ImageAnalysis) = ProviderCallback(
        future = ProcessCameraProvider.getInstance(this),
        onSuccess = { processCameraProvider: ProcessCameraProvider ->
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            processCameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, *imageAnalysis)
        }
    )
}

