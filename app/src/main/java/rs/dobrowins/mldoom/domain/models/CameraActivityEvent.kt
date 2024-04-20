package rs.dobrowins.mldoom.domain.models

sealed interface CameraActivityEvent {
    class Error(val throwable: Throwable) : CameraActivityEvent
}