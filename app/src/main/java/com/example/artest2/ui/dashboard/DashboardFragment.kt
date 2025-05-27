package com.example.artest2.ui.dashboard

import com.example.artest2.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.artest2.DialogManager
import com.example.artest2.databinding.FragmentDashboardBinding
import com.example.artest2.manager.TransactionManager

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
     //private var dashboardViewModel?: Any = null //DashboardViewModel = DashboardViewModel(requireActivity().application)
    // Store the active dialog's original TransactionManager callback, keyed by requestId
    private val activeDialogCallbacks = mutableMapOf<String, (Map<String, Any>?) -> Unit>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        dashboardViewModel.setDashboardFragment(this)
        navController = findNavController()
        collectTransactionStatus(dashboardViewModel)
        collectUiActions(dashboardViewModel)
        collectUiState(dashboardViewModel)
        collectUiEventsFromViewModel()


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
                           viewModel. processStateCompletion(action.requestId,null)  //state.message

                           Log.d("MainActivity", "Collecting UiAction: DialogResult (handled internally)")
                    }

                    is TransactionManager.UiAction.ShowDialogActivity -> TODO()
                    is TransactionManager.UiAction.RequestInputScreen -> requestInputScreen(action)
                    is TransactionManager.UiAction.ShowMessage -> TODO()
                    is TransactionManager.UiAction.UpdateTransactionStatus -> TODO()
                }
            }
        }
    }
    private fun requestInputScreen(action: TransactionManager.UiAction.RequestInputScreen) {

        Log.i("DashboardFragment", "Handling RequestInputScreen (ID: ${action.requestId}) for screen: ${action.screenIdentifier}")
        // Decide which actual Fragment/Dialog to show based on action.screenIdentifier
        // This is similar to how ShowDialogActivity uses dialogType.

        val dialogTypeToUse: DialogManager.DialogType = when (action.screenIdentifier) {
            "TEXT_INPUT_DIALOG" -> DialogManager.DialogType.TEXT_INPUT_DIALOG
            "SELECT_QUANTITY" -> DialogManager.DialogType.QUANTITY_INPUT_FRAGMENT // Hypothetical
            // Add more mappings as needed
            else -> {
                Log.w("DashboardFragment", "Unknown screenIdentifier for RequestInputScreen: ${action.screenIdentifier}")
                viewModel.processFragmentCancellation(action.requestId) // CRITICAL: Complete the deferred
                return //@collect // Stop further processing for this unknown action
            }
        }

        // Now, effectively treat it like a ShowDialogActivity
        val showDialogAction = TransactionManager.UiAction.ShowDialogActivity(
            requestId = action.requestId,
            stateName = "UnknownState_From_RequestInputScreen", // Or try to get this from TM if possible
            transactionId = "UnknownTransaction_From_RequestInputScreen", // Or try to get this from TM
            dialogType = dialogTypeToUse,
            onResult = action.onResult, // Or construct from action
            title = action.prompt ?: "Input Required",
            message = "",
            positiveButtonText = "OK",
            negativeButtonText = "Cancel",    // action.initialValue,
            neutralButtonText = "",
            items = "",
            initialSelection = "",
            callback = {},
            inputHint = "")
        handleShowDialogActivity(showDialogAction) // Reuse your existing navigation logic

    }
    /**
     * Handles the ShowDialogActivity UI Action by navigating to the appropriate
     * Fragment or showing a system dialog based on the action's dialogType.
     */
    private fun handleShowDialogActivity(action: TransactionManager.UiAction.ShowDialogActivity) {
        Log.i("DashboardFragment", "Handling ShowDialogActivity for RequestID: ${action.requestId}, DialogType: ${action.dialogType}")

        // Always ensure the NavController is ready and the Fragment is in a valid state to navigate
        if (!isAdded || !navController.currentDestination?.id.let { it == R.id.dashboardFragment || it == null /* initial state */ }) {
            // Or if currentDestination is something else, it might mean a navigation is already in progress
            Log.w("DashboardFragment", "Cannot handle ShowDialogActivity: Fragment not added, NavController not ready, or already navigating. RequestID: ${action.requestId}")
            viewModel.processFragmentCancellation(action.requestId) // Critical: inform TM
            return
        }

        try {
            when (action.dialogType) {
                DialogManager.DialogType.TEXT_INPUT_DIALOG -> {
                    val args = bundleOf(
                        // Standard arguments your VesselSelectionFragment expects
                        "requestId" to action.requestId, // Essential for result mapping
                        "dialogTitle" to action.title,
                        // Example of passing custom data
                       // "customFilter" to (action.customData["filter"] as? String),
                        //"region" to (action.customData["region"] as? String)
                        // Add other arguments defined in your nav_graph for VesselSelectionFragment
                    )
                    // Ensure R.id.action_dashboardFragment_to_vesselSelectionFragment exists in your nav graph
                    // and originates from dashboardFragment
                    navController.navigate(R.id.action_dashboardFragment_to_StateInputFragment, args)
                }

                DialogManager.DialogType.TEXT_INPUT_DIALOG -> {
                    // This could be a simple DialogFragment for text input
                    // Example: SimpleTextDialogFragment.newInstance(...).show(...)
                    // For this to work seamlessly with the TransactionManager's deferred result,
                    // this DialogFragment would also need to use the FragmentResultListener
                    // or a direct callback mechanism that eventually calls viewModel.processFragmentResult.

                    // Let's assume you have a StateInputFragment or similar that can handle this
                    val dialogFragment = StateInputFragment.newInstance(
                        requestId = action.requestId,
                        dialogType = action.dialogType, // Pass it along
                        title = action.title ?: "Input Text",
                        message = action.message ?: "Please enter the required information.",
                        positiveButton = action.positiveButtonText ?: "OK",
                        negativeButton = action.negativeButtonText ?: "Cancel",
                        inputHint = action.inputHint
                        // `items` and `initialSelection` might not be used by a simple text input
                    )
                    // The StateInputFragment needs to be set up to use the FragmentResult API
                    // and call parentFragmentManager.setFragmentResult(...)
                    // It should also be prepared to be launched by childFragmentManager if it's a DialogFragment
                    dialogFragment.show(childFragmentManager, "${StateInputFragment.TAG}_${action.requestId}")
                }

                DialogManager.DialogType.CONFIRMATION_DIALOG -> {
                    Builder(requireContext())
                        .setTitle(action.title)
                        .setMessage(action.message)
                        .setPositiveButton(action.positiveButtonText) { _, _ ->
                            // Positive confirmation
                            viewModel.processFragmentResult(action.requestId, mapOf("confirmed" to true))
                        }
                        .setNegativeButton(action.negativeButtonText) { _, _ ->
                            // Negative confirmation or cancellation
                            viewModel.processFragmentResult(action.requestId, mapOf("confirmed" to false, "cancelled" to true))
                        }
                        .setOnCancelListener {
                            // Dismissed without button press (e.g. back button)
                            viewModel.processFragmentCancellation(action.requestId)
                        }
                        .show()
                }

                DialogManager.DialogType.BUNKER_DETAILS_FRAGMENT -> {
                    val args = bundleOf(
                        "requestId" to action.requestId,
                        "transactionId" to action.transactionId,
                        //"initialData" to (action.customData["details"] as? HashMap<String, Any>) // Example custom data
                    )
                    // Ensure R.id.action_dashboardFragment_to_bunkerDetailsFragment exists
                    // navController.navigate(R.id.action_dashboardFragment_to_bunkerDetailsFragment, args)
                    Log.w("DashboardFragment", "Navigation for BUNKER_DETAILS_FRAGMENT not fully implemented yet.")
                    viewModel.processFragmentCancellation(action.requestId) // Placeholder
                }

                DialogManager.DialogType.GENERIC_MESSAGE -> {
                    // This DialogType might be better handled by UiAction.ShowMessage,
                    // but if ShowDialogActivity is used for it:
                    Builder(requireContext())
                        .setTitle(action.title)
                        .setMessage(action.message)
                        .setPositiveButton(action.positiveButtonText ?: "OK") { dialog, _ ->
                            dialog.dismiss()
                            // If a GENERIC_MESSAGE shown via ShowDialogActivity is expected
                            // to "complete" a step, send a generic result.
                            viewModel.processFragmentResult(action.requestId, mapOf("acknowledged" to true))
                        }
                        .setOnCancelListener {
                            viewModel.processFragmentCancellation(action.requestId) // If cancellation means something
                        }
                        .show()
                }

                // Add cases for other DialogManager.DialogType values you define
                // else -> { ... } // Kotlin 'when' should be exhaustive if DialogType is an enum
                DialogManager.DialogType.VESSEL_SELECTION -> TODO()
                DialogManager.DialogType.USER_INPUT_TEXT -> TODO()
                DialogManager.DialogType.CONFIRMATION -> TODO()
                DialogManager.DialogType.INFO_DISPLAY -> TODO()
                DialogManager.DialogType.QUANTITY_INPUT_FRAGMENT -> TODO()
                DialogManager.DialogType.VESSEL_SELECTION_FRAGMENT -> TODO()
            }
        } catch (e: Exception) {
            // Catching general exceptions during navigation or dialog showing is crucial
            Log.e("DashboardFragment", "Failed to handle ShowDialogActivity for type ${action.dialogType}, RequestID: ${action.requestId}", e)
            // CRITICAL: Inform the TransactionManager that the UI could not be shown,
            // otherwise the state will hang indefinitely waiting for a result.
            viewModel.processFragmentCancellation(action.requestId)
        }
    }
    private fun collectUiEventsFromViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Assuming dashboardViewModel.uiActions is where TransactionManager.UiAction.ShowDialogActivity arrives
            TransactionManager.uiActions.collect { action ->
                when (action) {
                    is TransactionManager.UiAction.ShowDialogActivity -> {
                        Log.d("DashboardFragment", "Received ShowDialogActivity for ID ${action.requestId}")
                        // Store the original callback from the TransactionManager
                        activeDialogCallbacks[action.requestId] = action.onResult
                        // Show the generic dialog
                        //showStateDrivenDialog(action)
                    }
                    // ... other actions
                    is TransactionManager.UiAction.RequestInputScreen->
                    {
                        Log.d("DashboardFragment","Received RequestInputScreen for ID ${action.requestId}")
                    }
                    is TransactionManager.UiAction.ShowMessage -> {Log.d("DashboardFragment","ShowMessage")}
                    is TransactionManager.UiAction.UpdateTransactionStatus -> {Log.d("DashboardFragment","UpdateTransactionStatus}")}
                    is TransactionManager.UiAction.DialogResult -> {Log.d("DashboardFragment","DialogResult ${action.requestId}")}
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
                        viewModel. processStateCompletion(state.message,null)
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

private fun DashboardViewModel.processFragmentResult(
    requestId: String,
    mapOf: Map<String, Boolean>
) {
}

private fun DashboardViewModel.processFragmentCancellation(requestId: String) {

}

private fun StateInputFragment.setDialogListener(fragment: Fragment) {


}
