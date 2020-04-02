package com.charliechristensen.voxeet.home

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.charliechristensen.voxeet.R
import com.voxeet.VoxeetSDK
import com.voxeet.promise.solve.PromiseExec
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment: Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startChatButton.setOnClickListener { startChat() }
        logoutButton.setOnClickListener { logout() }
        nameTextView.text = VoxeetSDK.session().participant?.info?.name ?: ""
        conferenceInput.addTextChangedListener { conferenceInputLayout.error = null }
    }

    private fun startChat() {
        val conferenceName = conferenceInput.text.toString()
        if(conferenceName.isBlank()) {
            conferenceInputLayout.error = "Enter conference name"
            return
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view, ChatFragment.create(conferenceName))
            .addToBackStack(null)
            .commit()
    }

    private fun logout() {
        showProgress(true)
        VoxeetSDK.session()
            .close()
            .then(PromiseExec<Boolean, Any> { _, _ ->
                showProgress(false)
                parentFragmentManager.popBackStack()
            })
            .error { error ->
                Log.d("VoxeetDEBUG", "Error logging out - ${error.message}")
                showProgress(false)
            }
    }

    private fun showProgress(show: Boolean) {
        if(show) {
            progressBar.isVisible = true
            startChatButton.isEnabled = false
            logoutButton.isEnabled = false
        } else {
            progressBar.isInvisible = true
            startChatButton.isEnabled = true
            logoutButton.isEnabled = true
        }
    }

}
