package rs.dobrowins.mldoom

import android.app.Activity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures

inline fun onProcessCameraProvided(crossinline onSuccess: (ProcessCameraProvider) -> Unit): Activity.() -> Unit = {
    Futures.addCallback(
        ProcessCameraProvider.getInstance(baseContext),
        object : FutureCallback<ProcessCameraProvider> {

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
            }

            override fun onSuccess(result: ProcessCameraProvider?) {
                if (result == null) {
                    return
                }
                onSuccess(result)
            }
        },
        ContextCompat.getMainExecutor(this)
    )
}