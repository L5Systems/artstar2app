package com.example.artest2.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.artest2.core.BaseTransaction
import com.example.artest2.core.StateBase
import com.example.artest2.core.StateNode
import com.example.artest2.core.TransStatus
import com.example.artest2.states.ExampleCommitState
import com.example.artest2.states.ExampleDataFetchingState
import com.example.artest2.states.ExampleProcessingState
import com.example.artest2.transactions.SampleTransaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class TransactionManager {
    private val transactionPrototypes: MutableMap<String, (Long, String) -> BaseTransaction> = mutableMapOf()
    private val openTransactions: MutableList<BaseTransaction> = mutableListOf()

    private val _currentTransStatus = MutableStateFlow(TransStatus())
    val currentTransStatus: StateFlow<TransStatus> = _currentTransStatus.asStateFlow()

    private var activeTransactionId: Long = -1

    init {
        // Register transaction prototypes
        transactionPrototypes[SampleTransaction.TRANSACTION_TYPE] =
            { id, type -> SampleTransaction(id, type) }

        // Create a default transaction or load initial state if necessary
        // createNewTransaction(SampleTransaction.TRANSACTION_TYPE)
    }

    private fun getRandomId(): Long = Random.nextLong(1000, 100000)

    fun createNewTransaction(tranType: String): BaseTransaction? {
        val id = getRandomId()
        val transactionConstructor = transactionPrototypes[tranType]
        val newTransaction = transactionConstructor?.invoke(id, tranType)

        return newTransaction?.also {
            openTransactions.add(it)
            setActiveTransaction(it.transactionID)
            println("TransactionManager: Created new transaction ${it.transactionName} with ID ${it.transactionID}")
        } ?: run {
            println("Error: Unknown transaction type name provided: $tranType")
            null
        }
    }

    fun setActiveTransaction(transactionId: Long) {
        val transaction = openTransactions.find { it.transactionID == transactionId }
        if (transaction != null) {
            activeTransactionId = transactionId
            _currentTransStatus.value = transaction.loadStatus() // Load its specific status
            println("TransactionManager: Active transaction set to ID $activeTransactionId (${transaction.getName()})")
        } else {
            println("TransactionManager: Could not find transaction with ID $transactionId to set active.")
            // Optionally reset if no valid transaction is found
            // activeTransactionId = -1
            // _currentTransStatus.value = TransStatus()
        }
    }

    fun getActiveTransaction(): BaseTransaction? {
        return openTransactions.find { it.transactionID == activeTransactionId }
    }

    // Simplified state creation. In a real app, this might involve dependency injection.
    fun createStateForTransaction(stateName: String, transaction: BaseTransaction): StateBase? {
        return when (stateName) {
            "ExampleDataFetchingState" -> ExampleDataFetchingState(transaction)
            "ExampleProcessingState" -> ExampleProcessingState(transaction)
            "ExampleCommitState" -> ExampleCommitState(transaction)
            // Add other state types here
            else -> {
                println("Error: Unknown state name provided: $stateName")
                null
            }
        }
    }

    fun addStateToActiveTransactionByName(stateName: String): StateBase? {
        val activeTransaction = getActiveTransaction() ?: run {
            println("Error: No active transaction to add state to.")
            return null
        }

        // Check if state already exists (for re-entry logic, simplified here)
        var state = activeTransaction.states.find { it.getName() == stateName }
        if (state != null) {
            println("State $stateName already exists in transaction. Re-entering.")
            // Potentially trim stack or just set as current
            activeTransaction.currentState = state
            return state
        }

        state = createStateForTransaction(stateName, activeTransaction)
        state?.let {
            activeTransaction.states.add(it)
            activeTransaction.currentState = it // Set as current for immediate execution
            println("Added state ${it.getName()} to transaction ${activeTransaction.getName()}")
        }
        return state
    }

    suspend fun executeState(state: StateBase, MainActivityContext: Context, initialContext: Map<String, Any>? = null): Map<String, Any>? {
        val activeTransaction = getActiveTransaction() ?: run {
            println("Error: No active transaction to execute state.")
            return null
        }
        if (activeTransaction.currentState != state && !activeTransaction.states.contains(state)) {
             println("Error: State ${state.getName()} is not part of the active transaction ${activeTransaction.getName()}.")
             return null
        }

        activeTransaction.currentState = state // Ensure this is the current state being executed
        val executionContext = activeTransaction.context + (initialContext ?: emptyMap())
        var returnData: Map<String, Any>? = null

        println("--- Starting execution for state: ${state.getName()} ---")
        _currentTransStatus.value = _currentTransStatus.value.copy(transactionStatus = "Executing ${state.getName()}")

        try {
            val inputData = state.fetchInputData(executionContext)
            val executionResult = state.executeDialog(inputData,MainActivityContext)
            returnData = state.fetchReturnData(executionResult)
            state.commit(returnData)

            println("--- State ${state.getName()} completed successfully ---")
            activeTransaction.context.putAll(returnData) // Update shared context
            _currentTransStatus.value = _currentTransStatus.value.copy(transactionStatus = "${state.getName()} Committed")

        } catch (e: Exception) {
            println("--- Error during execution of state ${state.getName()}: ${e.message} ---")
            _currentTransStatus.value = _currentTransStatus.value.copy(transactionStatus = "Error in ${state.getName()}")
            try {
                state.rollback(e)
            } catch (rollbackError: Exception) {
                println("--- Critical Error during rollback for state ${state.getName()}: ${rollbackError.message} ---")
            }
            // Re-throw or handle error appropriately
            // For this starter, we'll just log and not re-throw to allow UI to update.
            return null // Indicate failure
        } finally {
             // activeTransaction.currentState = null // Or move to next state logic
        }
        return returnData
    }

    fun getCurrentTransactionStateNodes(): List<StateNode> {
        return getActiveTransaction()?.getStateEntries() ?: emptyList()
    }

    fun isValidStateTransition(currentStateKey: String, nextStateKey: String): Boolean {
        return getActiveTransaction()?.isValidStateTransition(currentStateKey, nextStateKey) ?: false
    }

    fun saveActiveTransactionStatus() {
        getActiveTransaction()?.saveStatus(_currentTransStatus.value)
    }

    fun clearTransactionHistoryForActive() {
        getActiveTransaction()?.let {
            it.states.clear()
            it.context.clear()
            it.clearTransactionLevelData()
            // Reset parts of TransStatus related to this transaction's progress
            _currentTransStatus.value = TransStatus(
                selectedTransactionId = it.transactionID,
                selectedTransactionName = it.transactionName,
                transactionStatus = "inactive"
                // Keep vessel_id etc. if they are truly global and not transaction-specific
            )
            println("Cleared history for active transaction ${it.getName()}")
        }
    }

    fun getOpenTransactionsList(): List<BaseTransaction> = openTransactions.toList()

    fun deleteTransaction(transactionId: Long) {
        val removed = openTransactions.removeAll { it.transactionID == transactionId }
        if (removed) {
            println("TransactionManager: Deleted transaction with ID $transactionId")
            if (activeTransactionId == transactionId) {
                activeTransactionId = -1
                _currentTransStatus.value = TransStatus() // Reset global status
                // Optionally, set another transaction as active or create a new default one
            }
        }
    }
    // Define the sealed class for UI actions (can be outside the class too)
    sealed class UiAction {
        data class ShowDialogActivity(val intent: Intent) : UiAction()
        data class DialogResult(val resultCode: Int, val data: Intent?) : UiAction() // Add this
        // Add other UI actions as needed
    }

    private val _uiActions = MutableSharedFlow<UiAction>()
    val uiActions = _uiActions.asSharedFlow()


    // You might also need a function to process the result received by the Activity
    fun processDialogActivityResult(resultCode: Int, data: Intent?) {
        // This function will be called from MainActivity
        // It needs to somehow feed the result back into the state machine's logic.
        // This is the trickiest part and depends on how your state machine expects results.
        println("TransactionManager: Received dialog result - resultCode: $resultCode, data: $data")

        // Example: Find the active transaction and current state (assuming it's the one that launched the dialog)
        val activeTransaction = getActiveTransaction()
        val currentState = activeTransaction?.currentState as? ExampleDataFetchingState // Cast to the state that launched the dialog

        currentState?.let {
            // You need a way for the state to "receive" this result.
            // This could involve:
            // 1. The state machine/ViewModel having a mechanism to wait for a specific event.
            // 2. The state itself having a way to resume from a suspended state with a result.
            // This is advanced and often requires rethinking the state execution flow.

            // A simpler approach for now: just log or process the result here in the Manager
            // and then potentially trigger the next state transition based on this result.
            // However, this tightly couples the Manager to the dialog result logic.
            if (resultCode == Activity.RESULT_OK) {
                val userName = data?.getStringExtra("userName") // Get data from the result Intent
                val consentGiven = data?.getBooleanExtra("consentGiven", false)

                println("Dialog Result - UserName: $userName, Consent Given: $consentGiven")

                // How to use this result?
                // Option A (More complex, better separation): The state machine/ViewModel has a suspend function that waits for the result emitted via a different channel (e.g., another SharedFlow).
                // Option B (Simpler for now, less ideal separation): You call a method on the ViewModel/Manager that processes the result and determines the next state transition based on it.
            } else {
                println("Dialog was cancelled or failed.")
                // Handle cancellation/failure
            }
        } ?: run {
            println("No active transaction or current state to process dialog result.")
        }
    }    // ... rest of TransactionManager code ...

    // Function to emit a UI action
    suspend fun emitUiAction(action: UiAction) {
        _uiActions.emit(action)
    }
}
