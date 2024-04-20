package rs.dobrowins.mldoom.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rs.dobrowins.mldoom.domain.models.CameraActivityIntent
import rs.dobrowins.mldoom.domain.models.FaceState
import rs.dobrowins.mldoom.domain.models.Status
import java.util.concurrent.atomic.AtomicInteger

class CameraActivityViewModel {

    private val isFightMechanicEnabled = true
    private val health = AtomicInteger(100)

    private val _state = MutableStateFlow(FaceState())
    val state = _state as StateFlow<FaceState>

    fun handleIntent(intent: CameraActivityIntent) {
        when (intent) {
            CameraActivityIntent.StatusDrawableClick -> {
                if (!isFightMechanicEnabled) return
                if (health.get() <= 0) {
                    health.set(100)
                    return
                }
                health.set(health.get() - 17)
                _state.value = _state.value.copy(health = health.get())
            }

            is CameraActivityIntent.UpdateLandmarks -> {
                val faces = intent.faces
                if (faces.isEmpty()) {
                    return
                }
                val landmarks = faces.first().allLandmarks
                _state.value = _state.value.copy(landmarks = landmarks)
            }

            is CameraActivityIntent.UpdateAnalysis -> {
                val faces = intent.faces
                if (faces.isEmpty()) {
                    return
                }

                val face = faces.first()
                val thresholdEyeOpen = 0.30f
                val rightEyeOpenProb = face.rightEyeOpenProbability ?: 0f
                val leftEyeOpenProb = face.leftEyeOpenProbability ?: 0f
                val isOneEyeOpened = rightEyeOpenProb < thresholdEyeOpen && leftEyeOpenProb > thresholdEyeOpen
                        || rightEyeOpenProb > thresholdEyeOpen && leftEyeOpenProb < thresholdEyeOpen
                val smilingProbability = face.smilingProbability ?: 0f
                val isLookingToTheRightOfTheCamera = face.headEulerAngleY > 0f

                val status = when {
                    isOneEyeOpened -> {
                        Status.GOD
                    }

                    smilingProbability > 0.8f -> {
                        if (!isFightMechanicEnabled) {
                            Status.SMILE
                        } else {
                            when (health.get()) {
                                in 0..40 -> Status.CARNAGE
                                in 41..70 -> Status.BLOODY_MORE
                                in 71..90 -> Status.BLOODY
                                else -> Status.SMILE
                            }
                        }
                    }

                    isLookingToTheRightOfTheCamera -> {
                        Status.FACE_RIGHT
                    }

                    else -> {
                        Status.FACE_LEFT
                    }
                }

                val faceData = FaceState(
                    smilingProbability = smilingProbability,
                    headY = face.headEulerAngleY,
                    isLookingToTheRightOfTheCamera = isLookingToTheRightOfTheCamera,
                    health = health.get(),
                    rightEyeOpenProbability = rightEyeOpenProb,
                    leftEyeOpenProbability = leftEyeOpenProb,
                    status = status,
                )
                _state.value = faceData
            }
        }
    }
}
