package com.charliechristensen.voxeet.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.charliechristensen.voxeet.R
import com.voxeet.android.media.MediaStreamType
import com.voxeet.sdk.VoxeetSdk
import com.voxeet.sdk.events.v2.StreamAddedEvent
import com.voxeet.sdk.events.v2.StreamRemovedEvent
import com.voxeet.sdk.events.v2.StreamUpdatedEvent
import com.voxeet.sdk.json.UserInfo
import com.voxeet.sdk.models.v1.CreateConferenceResult
import eu.codlab.simplepromise.solve.ErrorPromise
import eu.codlab.simplepromise.solve.PromiseExec
import eu.codlab.simplepromise.solve.Solver
import kotlinx.android.synthetic.main.fragment_chat.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var isShowingVideo: Boolean by Delegates.observable(false) { property, oldValue, newValue ->
        if (newValue) {
            startVideo()
        } else {
            stopVideo()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoxeetSdk.instance()?.register(this)
        val name = requireArguments().getString(KEY_NAME) ?: "Chris"
        val conferenceName = requireArguments().getString(KEY_CONFERENCE_NAME) ?: "DefaultConferenceName"
        toolbar.title = conferenceName
        startSession(name, conferenceName)
        toggleVideoButton.setOnClickListener {
            isShowingVideo = !isShowingVideo
        }
    }

    override fun onDestroy() {
        stopVideo()
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
        VoxeetSdk.instance()?.unregister(this)
        super.onDestroy()
    }

    private fun startSession(name: String, conferenceName: String) {
        if (name.isBlank() || conferenceName.isBlank()) {
            return
        }
        val participantInfo = UserInfo(name, "", "")
        VoxeetSdk.session()
            ?.open(participantInfo)
            ?.then(object : PromiseExec<Boolean, CreateConferenceResult>() {
                override fun onCall(result: Boolean?, solver: Solver<CreateConferenceResult>) {
                    solver.resolve(VoxeetSdk.conference()!!.create(conferenceName))
                }
            })
            ?.then(object : PromiseExec<CreateConferenceResult, Boolean>() {
                override fun onCall(result: CreateConferenceResult?, solver: Solver<Boolean>) {
                    Log.d("ChatFragment", "Conference Connected - Result-$result")
                    val join = VoxeetSdk.conference()!!.join(result!!.conferenceId)
                    solver.resolve(join)
                }
            })
            ?.then(object : PromiseExec<Boolean, Boolean>() {
                override fun onCall(result: Boolean?, solver: Solver<Boolean>) {
                    Log.d("ChatFragment", "Conference Joined - Result-$result")
                    isShowingVideo = true
                    toggleVideoButton.visibility = View.VISIBLE
                }
            })
            ?.error(object : ErrorPromise() {
                override fun onError(error: Throwable) {
                    Log.d(
                        "ChatFragment",
                        "Error creating and joining conference - Error - ${error.message}"
                    )
                }
            })
    }

    private fun startVideo() {
        VoxeetSdk.conference()?.startVideo()
            ?.then(object : PromiseExec<Boolean, Boolean>() {
                override fun onCall(result: Boolean?, solver: Solver<Boolean>) {
                    Log.d("ChatFragment", "Video Started - Result-$result")
                }
            })
            ?.error(object : ErrorPromise() {
                override fun onError(error: Throwable) {
                    Log.d(
                        "ChatFragment",
                        "Error starting video - Error - ${error.message}"
                    )
                }
            })
    }

    private fun stopVideo() {
        VoxeetSdk.conference()?.stopVideo()
            ?.then(object : PromiseExec<Boolean, Boolean>() {
                override fun onCall(result: Boolean?, solver: Solver<Boolean>) {
                    Log.d("ChatFragment", "Video Stopped - Result-$result")
                }
            })
            ?.error(object : ErrorPromise() {
                override fun onError(error: Throwable) {
                    Log.d(
                        "ChatFragment",
                        "Error stopping video - Error - ${error.message}"
                    )
                }
            })
    }

    private fun updateStreams() {
        VoxeetSdk.conference()?.users
            ?.forEach { user ->
                val userId = user.id
                val isLocal = user.id == VoxeetSdk.session()?.userId
                val stream = user.streamsHandler().getFirst(MediaStreamType.Camera)
                val video = if (isLocal) playerVideoView else subjectVideoView
                val name = if (isLocal) playerNameTextView else subjectNameTextView
                name.text = user.userInfo?.name ?: ""
                if (userId != null && stream != null && stream.videoTracks().isNotEmpty()) {
                    video.visibility = View.VISIBLE
                    name.visibility = View.VISIBLE
                    video.attach(userId, stream)
                } else {
                    video.visibility = View.GONE
                    name.visibility = View.GONE
                }
            }
    }

    //region Voxeet Event Bus Methods

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: StreamAddedEvent) {
        updateStreams()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: StreamUpdatedEvent) {
        updateStreams()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: StreamRemovedEvent) {
        updateStreams()
    }

    //endregion

    companion object {
        private const val KEY_NAME = "KeyName"
        private const val KEY_CONFERENCE_NAME = "KeyConferenceName"

        fun create(name: String, conferenceName: String): ChatFragment = ChatFragment().apply {
            arguments = bundleOf(
                KEY_NAME to name,
                KEY_CONFERENCE_NAME to conferenceName
            )
        }

    }

}
