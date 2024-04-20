package rs.dobrowins.mldoom.domain.models

import com.google.mlkit.vision.face.FaceLandmark

data class FaceState(
    val smilingProbability: Float = 0f,
    val headY: Float = 0f,
    val isLookingToTheRightOfTheCamera: Boolean = false,
    val health: Int = 100,
    val rightEyeOpenProbability: Float = 0f,
    val leftEyeOpenProbability: Float = 0f,
    val isOneEyeOpened: Boolean = false,
    val status: Status = Status.FACE_LEFT,
    val landmarks: List<FaceLandmark> = emptyList(),
)