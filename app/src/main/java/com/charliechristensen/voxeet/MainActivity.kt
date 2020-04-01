package com.charliechristensen.voxeet

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.charliechristensen.voxeet.home.HomeFragment
import com.voxeet.sdk.VoxeetSdk
import eu.codlab.simplepromise.solve.ErrorPromise
import eu.codlab.simplepromise.solve.PromiseExec
import eu.codlab.simplepromise.solve.Solver


class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container_view, HomeFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
            ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ),
                0x20
            )
        }
    }

    override fun onDestroy() {
        VoxeetSdk.conference()?.leave()
            ?.then(object : PromiseExec<Boolean, Any>() {
                override fun onCall(result: Boolean?, solver: Solver<Any>) {
                    Log.d("ChatFragment", "Conference Left - Result-$result")
                }
            })
            ?.error(object : ErrorPromise() {
                override fun onError(error: Throwable) {
                    Log.d(
                        "ChatFragment",
                        "Error leaving conference - Error - ${error.message}"
                    )
                }
            })
        VoxeetSdk.session()?.close()
            ?.then(object : PromiseExec<Boolean, Any>() {
                override fun onCall(result: Boolean?, solver: Solver<Any>) {
                    Log.d("ChatFragment", "Session Closed - Result-$result")
                }
            })
            ?.error(object : ErrorPromise() {
                override fun onError(error: Throwable) {
                    Log.d(
                        "ChatFragment",
                        "Error ending session - Error - ${error.message}"
                    )
                }
            })
        super.onDestroy()
    }

}
