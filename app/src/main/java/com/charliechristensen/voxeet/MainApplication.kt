package com.charliechristensen.voxeet

import android.app.Application
import com.voxeet.sdk.VoxeetSdk

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        VoxeetSdk.initialize("", "")
    }

}
