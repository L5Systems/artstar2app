package com.example.artest2.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.example.artest2.databinding.FragmentDashboardBinding
import com.example.artest2.manager.TransactionManager
import com.example.artest2.transactions.SampleTransaction
import com.example.artest2.ui.statedialogs.StateInputFragment
import kotlinx.coroutines.launch

public val TAG: String = "ZZ"

class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var navController: NavController


    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var selectedTran:String = ""
    private lateinit var dashboardViewModel: DashboardViewModel
    // ActivityResultLauncher for Fragments that return results

    //private val vesselSelectionLauncher = registerForActivityResult(VesselSelectionResultContract()) { resultData ->
        // The resultData here should include the 'requestId'
    //    val requestId = resultData?.getString("requestId")
    //   if (requestId != null) {
    //        val dataMap = resultData.getSerializableExtra("dataMap") as? HashMap<String, Any>
    //        viewModel.processDialogResult(requestId, dataMap)
    //    } else {
    //        Log.w("DashboardFragment", "Received result from VesselSelectionFragment without a requestId.")
   //     }
  // }


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
        dashboardViewModel.setDashboardFragment(this)

        collectTransactionStatus(dashboardViewModel)
        collectUiActions(dashboardViewModel)
        collectUiState(dashboardViewModel)

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
        // In DashboardFragment.kt

        class DashboardFragment : Fragment() { // Implement the new listener

            // ... (ViewModel, binding, etc.)
            // Store the active dialog's original TransactionManager callback, keyed by requestId
            private val activeDialogCallbacks = mutableMapOf<String, (Map<String, Any>?) -> Unit>()


            private fun collectUiEventsFromViewModel() {
                viewLifecycleOwner.lifecycleScope.launch {
                    // Assuming dashboardViewModel.uiActions is where TransactionManager.UiAction.ShowDialogActivity arrives
                    dashboardViewModel.uiActions.collect { action ->
                        when (action) {
                            is TransactionManager.UiAction.ShowDialogActivity -> {
                                Log.d("DashboardFragment", "Received ShowDialogActivity for ID ${action.requestId}")
                                // Store the original callback from the TransactionManager
                                activeDialogCallbacks[action.requestId] = action.onResult
                                // Show the generic dialog
                                showStateDrivenDialog(action)
                            }
                            // ... other actions
                            is TransactionManager.UiAction.RequestInputScreen -> TODO()
                            is TransactionManager.UiAction.ShowMessage -> TODO()
                            is TransactionManager.UiAction.UpdateTransactionStatus -> TODO()
                            is TransactionManager.UiAction.DialogResult -> TODO()
                        }
                    }
                }
            }

            private fun showStateDrivenDialog(dialogAction: TransactionManager.UiAction.ShowDialogActivity) {
                val dialogFragment = StateInputFragment.newInstance(dialogAction.requestId, dialogAction.dialogType, "testDialog", "DO This", "OK", "CANCEL")
                dialogFragment.setDialogListener(this) // DashboardFragment is the StateDialogListener
                dialogFragment.show(childFragmentManager, StateInputFragment.TAG + "_" + dialogAction.requestId) // Unique tag
            }

            // --- Implementation of StateDialogFragment.StateDialogListener ---
            fun onDialogDismissed(requestId: String, results: Map<String, Any>?) {
                Log.d("DashboardFragment", "StateDialog dismissed for requestId: $requestId, Results: $results")

                // Retrieve the original callback for this request
                val originalCallback = activeDialogCallbacks.remove(requestId)

                if (originalCallback == null) {
                    Log.w("DashboardFragment", "No callback found for dialog requestId: $requestId")
                    return
                }

                // Process the generic results into what the TransactionManager/State expects
                // This is where you map from the generic dialog output to the specific needs
                // of the state that requested the dialog.
                var processedResultForState: Map<String, Any>? = null

                if (results != null) {
                    if (results[StateInputFragment.ResultKeys.POSITIVE_CLICK] == true) {
                        val outputMap = mutableMapOf<String, Any>()
                        results[StateInputFragment.ResultKeys.USER_TEXT_INPUT]?.let { outputMap["userInput"] = it }
                        results[StateInputFragment.ResultKeys.SELECTED_ITEM]?.let { outputMap["selectedVessel"] = it } // Example specific mapping
                        // Add more mappings based on dialogType or customData if needed

                        if (outputMap.isEmpty() && results.containsKey(StateInputFragment.ResultKeys.POSITIVE_CLICK)) {
                            // If it's just a confirmation dialog, positive click might be enough
                            processedResultForState = mapOf("confirmed" to true)
                        } else if (outputMap.isNotEmpty()){
                            processedResultForState = outputMap
                        } else {
                            // Positive click but no specific data captured relevant to this state type
                            // This logic depends on how your states interpret an "empty" positive result
                            Log.d("DashboardFragment", "Positive click but no specific data for TM, requestId: $requestId")
                            processedResultForState = mapOf("confirmed_empty" to true) // Or null, depending on state needs
                        }

                    } else if (results[StateInputFragment.ResultKeys.NEGATIVE_CLICK] == true || results["cancelled"] == true) {
                        Log.d("DashboardFragment", "Dialog cancelled or negative for requestId: $requestId")
                        processedResultForState = null // Typically null for cancellation/negative
                    }
                    // Handle neutral click if necessary
                } else {
                    // Dialog dismissed without explicit button press (e.g. back button, touch outside)
                    // Usually treated as cancellation
                    Log.d("DashboardFragment", "Dialog dismissed without action for requestId: $requestId")
                    processedResultForState = null
                }

                originalCallback.invoke(processedResultForState)
            }
            // ...
        }
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

                    dashboardViewModel.startSampleTransaction(selectedTran,selectedVessel, this@DashboardFragment)
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
    private fun collectUiActions(dashboardView: DashboardViewModel) {
        val manager = dashboardView.getTransactionManager()
        lifecycleScope.launch {

            dashboardView.uiActions.collect { action ->
                when (action) {
                       is TransactionManager.UiAction.DialogResult -> {
                        // This action is primarily for internal ViewModel/TransactionManager use,
                        // not typically handled directly in the Activity collector.
                        // The result is already being passed via handleDialogResult().
                        Log.d("MainActivity", "Collecting UiAction: DialogResult (handled internally)")
                    }

                    is TransactionManager.UiAction.ShowDialogActivity -> TODO()
                    is TransactionManager.UiAction.RequestInputScreen -> TODO()
                    is TransactionManager.UiAction.ShowMessage -> TODO()
                    is TransactionManager.UiAction.UpdateTransactionStatus -> TODO()
                }
            }
        }
    }
    // NEW FUNCTION TO COLLECT UI STATE
    private fun collectUiState(dashboardView: DashboardViewModel) {
        lifecycleScope.launch {
            // COLLECTING STATE (current state of the UI)
            dashboardView.uiState.collect { state ->
                when (state) {
                    is DashboardViewModel.UiState.Idle -> {
                        Log.d("MainActivity", "UI State: Idle")
                        // Update UI for Idle state
                    }
                    is DashboardViewModel.UiState.Loading -> {
                        Log.d("MainActivity", "UI State: Loading - ${state.message}")
                        // Update UI to show loading indicator/message
                    }
                    is DashboardViewModel.UiState.StepComplete -> {
                        Log.d("MainActivity", "UI State: Step Complete - ${state.message}")
                        // Update UI to show step completion message, maybe enable next button
                    }
                    is DashboardViewModel.UiState.TransactionComplete -> {
                        Log.d("MainActivity", "UI State: Transaction Complete - ${state.message}")
                        // Update UI to show transaction finished message
                    }
                    is DashboardViewModel.UiState.Error -> {
                        Log.e("MainActivity", "UI State: Error - ${state.message}")
                        // Show an error message to the user
                    }


                    DashboardViewModel.UiState.Idle -> TODO()

                }
            }
        }
    }

    // You might also collect the overall transaction status for display
    private fun collectTransactionStatus(dashboardView: DashboardViewModel) {
        lifecycleScope.launch {
            dashboardView.currentTransStatus.collect { status ->
                Log.d("MainActivity", "Transaction Status: ID=${status.selectedTransactionId}, Name=${status.selectedTransactionName}, Status=${status.transactionStatus}")
                // Update a TextView or other UI element to show the status
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun StateInputFragment.setDialogListener(fragment: Fragment) {}
