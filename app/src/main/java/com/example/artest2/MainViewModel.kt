package com.example.artest2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artest2.core.StateNode
import com.example.artest2.manager.TransactionManager
import com.example.artest2.transactions.SampleTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Example LiveData for a title
    private val _activityTitle = MutableLiveData<String>().apply {
        value = "AR Test App" // Default title
    }
    val activityTitle: LiveData<String> = _activityTitle

    // Example LiveData for some user status
    private val _userLoggedIn = MutableLiveData<Boolean>().apply {
        value = false // Default to not logged in
    }
    val userLoggedIn: LiveData<Boolean> = _userLoggedIn

    // Add more LiveData properties and functions as needed for your MainActivity

    fun updateActivityTitle(newTitle: String) {
        _activityTitle.value = newTitle
    }

    fun setUserLoggedIn(isLoggedIn: Boolean) {
        _userLoggedIn.value = isLoggedIn
    }

    // Example: A function that might be called from MainActivity
    fun onAppStart() {
        // Perform any on-start logic for the MainActivity
        println("MainViewModel: App Started")
        // You could load initial data here or check user session
        //updateActivityTitle("New Title from ViewModel")
        //setUserLoggedIn(true)

    }
    // Define UI States
    sealed class UiState {
        object Idle : UiState()
        data class Loading(val message: String) : UiState()
        data class StepComplete(val message: String, val nextStepDescription: String) : UiState()
        data class TransactionComplete(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
    private val transactionManager: TransactionManager = TransactionManager()
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // EXPOSE UI ACTIONS FROM TRANSACTIONMANAGER
    val uiActions: Flow<TransactionManager.UiAction> = transactionManager.uiActions // Expose the flow


    // Expose TransactionManager's status flow if needed for UI
    val currentTransStatus = transactionManager.currentTransStatus

    // Iterator for state nodes
    private var currentTransactionIterator: Iterator<StateNode>? = null

    // Factory for ViewModel creation (if needed for dependency injection)


    fun startSampleTransaction(context: Context) { // Pass context here
        viewModelScope.launch {
            _uiState.value = UiState.Loading("Starting transaction...")
            Log.d("MainViewModel", "Attempting to create new transaction...")
            val newTransaction = transactionManager.createNewTransaction(SampleTransaction.TRANSACTION_TYPE)
            if (newTransaction != null) {
                transactionManager.setActiveTransaction(newTransaction.transactionID)
                Log.d("MainViewModel", "New transaction created and set active: ${newTransaction.transactionID}")
                // Initialize the iterator
                currentTransactionIterator = transactionManager.getCurrentTransactionStateNodes().iterator()
                Log.d("MainViewModel", "Transaction flow initialized with ${transactionManager.getCurrentTransactionStateNodes().size} steps.")
                _uiState.value = UiState.StepComplete("Transaction Started.", if (currentTransactionIterator?.hasNext() == true) "Next: ${currentTransactionIterator?.next()?.label ?: "Step"}" else "Finish")
                currentTransactionIterator = transactionManager.getCurrentTransactionStateNodes().iterator() // Reset iterator for actual steps

            } else {
                _uiState.value = UiState.Error("Failed to start transaction.")
                Log.e("MainViewModel", "Failed to create new transaction.")
            }
        }
    }

    fun proceedToNextStep(context: Context) { // Pass context here
        viewModelScope.launch {
            val iterator = currentTransactionIterator ?: run {
                _uiState.value = UiState.Error("No active transaction flow.")
                return@launch
            }
            if (!iterator.hasNext()) {
                _uiState.value = UiState.TransactionComplete("Transaction flow finished.")
                transactionManager.getActiveTransaction()?.let {
                    transactionManager.saveActiveTransactionStatus() // Save final status
                }
                Log.d("MainViewModel", "Transaction flow finished.")
                return@launch
            }

            val nextNode = iterator.next()
            _uiState.value = UiState.Loading("Executing: ${nextNode.label}")
            Log.d("MainViewModel", "Proceeding to state: ${nextNode.key} (${nextNode.label})")

            val stateInstance = transactionManager.addStateToActiveTransactionByName(nextNode.key)
            if (stateInstance != null) {
                // Determine the initial transaction context data (the Map)
                // This logic might need refinement if a state is re-entered and needs specific data.
                val initialTransactionContextData = if (transactionManager.getActiveTransaction()?.states?.size == 1) {
                    mapOf("initialUserId" to 456L) // Your initial data for the first state
                } else {
                    // Use the accumulated context from previous states
                    transactionManager.getActiveTransaction()?.context
                }

                // Call executeState, passing the Android Context as the second argument,
                // and the transaction context data as the third argument.
                Log.d("MainViewModel", "Executing state ${stateInstance.getName()} with context.")
                val result = transactionManager.executeState(stateInstance, context, initialTransactionContextData) // Pass context and initialTransactionContextData

                if (result != null) {
                    Log.d("MainViewModel", "State execution successful for ${stateInstance.getName()}. Result: $result")
                    // Update UI state to reflect completion
                    val nextLabel = if (iterator.hasNext()) {
                        transactionManager.getCurrentTransactionStateNodes()
                            .find { it.key == iterator.peekNext()?.key }?.label ?: "Next Step"
                    } else {
                        "Finish"
                    }
                    _uiState.value = UiState.StepComplete(
                        "${nextNode.label} Complete.",
                        if (iterator.hasNext()) "Next: $nextLabel" else "Finish Transaction"
                    )
                    Log.d("MainViewModel", "UI State updated: StepComplete")
                } else {
                    Log.e("MainViewModel", "State execution failed for ${stateInstance.getName()}.")
                    _uiState.value = UiState.Error("Error in state: ${nextNode.label}")
                    Log.d("MainViewModel", "UI State updated: Error")
                }
            } else {
                Log.e("MainViewModel", "Failed to create state instance for: ${nextNode.key}")
                _uiState.value = UiState.Error("Failed to create state: ${nextNode.label}")
                Log.d("MainViewModel", "UI State updated: Error")
            }
        }
    }

    // Function to handle the result received from the dialog Activity
    fun handleDialogResult(resultCode: Int, data: Intent?) {
        Log.d("MainViewModel", "Handling dialog result. Result Code: $resultCode")
        // Pass the result to the TransactionManager for processing
        transactionManager.processDialogActivityResult(resultCode, data)

        // Now, based on the processed result, you might need to trigger the next step.
        // A simple approach: If the dialog was OK, attempt to proceed to the next state.
        // A more complex approach: The state that launched the dialog would be in a
        // "waiting" state and resume execution using the result data.
        if (resultCode == Activity.RESULT_OK) {
            Log.d("MainViewModel", "Dialog result OK. Attempting to proceed to next step.")
            // IMPORTANT: Need a Context to call proceedToNextStep.
            // Re-architect if ViewModel shouldn't hold Context.
            // Option: ViewModel emits a UiAction to MainActivity to "ProceedWithResult"
            // and MainActivity calls proceedToNextStep with its context.
            // For now, we'll assume ViewModel needs the Context, which is a limitation.
            //  val context = // How to get context here? This is the challenge.
            //  proceedToNextStep(context) // Cannot call without context
        } else {
            Log.d("MainViewModel", "Dialog result Canceled or Failed. Handling error.")
            // Handle cancellation or failure, e.g., transition to an error state
            _uiState.value = UiState.Error("Dialog interaction canceled or failed.")
        }
    }

    // Extension function to peek next element without advancing iterator
    private fun <T> Iterator<T>.peekNext(): T? {
        val list = this.asSequence().take(2).toList()
        return if (list.size > 1) list[1] else null
    }

    // Example function to clear transaction history
    fun clearTransactionHistory() {
        transactionManager.clearTransactionHistoryForActive()
        currentTransactionIterator = null // Reset iterator
        _uiState.value = UiState.Idle // Reset UI state
        Log.d("MainViewModel", "Transaction history cleared.")
    }

    // Example function to delete a transaction
    fun deleteTransaction(transactionId: Long) {
        transactionManager.deleteTransaction(transactionId)
        // Update UI or state if the active transaction was deleted
        if (transactionManager.currentTransStatus.value.selectedTransactionId != transactionId) {
            currentTransactionIterator = null
            _uiState.value = UiState.Idle
        }
        Log.d("MainViewModel", "Transaction $transactionId deleted.")
    }

}