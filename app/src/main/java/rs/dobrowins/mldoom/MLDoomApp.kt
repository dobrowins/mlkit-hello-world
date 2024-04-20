package rs.dobrowins.mldoom

import android.app.Application
import android.content.pm.PackageManager

object NoCameraException : RuntimeException("Phone have no camera. Childish if you axe me")

class MLDoomApp : Application() {

    private fun checkCameraHardware(packageManager: PackageManager): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    override fun onCreate() {
        super.onCreate()
        if (checkCameraHardware(this.packageManager)) throw NoCameraException
    }
}