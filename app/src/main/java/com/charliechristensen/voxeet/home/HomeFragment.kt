package com.charliechristensen.voxeet.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.charliechristensen.voxeet.R
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment: Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startChatButton.setOnClickListener {
            val name = nameTextView.text.toString()
            val conferenceName = conferenceNameTextView.text.toString()
            if(name.isBlank() || conferenceName.isBlank()) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, ChatFragment.create(name, conferenceName))
                .addToBackStack(null)
                .commit()
        }
    }

}
