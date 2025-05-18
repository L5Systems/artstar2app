package com.example.artest2.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.commit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.replace
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController


import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.artest2.R
import com.example.artest2.databinding.FragmentHomeBinding


private fun Any.findFragmentById(navHostFragment: Int) {}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var navController: NavController
    // Get the NavController from the NavHostFragment
    //navController = navHostFragment.navController
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        //findNavController().navigate(R.id.navigation_home)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val navView: Any = findFragmentById(R.id.home_view) // If you have a BottomNavigationView

        val textView: TextView = binding.newsText
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // Find the button by its ID using the binding object
        val fetchButton = binding.fetchLatestNews // Assuming 'fetchLatestNews' is the ID in your layout XML

        // Set an OnClickListener for the button
        fetchButton.setOnClickListener {
            updateNewsText(Editable.Factory.getInstance().newEditable("News text"))
            displayVesselSelectStateFragment()

        }


        return root
    }

// ... other imports

    fun displayVesselSelectStateFragment() {
        //  val action = R.id.action_navigation_home_to_fragment_ssel_select_state
        findNavController().navigate(R.id.fragment_ssel_select_state)
    }
    fun displayAlertDialog() {
        val builder = AlertDialog.Builder(context)
        updateNewsText(Editable.Factory.getInstance().newEditable("News text"))
        // Set the title and message
        builder.setTitle("Dialog Title")
        builder.setMessage("This is an example of an AlertDialog in Kotlin.")

        // Set a positive button (e.g., "OK")
        builder.setPositiveButton("OK") { dialog, _ ->
            // Handle the "OK" button click
            dialog.dismiss()
        }

        // Set a negative button (e.g., "Cancel")
        builder.setNegativeButton("Cancel") { dialog, _ ->
            // Handle the "Cancel" button click
            dialog.dismiss()
        }

        // Optionally, set a neutral button
        builder.setNeutralButton("Maybe") { dialog, _ ->
            // Handle the "Maybe" button click
            dialog.dismiss()
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()

    }
    fun updateNewsText(news: Editable) {
        binding.newsText.text = news
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}