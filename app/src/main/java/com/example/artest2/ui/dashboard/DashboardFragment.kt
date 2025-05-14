package com.example.artest2.ui.dashboard

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.artest2.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // --- Spinner Setup with Dummy Data ---

        // ... inside onCreateView method ...

        // --- Spinner Setup with Dummy Data ---

        val vesselItems = listOf(
            "Please select Vessel", // Hint item
            "Titanic",
            "Santa Maria",
            "Mayflower",
            "Black Pearl",
            "Endeavour"
        )
        val vesselAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item, // Corrected this from R.layout.simple_spinner_dropdown_item
            vesselItems
        )
        binding.spinnerVessel.adapter = vesselAdapter

        binding.spinnerVessel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedVessel = vesselItems[position]
                if (position > 0) {
                    Toast.makeText(
                        requireContext(),
                        "Selected Vessel: $selectedVessel",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // This block should be empty or contain logic for when nothing is selected in spinnerVessel
            }
        } // End of spinnerVessel.onItemSelectedListener

        // **CORRECT PLACEMENT FOR TRANSACTION SPINNER SETUP**
        val transactionItems = listOf(
            "Select Transaction", // Hint item
            "Bunkering",
            "Cargo Loading",
            "Cargo Discharge",
            "Stores Supply",
            "Crew Change"
        )
        val transactionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            transactionItems
        )
        binding.spinnerTransaction.adapter = transactionAdapter // This line was trying to use transactionAdapter before it was defined in the previous scope

        binding.spinnerTransaction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // ... listener for transaction spinner ...
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTransaction = transactionItems[position]
                if (position > 0) {
                    Toast.makeText(requireContext(), "Selected Transaction: $selectedTransaction", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}