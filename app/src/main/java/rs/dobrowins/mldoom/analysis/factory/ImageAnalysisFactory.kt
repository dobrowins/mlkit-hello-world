package rs.dobrowins.mldoom.analysis.factory

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST
import rs.dobrowins.mldoom.analysis.FacesAnalyzer
import java.util.concurrent.Executors

object ImageAnalysisFactory {

    private fun imageAnalysis(): ImageAnalysis = ImageAnalysis.Builder()
        .setImageQueueDepth(1)
        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private val detectors = mapOf(
        AnalysisType.CLASSIFICATION to {
            val options: FaceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(PERFORMANCE_MODE_FAST)
                .setClassificationMode(CLASSIFICATION_MODE_ALL)
                .build()
            FaceDetection.getClient(options)
        },
        AnalysisType.LANDMARK to {
            val options: FaceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(PERFORMANCE_MODE_FAST)
                .setLandmarkMode(LANDMARK_MODE_ALL)
                .build()
            FaceDetection.getClient(options)
        },
    )

    private val executor get() = Executors.newSingleThreadExecutor()

    fun create(analysisType: AnalysisType, onSuccess: (List<Face>) -> Unit): ImageAnalysis {
        val imageAnalysis = imageAnalysis()
        val classificationAnalyzer = FacesAnalyzer(detectors[analysisType]!!.invoke(), onSuccess = onSuccess)
        imageAnalysis.setAnalyzer(executor, classificationAnalyzer)
        return imageAnalysis
    }
}