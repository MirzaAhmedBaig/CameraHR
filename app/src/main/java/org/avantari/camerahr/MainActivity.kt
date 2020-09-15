package org.avantari.camerahr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import net.kibotu.kalmanrx.jama.Matrix
import net.kibotu.kalmanrx.jkalman.JKalman
import org.avantari.camerahr.camview.HeartRateOmeter

/**
 * Created by Akhil B.V on 6/3/2020.
 * akhilbv@avantari.org
 */
@RequiresApi(Build.VERSION_CODES.M)
class MainActivity : AppCompatActivity() {

    private val TAG: String = MainActivity::class.java.simpleName

    override fun onResume() {
        super.onResume()
        dispose()
        subscription = CompositeDisposable()
        startCapturingCamHR()
    }

    private fun dispose() {
        if (subscription?.isDisposed == false)
            subscription?.dispose()
    }

    override fun onPause() {
        super.onPause()
        dispose()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    private fun hasPermission(vararg permissionsId: String): Boolean {
        var hasPermission = true

        permissionsId.forEach { permission ->
            hasPermission = hasPermission
                    && ContextCompat.checkSelfPermission(
                    this,
                    permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        return hasPermission
    }


    private val REQUEST_CAMERA_PERMISSION = 123
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCapturingCamHR()
                }
            }
        }
    }


    private var subscription: CompositeDisposable? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startCapturingCamHR() {
        if (!hasPermission(Manifest.permission.CAMERA)) {
            return
        }

        val kalman = JKalman(2, 1)
        val m = Matrix(1, 1)
        val tr = arrayOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, 1.0))
        kalman.transition_matrix = Matrix(tr)
        kalman.error_cov_post = kalman.error_cov_post.identity()
        val bpmUpdates = HeartRateOmeter()
                .withAverageAfterSeconds(3)
                .setFlashEnabled(false)
                .setCameraType(HeartRateOmeter.BACK_CAMERA, this)
                .setFingerDetectionListener(this::onFingerChange)
                .bpmUpdates(preview_view_1)
                .subscribe({
                    if (it.value == 0)
                        return@subscribe

                    m.set(0, 0, it.value.toDouble())
                    kalman.Predict()
                    val c = kalman.Correct(m)

                    val bpm = it.copy(value = c.get(0, 0).toInt())
                    Log.d(TAG, "ON BPM : ${it.value}")
                    onBpm(bpm.value)
                }, {
                    it.printStackTrace()
                    Log.i(TAG, "error is ${it?.localizedMessage}")
                })
        subscription?.add(bpmUpdates)
    }

    private fun onBpm(hr: Int) {
        Log.i(TAG, "onBpm is $hr")
        tvHeartRates.text = "onBpm is $hr"
    }

    private fun onFingerChange(boolean: Boolean) {
        Log.i(TAG, "onFingerChange is $boolean")
        tvFingerDetection.text = "Finger Detected $boolean"
    }


}