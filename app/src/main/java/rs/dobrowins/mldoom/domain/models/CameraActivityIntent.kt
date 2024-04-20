package rs.dobrowins.mldoom.domain.models

import com.google.mlkit.vision.face.Face

sealed interface CameraActivityIntent {
    object StatusDrawableClick : CameraActivityIntent
    class UpdateAnalysis(val faces: List<Face>) : CameraActivityIntent
    class UpdateLandmarks(val faces: List<Face>) : CameraActivityIntent
}