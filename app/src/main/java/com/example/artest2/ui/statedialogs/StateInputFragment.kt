package com.example.artest2.ui.statedialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.example.artest2.DialogManager // Make sure DialogType is accessible
import com.example.artest2.databinding.FragmentStateInputBinding
import com.example.artest2.manager.TransactionManager
import com.example.artest2.ui.dashboard.DashboardViewModel


class StateInputFragment : DialogFragment() {

    object ResultKeys {
        const val POSITIVE_CLICK = "positive_click_result"
        const val NEGATIVE_CLICK = "negative_click_result"
        const val NEUTRAL_CLICK = "neutral_click_result"
        const val USER_TEXT_INPUT = "user_text_input_result"
        const val SELECTED_ITEM = "selected_item_result"
    }

    private var _binding: FragmentStateInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StateInputViewModel

    // Companion object for static methods like newInstance
    companion object {
        // Argument keys (should match those in nav_graph.xml and StateInputViewModel.StateInputArgs)
        // It's good practice to define these constants here for the newInstance method.
        private const val ARG_REQUEST_ID = "requestId"

        private const val ARG_DIALOG_TYPE = "dialogType"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE_BUTTON_TEXT = "positiveButtonText"
        private const val ARG_NEGATIVE_BUTTON_TEXT = "negativeButtonText"
        private const val ARG_NEUTRAL_BUTTON_TEXT = "neutralButtonText"
        private const val ARG_INPUT_HINT = "inputHint"
        private const val ARG_ITEMS = "items"
        private const val ARG_INITIAL_SELECTION = "initialSelection"
        // private const val ARG_CUSTOM_DATA = "customData" // If you pass custom data

        /**
         * Creates a new instance of StateInputFragment with the provided arguments.
         *
         * @param requestId A unique ID to identify the result callback.
         * @param dialogType The type of dialog to display (e.g., text input, confirmation).
         * @param title The title of the dialog.
         * @param message Optional message content for the dialog.
         * @param positiveButtonText Text for the positive action button.
         * @param negativeButtonText Optional text for the negative action button.
         * @param neutralButtonText Optional text for a neutral action button.
         * @param inputHint Optional hint for text input fields.
         * @param items Optional list of items for a spinner/selection dialog.
         * @param initialSelection Optional initial selection for spinner/selection dialog.
         * @return A new instance of StateInputFragment.
         */
        fun newInstance(
            requestId: String,
            dialogType: DialogManager.DialogType,
            title: String,
            message: String? = null,
            positiveButtonText: String? = "Confirm", // Provide sensible defaults
            negativeButtonText: String? = "Cancel",
            neutralButtonText: String? = null,
            inputHint: String? = null,
            items: List<String>? = null,
            initialSelection: String? = null,
            positiveButton: String,
            negativeButton: String
            // customData: Map<String, Any>? = null // Add if you pass custom data
        ): StateInputFragment {
            val fragment = StateInputFragment()
            val args = Bundle().apply {
                putString(ARG_REQUEST_ID, requestId)
                // DialogManager.DialogType needs to be Serializable or Parcelable to be put in a Bundle directly
                // If it's an enum, you can store its name() or ordinal() and reconstruct it.
                // Assuming it's Serializable for simplicity here:
                putSerializable(ARG_DIALOG_TYPE, dialogType) // Or use custom Parceler, or dialogType.name
                putString(ARG_TITLE, title)
                message?.let { putString(ARG_MESSAGE, it) }
                positiveButtonText?.let { putString(ARG_POSITIVE_BUTTON_TEXT, it) }
                negativeButtonText?.let { putString(ARG_NEGATIVE_BUTTON_TEXT, it) }
                neutralButtonText?.let { putString(ARG_NEUTRAL_BUTTON_TEXT, it) }
                inputHint?.let { putString(ARG_INPUT_HINT, it) }
                items?.let { putStringArray(ARG_ITEMS, it.toTypedArray()) }
                initialSelection?.let { putString(ARG_INITIAL_SELECTION, it) }
                // Handle customData if needed (might require Parcelable/Serializable logic)
            }
            fragment.arguments = args
            return fragment
        }
        public val TAG = "StateInputFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ViewModelProvider will automatically use the fragment's arguments
        // if the ViewModel uses SavedStateHandle.
        viewModel = ViewModelProvider(this).get(StateInputViewModel::class.java)

        // Alternative if not using SavedStateHandle in ViewModel (less common now):
        // arguments?.let {
        //    val typeName = it.getString(ARG_DIALOG_TYPE_NAME)
        //    viewModel.initialize(...)
        // }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStateInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // ... (your existing setupViews logic based on viewModel) ...
        // This part remains the same as it relies on the ViewModel which gets args via SavedStateHandle
        when (viewModel.dialogType) {
            DialogManager.DialogType.USER_INPUT_TEXT -> {
                binding.stateInputEditText.isVisible = true
                binding.stateInputSpinner.isVisible = true
                binding.stateInputNegativeButton.isVisible = true;
                binding.stateInputPositiveButton.isVisible = true;
            }
            DialogManager.DialogType.VESSEL_SELECTION -> {
                binding.stateInputEditText.isVisible = false
                binding.stateInputSpinner.isVisible = true
            }
            DialogManager.DialogType.CONFIRMATION, DialogManager.DialogType.INFO_DISPLAY -> {
                binding.stateInputEditText.isVisible = false
                binding.stateInputSpinner.isVisible = false
            }
            null -> {
                binding.stateInputEditText.isVisible = false
                binding.stateInputSpinner.isVisible = false
            }

            DialogManager.DialogType.QUANTITY_INPUT_FRAGMENT -> TODO()
            DialogManager.DialogType.TEXT_INPUT_DIALOG -> TODO()
            DialogManager.DialogType.VESSEL_SELECTION_FRAGMENT -> TODO()
            DialogManager.DialogType.CONFIRMATION_DIALOG -> TODO()
            DialogManager.DialogType.BUNKER_DETAILS_FRAGMENT -> TODO()
            DialogManager.DialogType.GENERIC_MESSAGE -> TODO()
        }

        binding.stateInputEditText.setText(viewModel.currentTextInput.value)
        binding.stateInputEditText.addTextChangedListener(SimpleTextWatcher { text ->
            viewModel.currentTextInput.value = text
        })

        binding.stateInputPositiveButton.setOnClickListener {
            if (viewModel.isInputValid()) {
                val resultDataMap = viewModel.onPositiveClicked()
                val resultBundle = bundleOf().apply {
                    resultDataMap.forEach { (key, value) ->
                        when (value) {
                            is String -> putString(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Int -> putInt(key, value)
                            // Add other types
                        }

                    }
                }
                viewModel.requestId?.let { reqId ->
                    //var result = setFragmentResult(reqId, resultDataMap)

                    sendResultBack(viewModel.requestId!!, resultBundle)
                }

                dismiss()
            } else {
                if (viewModel.dialogType == DialogManager.DialogType.USER_INPUT_TEXT) {
                    binding.stateInputEditText.error = "Input required"
                }
            }
        }

        binding.stateInputNegativeButton.setOnClickListener {
            val resultDataMap = viewModel.onNegativeClicked()
            val resultBundle = bundleOf().apply {
                putBoolean(ResultKeys.NEGATIVE_CLICK, resultDataMap[ResultKeys.NEGATIVE_CLICK] as? Boolean ?: false)
            }
            viewModel.requestId?.let { reqId ->
                setFragmentResult(reqId, resultBundle)
            }
            dismiss()
        }
        binding.stateInputPositiveButton.text = "OK"
       // binding.stateInputPositiveButton.setOnClickListener {
            // ... handle result ...
       //     dismiss()
       // }

        binding.stateInputNegativeButton.text = "Cancel"
        binding.stateInputNegativeButton.setOnClickListener {
            // ... handle cancellation ...
            dismiss()
        }
        // Ensure buttons themselves are visible if they were initially gone (they aren't in your XML)
        // binding.stateInputPositiveButton.visibility = View.VISIBLE
        // binding.stateInputNegativeButton.visibility = View.VISIBLE
    }
    // In VesselSelectionFragment.kt (when it's ready to return a result)
    // This assumes VesselSelectionFragment was launched in a way that expects a result
    // via an Activity intermediary or a specific contract.
    private fun sendResultBack(requestId: String, dataMap: Bundle) {
        //val resultBundle = Bundle().apply {
        //    putString("requestId", requestId)
        //    putSerializable("dataMap", dataMap)
        //}
        // If launched by an Activity for result:
        // requireActivity().setResult(Activity.RESULT_OK, Intent().putExtras(resultBundle))
        // requireActivity().finish()

        // If using Jetpack Navigation Component & Fragment Result API:
        // (This is the more common way for Fragment-to-Fragment)
        //parentFragmentManager.setFragmentResult("VesselSelectionKey_from_DashboardFragment", dataMap)
        //findNavController().popBackStack() // Go back to DashboardFragmen
        Log.i("StateInputFragment", "Sending Shoe Message result back to DashboardFragment")
        viewModel.sendResultBack(requestId,dataMap)
        TransactionManager.UiAction.ShowMessage(requestId,  "Art Dialog finished")
    }
    private fun observeViewModel() {
        // ... (your existing observeViewModel logic) ...
        viewModel.title.observe(viewLifecycleOwner) { title ->
            binding.stateInputTitle.text = title
        }
        viewModel.message.observe(viewLifecycleOwner) { message ->
            binding.stateInputMessage.text = message
            binding.stateInputMessage.isVisible = !message.isNullOrEmpty()
        }
        viewModel.positiveButtonText.observe(viewLifecycleOwner) { text ->
            binding.stateInputPositiveButton.text = text
        }
        viewModel.negativeButtonText.observe(viewLifecycleOwner) { text ->
            binding.stateInputNegativeButton.text = text
            binding.stateInputNegativeButton.isVisible = !text.isNullOrEmpty()
        }
        viewModel.inputHint.observe(viewLifecycleOwner) { hint ->
            binding.stateInputEditText.hint = hint
        }
        viewModel.spinnerItems.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.stateInputSpinner.adapter = adapter
                viewModel.currentSpinnerSelection.value?.let { selection ->
                    val position = items.indexOf(selection)
                    if (position >= 0) {
                        binding.stateInputSpinner.setSelection(position)
                    }
                }
            }
        }
        binding.stateInputSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.currentSpinnerSelection.value = viewModel.spinnerItems.value?.get(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                viewModel.currentSpinnerSelection.value = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun completeUiRequest() {
    TODO("Not yet implemented")

}


// SimpleTextWatcher (as defined before)
private class SimpleTextWatcher(private val onTextChanged: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onTextChanged(s.toString())
    }
    override fun afterTextChanged(s: android.text.Editable?) {}
}