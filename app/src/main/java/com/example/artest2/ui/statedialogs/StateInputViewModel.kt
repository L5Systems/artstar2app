
package com.example.artest2.ui.statedialogs // Assuming same package as StateInputFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.artest2.DialogManager

// Argument keys (should match those used in your navigation graph and StateInputFragment)
object StateInputArgs {
    const val REQUEST_ID = "requestId" // Crucial for sending result back
    const val DIALOG_TYPE = "dialogType"
    const val TITLE = "title"
    const val MESSAGE = "message"
    const val POSITIVE_BUTTON = "positiveButtonText"
    const val NEGATIVE_BUTTON = "negativeButtonText"
    const val NEUTRAL_BUTTON = "neutralButtonText"
    const val INPUT_HINT = "inputHint"
    const val ITEMS = "items" // For Spinner
    const val INITIAL_SELECTION = "initialSelection" // For Spinner
    const val CUSTOM_DATA = "customData" // Map, might need custom Parcelable/Serializable handling if complex
}

class StateInputViewModel(
    private val savedStateHandle: SavedStateHandle // For accessing navigation arguments
) : ViewModel() {

    // --- LiveData for UI elements driven by arguments ---
    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _positiveButtonText = MutableLiveData<String>()
    val positiveButtonText: LiveData<String> = _positiveButtonText

    private val _negativeButtonText = MutableLiveData<String?>()
    val negativeButtonText: LiveData<String?> = _negativeButtonText

    private val _neutralButtonText = MutableLiveData<String?>()
    val neutralButtonText: LiveData<String?> = _neutralButtonText

    private val _inputHint = MutableLiveData<String?>()
    val inputHint: LiveData<String?> = _inputHint

    private val _spinnerItems = MutableLiveData<List<String>>(emptyList())
    val spinnerItems: LiveData<List<String>> = _spinnerItems

    // --- LiveData for UI state / user input ---
    val currentTextInput = MutableLiveData<String>("")
    val currentSpinnerSelection = MutableLiveData<String?>()

    // --- Dialog Type ---
    // Not LiveData as it's unlikely to change after fragment creation for a dialog
    val dialogType: DialogManager.DialogType?

    // --- Request ID (critical for sending result) ---
    val requestId: String?


    init {
        // Load initial values from SavedStateHandle (navigation arguments)
        requestId = savedStateHandle.get<String>(StateInputArgs.REQUEST_ID)
        dialogType = savedStateHandle.get<DialogManager.DialogType>(StateInputArgs.DIALOG_TYPE)

        _title.value = savedStateHandle.get<String>(StateInputArgs.TITLE) ?: "Input Required"
        _message.value = savedStateHandle.get<String>(StateInputArgs.MESSAGE)
        _positiveButtonText.value = savedStateHandle.get<String>(StateInputArgs.POSITIVE_BUTTON) ?: "OK"
        _negativeButtonText.value = savedStateHandle.get<String>(StateInputArgs.NEGATIVE_BUTTON)
        _neutralButtonText.value = savedStateHandle.get<String>(StateInputArgs.NEUTRAL_BUTTON)
        _inputHint.value = savedStateHandle.get<String>(StateInputArgs.INPUT_HINT)

        val itemsArray = savedStateHandle.get<Array<String>>(StateInputArgs.ITEMS)
        if (itemsArray != null) {
            _spinnerItems.value = itemsArray.toList()
            val initialSelection = savedStateHandle.get<String>(StateInputArgs.INITIAL_SELECTION)
            if (!initialSelection.isNullOrEmpty() && _spinnerItems.value?.contains(initialSelection) == true) {
                currentSpinnerSelection.value = initialSelection
            } else if (_spinnerItems.value?.isNotEmpty() == true) {
                currentSpinnerSelection.value = _spinnerItems.value!![0] // Default to first item
            }
        } else {
            _spinnerItems.value = emptyList()
        }

        // You might also load initial text input if provided
        // currentTextInput.value = savedStateHandle.get<String>("initialTextInput") ?: ""
    }

    fun onPositiveClicked(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        result[StateInputFragment.ResultKeys.POSITIVE_CLICK] = true
        if (dialogType == DialogManager.DialogType.USER_INPUT_TEXT) {
            result[StateInputFragment.ResultKeys.USER_TEXT_INPUT] = currentTextInput.value ?: ""
        }
        if (dialogType == DialogManager.DialogType.VESSEL_SELECTION || (spinnerItems.value?.isNotEmpty() == true)) {
            result[StateInputFragment.ResultKeys.SELECTED_ITEM] = currentSpinnerSelection.value
        }
        // Add any customData passed in arguments if it needs to be returned
        // val customData = savedStateHandle.get<Map<String, Any>>(StateInputArgs.CUSTOM_DATA)
        // customData?.let { result[StateInputFragment.ResultKeys.CUSTOM_DATA_ECHO] = it }
        return result
    }

    fun onNegativeClicked(): Map<String, Any?> {
        return mapOf(StateInputFragment.ResultKeys.NEGATIVE_CLICK to true)
    }

    fun onNeutralClicked(): Map<String, Any?> {
        return mapOf(StateInputFragment.ResultKeys.NEUTRAL_CLICK to true)
    }

    // Optional: Validation logic
    fun isInputValid(): Boolean {
        return when (dialogType) {
            DialogManager.DialogType.USER_INPUT_TEXT -> {
                !currentTextInput.value.isNullOrBlank() // Example: non-empty input
            }
            DialogManager.DialogType.VESSEL_SELECTION -> {
                currentSpinnerSelection.value != null
            }
            // Add validation for other types as needed
            else -> true // Default to true if no specific validation
        }
    }
}