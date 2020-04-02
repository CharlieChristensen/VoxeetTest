package com.charliechristensen.voxeet

import android.app.Application
import com.voxeet.VoxeetSDK
import com.voxeet.sdk.VoxeetSdk

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        VoxeetSDK.initialize("NDkzanZkaTllZGJzcg==", "NTluZnZkaTU4MnZqOThhajg5ZDJhb3Z0aG4=")
    }

}
