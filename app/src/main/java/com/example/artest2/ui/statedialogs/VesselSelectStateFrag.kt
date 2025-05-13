package com.example.artest2.ui.statedialogs

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.fragment.findNavController
import com.example.artest2.R
import java.nio.channels.spi.AsynchronousChannelProvider.provider

class VesselSelectStateFrag : Fragment() {

    companion object {
        fun newInstance() = VesselSelectStateFrag()
    }

    private val viewModel: VesselSelectStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_ssel_select_state, container, false)

        // Find the button by its ID
        val myNewButton: Button = view.findViewById(R.id.vessel_select_button)

        // Set an OnClickListener
        myNewButton.setOnClickListener {
            // Code to execute when the button is clicked
            Log.d("ART","Button Clicked in VesselSelectStateFrag")
            findNavController().navigate(R.id.navigation_home)
        }
        return view
    }

}