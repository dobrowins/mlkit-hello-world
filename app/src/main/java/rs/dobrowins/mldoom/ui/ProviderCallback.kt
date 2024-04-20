package rs.dobrowins.mldoom.ui

import android.app.Activity
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

inline fun <R : Any> ProviderCallback(
    future: ListenableFuture<R>,
    crossinline onSuccess: (R) -> Unit): Activity.() -> Unit = {
    Futures.addCallback(
        future,
        object : FutureCallback<R> {

            override fun onFailure(t: Throwable) {
                t.printStackTrace()
            }

            override fun onSuccess(result: R?) {
                if (result == null) {
                    return
                }
                onSuccess(result)
            }
        },
        ContextCompat.getMainExecutor(this)
    )
}