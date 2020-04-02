package com.charliechristensen.voxeet.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.charliechristensen.voxeet.R
import com.voxeet.VoxeetSDK
import com.voxeet.android.media.MediaStreamType
import com.voxeet.promise.Promise
import com.voxeet.promise.solve.PromiseExec
import com.voxeet.sdk.events.v2.StreamAddedEvent
import com.voxeet.sdk.events.v2.StreamRemovedEvent
import com.voxeet.sdk.events.v2.StreamUpdatedEvent
import com.voxeet.sdk.json.ParticipantInfo
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.models.v1.CreateConferenceResult
import kotlinx.android.synthetic.main.fragment_chat.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChatFragment : Fragment(R.layout.fragment_chat) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoxeetSDK.instance().register(this)
        val name = requireArguments().getString(KEY_NAME) ?: "Chris"
        val conferenceName = requireArguments().getString(KEY_CONFERENCE_NAME) ?: "DefaultConferenceName"
        toolbar.title = conferenceName
        startSession(name, conferenceName)
        toggleVideoButton.setOnClickListener {
            val ownVideoStarted = VoxeetSDK.conference().currentConference?.isOwnVideoStarted ?: return@setOnClickListener
            if (ownVideoStarted) {
                stopVideo()
            } else {
                startVideo()
            }
        }
    }

    override fun onDestroy() {
        stopVideo()
        VoxeetSDK.conference().leave()
            .then(PromiseExec<Boolean, Any> { result, solver -> Log.d("ChatFragment", "Conference Left - Result-$result") })
            ?.error { error ->
                Log.d(
                    "ChatFragment",
                    "Error leaving conference - Error - ${error.message}"
                )
            }
        VoxeetSDK.instance().unregister(this)
        super.onDestroy()
    }

    private fun startSession(name: String, conferenceName: String) {
        if (name.isBlank() || conferenceName.isBlank()) {
            return
        }
        val participantInfo = ParticipantInfo(name, "", "")
        VoxeetSDK.session()
            .open(participantInfo)
            .then(PromiseExec<Boolean, CreateConferenceResult> { result, solver -> solver.resolve(VoxeetSDK.conference().create(conferenceName)) })
            ?.then(PromiseExec<CreateConferenceResult, Conference> { result, solver ->
                Log.d("ChatFragment", "Conference Connected - Result-$result")
                val join: Promise<Conference> = VoxeetSDK.conference().join(result!!.conferenceId)
                solver.resolve(join)
            })
            ?.then(PromiseExec<Conference, Boolean> { result, solver ->
                Log.d("ChatFragment", "Conference Joined - Result-$result")
                startVideo()
                toggleVideoButton.visibility = View.VISIBLE
            })
            ?.error { error ->
                Log.d(
                    "ChatFragment",
                    "Error creating and joining conference - Error - ${error.message}"
                )
            }
    }

    private fun startVideo() {
        VoxeetSDK.conference().startVideo()
            .then(PromiseExec<Boolean, Boolean> { result, solver -> Log.d("ChatFragment", "Video Started - Result-$result") })
            ?.error { error ->
                Log.d(
                    "ChatFragment",
                    "Error starting video - Error - ${error.message}"
                )
            }
    }

    private fun stopVideo() {
        VoxeetSDK.conference().stopVideo()
            .then(PromiseExec<Boolean, Boolean> { result, solver -> Log.d("ChatFragment", "Video Stopped - Result-$result") })
            ?.error { error ->
                Log.d(
                    "ChatFragment",
                    "Error stopping video - Error - ${error.message}"
                )
            }
    }

    private fun updateStreams() {
        VoxeetSDK.conference().participants
            .forEach { user ->
                val userId = user.id
                val isLocal = user.id == VoxeetSDK.session().participantId
                val stream = user.streamsHandler().getFirst(MediaStreamType.Camera)
                val video = if (isLocal) playerVideoView else subjectVideoView
                val name = if (isLocal) playerNameTextView else subjectNameTextView
                name.text = user.info?.name ?: ""
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
