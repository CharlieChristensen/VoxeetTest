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
import com.voxeet.sdk.json.ParticipantInfo
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment: Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginButton.setOnClickListener { login() }
        nameInput.addTextChangedListener { nameInputLayout.error = null }
    }

    private fun login() {
        val name = nameInput.text.toString()
        if(name.isBlank()) {
            nameInputLayout.error = "Enter name"
            return
        }
        showProgress(true)
        val participantInfo = ParticipantInfo(name, "", "")
        VoxeetSDK.session()
            .open(participantInfo)
            .then(PromiseExec<Boolean, Any> { _, _ ->
                showProgress(false)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, HomeFragment())
                    .addToBackStack(null)
                    .commit()
            })
            .error { error ->
                Log.d("VoxeetDEBUG", "Login error - $error.message")
                showProgress(false)
            }
    }

    private fun showProgress(show: Boolean) {
        if(show) {
            loginButton.isEnabled = false
            progressBar.isVisible = true
        } else {
            loginButton.isEnabled = true
            progressBar.isInvisible = true
        }
    }

}
